#!/bin/bash
echo "开始部署音乐播放器..."
docker pull beecool/music-player:latest
docker pull beecool/music-player-frontend:latest
docker compose -f docker-compose.images.yml up -d --force-recreate


echo "部署完成!应用访问地址: http://8.155.47.138:8081"