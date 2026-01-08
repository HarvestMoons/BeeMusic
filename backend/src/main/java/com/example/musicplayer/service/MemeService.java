package com.example.musicplayer.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObjectSummary;
import com.example.musicplayer.model.Meme;
import com.example.musicplayer.repository.MemeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MemeService {

    private final MemeRepository memeRepository;
    private final OssUtil ossUtil;
    private final OSS ossClient;

    private static final String MEME_FOLDER = "meme/";

    public MemeService(MemeRepository memeRepository, OssUtil ossUtil, OSS ossClient) {
        this.memeRepository = memeRepository;
        this.ossUtil = ossUtil;
        this.ossClient = ossClient;
    }

    public Meme getRandomMeme() {
        long count = memeRepository.countByIsDeletedFalse();
        if (count == 0) {
            return null;
        }

        int index = ThreadLocalRandom.current().nextInt((int) count);
        Page<Meme> page = memeRepository.findByIsDeletedFalse(PageRequest.of(index, 1));

        if (page.hasContent()) {
            Meme meme = page.getContent().getFirst();

            String signedUrl = ossClient.generatePresignedUrl(
                    ossUtil.getBucketName(),
                    meme.getKey(),
                    ossUtil.getExpirationDate()
            ).toString();

            meme.setUrl(signedUrl);
            return meme;
        }
        return null;
    }

    @Transactional
    public synchronized int syncMemes() {
        // 1. 获取 OSS 上所有文件列表
        List<OSSObjectSummary> ossObjects = ossUtil.listFilesByPrefix(MEME_FOLDER);

        // 2. 获取数据库中所有已存在的 Key (只查 Key 字段，性能更高)
        Set<String> dbKeys = new HashSet<>(memeRepository.findAllKeys());

        List<Meme> newMemes = new ArrayList<>();

        for (OSSObjectSummary obj : ossObjects) {
            String key = obj.getKey();
            // 跳过文件夹本身和非图片文件
            if (key.endsWith("/") || !isImage(key)) {
                continue;
            }

            // 3. 内存比对：如果数据库没有，则添加到待保存列表
            if (!dbKeys.contains(key)) {
                Meme meme = new Meme(key);
                newMemes.add(meme);
            }
        }

        // 4. 批量保存
        if (!newMemes.isEmpty()) {
            memeRepository.saveAll(newMemes);
        }

        return newMemes.size();
    }

    @Transactional
    public void deleteMeme(Long id) {
        Optional<Meme> memeOpt = memeRepository.findById(id);
        if (memeOpt.isPresent()) {
            Meme meme = memeOpt.get();
            meme.setDeleted(true);
            memeRepository.save(meme);
        }
    }

    private boolean isImage(String key) {
        String lowerKey = key.toLowerCase();
        return lowerKey.endsWith(".jpg") ||
                lowerKey.endsWith(".jpeg") ||
                lowerKey.endsWith(".png") ||
                lowerKey.endsWith(".gif") ||
                lowerKey.endsWith(".webp");
    }
}
