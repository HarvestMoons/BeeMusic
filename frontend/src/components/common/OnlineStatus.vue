<!-- src/components/OnlineStatus.vue -->
<template>
  <div class="online-status">
    在线 {{ onlineCount }} 人
    <span v-if="sameSongCount >= 1" class="same-song">
      · {{ sameSongCount }} 人正在听
    </span>
  </div>
</template>

<!-- OnlineStatus.vue -->
<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const onlineCount = ref(0)
const sameSongCount = ref(0)
const currentSongName = ref('')

let ws = null

onMounted(() => {
  // 用相对路径，走 Nginx 代理
  ws = new WebSocket(`ws://${location.host}/ws/online`)

  window.ws=ws;

  ws.onopen = () => {
    console.log('实时在线人数连接成功！')
  }

  ws.onmessage = (e) => {
    try {
      const data = JSON.parse(e.data)
      onlineCount.value = data.onlineCount || 0

      if (window.currentSongId && data.songListeners?.[window.currentSongId]) {
        sameSongCount.value = Number(data.songListeners[window.currentSongId])
        currentSongName.value = window.currentSongName || '这首歌'
      } else {
        sameSongCount.value = 0
      }
    } catch (err) {
      console.warn('WebSocket 数据解析失败', err)
    }
  }

  ws.onerror = (err) => {
    console.warn('WebSocket 连接出错', err)
  }

  ws.onclose = () => {
    console.log('WebSocket 断开，3秒后重连...')
    setTimeout(() => {
      ws = new WebSocket(`ws://${location.host}/ws/online`)
    }, 3000)
  }
})

onUnmounted(() => {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.close()
  }
})
</script>

<style scoped>
.online-status {
  font-size: 13px;
  color: var(--text-color);
  opacity: 0.7;
}

.same-song {
  color: var(--primary-color);
  font-weight: 600;
}
</style>