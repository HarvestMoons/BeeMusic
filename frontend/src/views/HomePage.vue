<template>
  <div class="home-page">
    <div class="layout">
      <Sidebar />

      <!-- 左侧歌单 -->
      <Playlist
          :playlist="playerRef?.playlist"
          :currentSongId="playerRef?.playlist[playerRef?.currentIndex]?.id"
          @select="handlePlaylistClick"
      />

      <!-- 右侧：播放器 + 频谱 -->
      <div class="right-container">
        <Player ref="playerRef" />
      </div>
    </div>

    <BackgroundParticles zIndex="0" opacity="0.4" color="0,0,0" :count="99" />
    <BackgroundRipple apiBase="/api" />
    <div v-if="loading">
      <MySpinner />
    </div>
  </div>
</template>

<script setup>
import {ref} from 'vue'
import Player from '../components/feature/player/Player.vue'
import Playlist from '../components/feature/player/Playlist.vue'
import Sidebar from '../components/layout/Sidebar.vue'
import BackgroundParticles from '../components/effects/BackgroundParticles.vue'
import BackgroundRipple from '../components/effects/BackgroundRipple.vue'
import MySpinner from "../components/effects/MySpinner.vue";

const playerRef = ref(null)

// Playlist 点击播放
function handlePlaylistClick(songId) {
  if (playerRef.value) {
    playerRef.value.handleSelectSong(songId)  // 通过 song.id 找索引播放
  }
}
</script>

<style scoped>
.home-page {
  font-family: 'Arial', sans-serif;
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  color: #333;
}

.layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.right-container {
  display: flex;
  flex-direction: column;
  flex: 1;
  gap: 20px;
  align-items: stretch;
}
</style>
