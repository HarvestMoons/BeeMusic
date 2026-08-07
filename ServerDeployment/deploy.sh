#!/usr/bin/env bash
set -Eeuo pipefail

cd "$(dirname "$0")"

echo "开始部署音乐播放器..."
docker compose pull
docker compose up -d --force-recreate --remove-orphans

echo "等待服务健康..."
for attempt in $(seq 1 30); do
    spring_status="$(docker inspect --format '{{.State.Health.Status}}' spring_app 2>/dev/null || true)"
    frontend_status="$(docker inspect --format '{{.State.Health.Status}}' nginx_frontend 2>/dev/null || true)"
    if [[ "$spring_status" == "healthy" && "$frontend_status" == "healthy" ]]; then
        break
    fi
    if [[ "$spring_status" == "unhealthy" || "$frontend_status" == "unhealthy" ]]; then
        echo "服务健康检查失败: spring=$spring_status frontend=$frontend_status" >&2
        docker compose ps
        exit 1
    fi
    sleep 5
    if [[ "$attempt" == "30" ]]; then
        echo "服务在规定时间内未达到健康状态" >&2
        docker compose ps
        exit 1
    fi
done

curl --fail --silent --show-error --max-time 10 \
    --resolve beemusic.fun:443:127.0.0.1 \
    https://beemusic.fun/api/public/health >/dev/null

docker compose ps
echo "部署完成! 应用访问地址: https://beemusic.fun"
