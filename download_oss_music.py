#!/usr/bin/env python3
"""从 OSS 下载音乐文件到本地目录结构。

该脚本只下载项目中定义的各个音乐文件夹下的歌曲，按文件夹分别保存，
不会将所有 Bucket 音乐合并到同一目录。

运行方式：
  python download_oss_music.py
  python download_oss_music.py --folders ha_ji_mi,dian_gun
  python download_oss_music.py --output ./my-music --overwrite

说明：
  --folders  指定要下载的文件夹键，多个用逗号分隔；
             如果省略，则下载所有支持的文件夹。
  --output   指定本地保存根目录，默认是 downloaded_music。
  --overwrite  如果文件已存在，则覆盖下载。

依赖：
  pip install oss2
"""

import argparse
import os
import re
import sys
from pathlib import Path

FOLDER_KEYS = {
    "ha_ji_mi": "哈基米",
    "dian_gun": "溜冰场",
    "da_si_ma": "大司马",
    "ding_zhen": "丁真",
    "dxl": "东洋雪莲",
    "DDF": "哲学",
    "true_music": "真正的音乐",
}

ENV_FILE = Path(__file__).parent / ".env"


def parse_env_file(env_path: Path):
    config = {}
    if not env_path.exists():
        return config

    with env_path.open("r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            if "=" not in line:
                continue
            key, value = line.split("=", 1)
            key = key.strip()
            value = value.strip().strip('"').strip("'")
            if key and value:
                config[key] = value
    return config


def resolve_value(cli_value, env_config, env_key, default=None):
    if cli_value:
        return cli_value
    if env_key in env_config and env_config[env_key]:
        return env_config[env_key]
    return default


def build_bucket(endpoint: str, bucket_name: str, access_key: str, secret_key: str):
    try:
        import oss2
    except ImportError as exc:
        raise SystemExit(
            "Missing dependency: install with `pip install oss2` and retry."
        ) from exc

    endpoint = endpoint.strip()
    if not endpoint.startswith(("http://", "https://")):
        endpoint = "https://" + endpoint

    auth = oss2.Auth(access_key, secret_key)
    return oss2.Bucket(auth, endpoint, bucket_name)


def list_mp3_keys(bucket, prefix: str):
    try:
        import oss2
    except ImportError:
        raise

    mp3_keys = []
    for obj in oss2.ObjectIterator(bucket, prefix=prefix):
        if obj.key.lower().endswith(".mp3"):
            mp3_keys.append(obj.key)
    return mp3_keys


def download_object(bucket, object_key: str, destination: Path, overwrite: bool = False):
    destination.parent.mkdir(parents=True, exist_ok=True)
    if destination.exists() and not overwrite:
        print(f"[skip] {destination}")
        return
    bucket.get_object_to_file(object_key, str(destination))
    print(f"[downloaded] {destination}")


def normalize_folder_keys(raw_value: str):
    if not raw_value:
        return []
    candidates = [item.strip() for item in raw_value.split(",") if item.strip()]
    normalized = []
    for item in candidates:
        if item in FOLDER_KEYS:
            normalized.append(item)
        else:
            raise ValueError(f"Unknown folder key: {item}. Supported keys: {', '.join(FOLDER_KEYS)}")
    return normalized


def main():
    parser = argparse.ArgumentParser(
        description="Download folder-specific songs from Aliyun OSS into local directories."
    )
    parser.add_argument(
        "--folders",
        help="Comma-separated folder keys to download, e.g. ha_ji_mi,dian_gun. If omitted, all folders are downloaded.",
    )
    parser.add_argument(
        "--output",
        default="downloaded_music",
        help="Local base folder to save downloaded music.",
    )
    parser.add_argument(
        "--overwrite",
        action="store_true",
        help="Overwrite existing files if they already exist locally.",
    )
    parser.add_argument(
        "--endpoint",
        help="OSS endpoint. If omitted, read from .env ALIYUN_OSS_ENDPOINT or environment.",
    )
    parser.add_argument(
        "--bucket",
        help="OSS bucket name. If omitted, read from .env ALIYUN_OSS_BUCKET_NAME or environment.",
    )
    parser.add_argument(
        "--access-key",
        help="OSS access key. If omitted, read from .env ALIYUN_OSS_ACCESS_KEY or environment.",
    )
    parser.add_argument(
        "--secret-key",
        help="OSS secret key. If omitted, read from .env ALIYUN_OSS_SECRET_KEY or environment.",
    )
    args = parser.parse_args()

    env_config = parse_env_file(ENV_FILE)

    endpoint = resolve_value(args.endpoint, env_config, "ALIYUN_OSS_ENDPOINT")
    bucket_name = resolve_value(args.bucket, env_config, "ALIYUN_OSS_BUCKET_NAME")
    access_key = resolve_value(args.access_key, env_config, "ALIYUN_OSS_ACCESS_KEY")
    secret_key = resolve_value(args.secret_key, env_config, "ALIYUN_OSS_SECRET_KEY")

    if not endpoint or not bucket_name or not access_key or not secret_key:
        print("Missing OSS configuration. Please set ALIYUN_OSS_ENDPOINT, ALIYUN_OSS_BUCKET_NAME, ALIYUN_OSS_ACCESS_KEY, ALIYUN_OSS_SECRET_KEY in .env or environment.")
        sys.exit(1)

    if args.folders:
        folder_keys = normalize_folder_keys(args.folders)
    else:
        folder_keys = list(FOLDER_KEYS.keys())

    output_base = Path(args.output).resolve()
    bucket = build_bucket(endpoint, bucket_name, access_key, secret_key)

    print(f"OSS endpoint: {endpoint}")
    print(f"OSS bucket: {bucket_name}")
    print(f"Saving to: {output_base}")
    print(f"Folders: {', '.join(folder_keys)}")

    for folder_key in folder_keys:
        folder_name = FOLDER_KEYS[folder_key]
        prefix = f"music/{folder_name}/"
        print(f"\n=== Downloading folder '{folder_key}' -> prefix '{prefix}' ===")

        mp3_keys = list_mp3_keys(bucket, prefix)
        if not mp3_keys:
            print(f"No .mp3 files found under prefix {prefix}.")
            continue

        folder_dir = output_base / folder_key
        downloaded = 0
        for object_key in mp3_keys:
            filename = Path(object_key).name
            destination = folder_dir / filename
            try:
                download_object(bucket, object_key, destination, overwrite=args.overwrite)
                downloaded += 1
            except Exception as exc:
                print(f"[error] failed to download {object_key}: {exc}")

        print(f"Downloaded {downloaded} files for folder '{folder_key}'.")

    print("\nDownload complete.")


if __name__ == "__main__":
    main()
