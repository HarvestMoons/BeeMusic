<!-- src/components/OnlineStatus.vue -->
<template>
  <div class="online-status">
    在线 {{ onlineCount }} 人
    <span v-if="sameSongCount === '-' || sameSongCount > 0" class="same-song">
      · {{ sameSongCount }} 人正在听
    </span>
  </div>
</template>

<script>
// 页面生命周期内固定，登录、登出和重连均复用；不同标签页各自生成。
const onlineClientId = crypto.randomUUID()
</script>

<script setup>
import {onMounted, onUnmounted, ref} from 'vue'
import {eventBus} from '@/utils/eventBus.js'

const onlineCount = ref('-')
const sameSongCount = ref('-')
const currentSongName = ref('')
const currentSongId = ref('')

let ws = null
let reconnectTimer = null
let heartbeatTimer = null
let isActive = false

function getOnlineWsUrl() {
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${location.host}/ws/online?clientId=${onlineClientId}`
}

function sendCurrentSongToServer() {
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    return
  }

  ws.send(JSON.stringify({
    songId: currentSongId.value || null,
    songName: currentSongName.value
  }))
}

function handleSongChanged({songId, songName} = {}) {
  currentSongId.value = songId ? String(songId) : ''
  currentSongName.value = songName || ''
  sendCurrentSongToServer()
}

function connectWebSocket() {
  if (!isActive || ws?.readyState === WebSocket.OPEN || ws?.readyState === WebSocket.CONNECTING) {
    return
  }

  clearTimeout(reconnectTimer)
  reconnectTimer = null
  const socket = new WebSocket(getOnlineWsUrl())
  ws = socket

  socket.onopen = () => {
    if (!isActive || ws !== socket) { socket.close(); return }
    console.log('实时在线人数连接成功！')
    sendCurrentSongToServer()
  }

  socket.onmessage = (e) => {
    if (!isActive || ws !== socket) return
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

  socket.onerror = (err) => {
    console.warn('WebSocket 连接出错', err)
  }

  socket.onclose = () => {
    if (!isActive || ws !== socket) {
      return
    }

    ws = null
    console.log('WebSocket 断开，3秒后重连...')
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      connectWebSocket()
    }, 3000)
  }
}

function disconnect() {
  isActive = false
  clearTimeout(reconnectTimer)
  reconnectTimer = null
  const socket = ws
  ws = null
  if (socket) socket.close()
}

function resume() {
  isActive = true
  connectWebSocket()
}

onMounted(() => {
  eventBus.on('player-song-changed', handleSongChanged)
  window.addEventListener('pagehide', disconnect)
  window.addEventListener('pageshow', resume)
  resume()
  heartbeatTimer = setInterval(sendCurrentSongToServer, 30000)
})

onUnmounted(() => {
  disconnect()
  eventBus.off('player-song-changed', handleSongChanged)
  window.removeEventListener('pagehide', disconnect)
  window.removeEventListener('pageshow', resume)
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
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
