<!-- src/components/OnlineStatus.vue -->
<template>
  <div class="online-status">
    在线 {{ onlineCount }} 人
    <span v-if="sameSongCount === '-' || sameSongCount > 0" class="same-song">
      · {{ sameSongCount }} 人正在听
    </span>
  </div>
</template>

<!-- OnlineStatus.vue -->
<script setup>
import {onMounted, onUnmounted, ref} from 'vue'
import {eventBus} from '@/utils/eventBus.js'

const onlineCount = ref('-')
const sameSongCount = ref('-')
const currentSongName = ref('')
const currentSongId = ref('')

let ws = null
let reconnectTimer = null

function getOnlineWsUrl() {
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${location.host}/ws/online`
}

function sendCurrentSongToServer() {
  if (!ws || ws.readyState !== WebSocket.OPEN || !currentSongId.value) {
    return
  }

  ws.send(JSON.stringify({
    songId: currentSongId.value,
    songName: currentSongName.value
  }))
}

function handleSongChanged({songId, songName} = {}) {
  currentSongId.value = songId ? String(songId) : ''
  currentSongName.value = songName || ''
  sendCurrentSongToServer()
}

function connectWebSocket() {
  ws = new WebSocket(getOnlineWsUrl())

  ws.onopen = () => {
    console.log('实时在线人数连接成功！')
    sendCurrentSongToServer()
  }

  ws.onmessage = (e) => {
    try {
      const data = JSON.parse(e.data)
      onlineCount.value = data.onlineCount || 0

      if (currentSongId.value && data.songListeners?.[currentSongId.value]) {
        sameSongCount.value = Number(data.songListeners[currentSongId.value])
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
    reconnectTimer = setTimeout(() => {
      connectWebSocket()
    }, 3000)
  }
}

onMounted(() => {
  eventBus.on('player-song-changed', handleSongChanged)
  connectWebSocket()
})

onUnmounted(() => {
  eventBus.off('player-song-changed', handleSongChanged)
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    ws.close()
  }
  ws = null
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