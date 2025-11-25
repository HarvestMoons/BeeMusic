<template>
  <div class="player-container">
    <div class="song-info-container">
      <div class="song-info">
        <h2 id="current-song">
          <template v-if="currentSongInfo.bv">
            <span class="song-title">{{ currentSongInfo.title }}</span>
            <a :href="`https://www.bilibili.com/video/${currentSongInfo.bv}/`" target="_blank" class="portal-link">
              <img :src="portalIcon" alt="传送门" class="portal-icon" />
            </a>
          </template>
          <template v-else>
            {{ currentSongInfo.title }}
          </template>
        </h2>
        
        <OnlineStatus />
        
        <VoteControls
            v-if="currentSong"
            :songId="currentSong.id"
        />
        <Playlist
            :playlist="playlist"
            :currentSongId="currentSong?.id"
            @select="handleSelectSong"
        />
        
        <audio 
          ref="audioRef" 
          controls
          crossorigin="anonymous"
          @ended="handlePlaybackEnd"
          @error="handleAudioError"
          @volumechange="handleVolumeChange"
          @ratechange="handleRateChangeNative"
          @loadedmetadata="handleLoadedMetadata"
        ></audio>
      </div>

      <div class="controls">
        <button id="play-btn" @click="handlePlayClick">随便听听</button>
        <button id="prev-btn" @click="playPreviousSong">上一首</button>
        <button id="toggleSpectrumBtn">显示频谱</button>

        <select id="play-mode" v-model="playMode">
          <option value="random">连播模式：随机播放</option>
          <option value="loop-list">连播模式：列表循环</option>
          <option value="single-loop">连播模式：单曲循环</option>
        </select>

        <select id="folder-selector" v-model="currentFolder" @change="handleFolderChange">
          <option value="ha_ji_mi">基米天堂</option>
          <option value="dian_gun">溜冰密室</option>
          <option value="da_si_ma">起飞基地</option>
          <option value="ding_zhen">烟雾缭绕</option>
          <option value="dxl">东洋雪莲</option>
        </select>
        
        <PlaybackRateControl v-model="playbackRate" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, computed, watch, onUnmounted } from 'vue'
import portalIcon from '@/assets/icons/protal.svg'
import VoteControls from "./VoteControls.vue";
import PlaybackRateControl from "./PlaybackRateControl.vue";
import { PUBLIC_API_BASE } from '@/constants';
import { useKeyboardShortcuts } from "../../../composables/useKeyboardShortcuts.js";
import OnlineStatus from "../../common/OnlineStatus.vue";

const DEFAULT_FOLDER = 'ha_ji_mi';
const STORAGE_KEYS = {
  VOLUME: 'music_volume',
  PLAYLIST_PREFIX: 'music_playlist_',
  SELECTED_FOLDER: 'music_selected_folder_folder-selector',
  PLAYBACK_RATE_PREFIX: 'music_playback_rate_'
};

// State
const audioRef = ref(null);
const playlist = ref([]);
const currentIndex = ref(-1);
const historyStack = ref([]);
const playbackRate = ref(1.0);
const playMode = ref('random');
const currentFolder = ref(DEFAULT_FOLDER);

// Computed
const currentSong = computed(() => playlist.value[currentIndex.value]);
const currentSongInfo = computed(() => {
  if (!currentSong.value) return { title: '当前未播放', bv: null };
  return parseSongNameWithBv(currentSong.value.name);
});

// Storage Helpers
const getStorage = (key, def = null) => localStorage.getItem(key) || def;
const setStorage = (key, val) => localStorage.setItem(key, val);
const getRateKey = (folder) => STORAGE_KEYS.PLAYBACK_RATE_PREFIX + folder;

// Initialization
onMounted(async () => {
  // Restore Volume
  const savedVol = parseFloat(getStorage(STORAGE_KEYS.VOLUME));
  if (!isNaN(savedVol) && audioRef.value) audioRef.value.volume = Math.min(1, Math.max(0, savedVol));

  // Restore Folder
  const savedFolder = getStorage(STORAGE_KEYS.SELECTED_FOLDER);
  if (savedFolder) currentFolder.value = savedFolder;

  // Restore Rate
  const savedRate = parseFloat(getStorage(getRateKey(currentFolder.value)));
  if (!isNaN(savedRate)) playbackRate.value = savedRate;
  if (audioRef.value) audioRef.value.playbackRate = playbackRate.value;

  // Setup Shortcuts
  useKeyboardShortcuts(
      () => audioRef.value,
      () => audioRef.value?.paused ? audioRef.value.play() : audioRef.value.pause(),
      playNextInOrder,
      playPrevInOrder,
      () => audioRef.value && (audioRef.value.muted = !audioRef.value.muted),
      playRandomSong,
      handlePlayClick
  );

  // Sync Storage
  window.addEventListener('storage', handleStorageEvent);

  // Initial Load
  await setFolder(currentFolder.value);
  playRandomSong();
});

onUnmounted(() => {
  window.removeEventListener('storage', handleStorageEvent);
});

// Logic
async function setFolder(folder) {
  try {
    document.body.classList.add('loading');
    const res = await fetch(`${PUBLIC_API_BASE}/songs/set-folder`, {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({folder})
    });
    const result = await res.json();

    if (result.status === 'ok') {
      historyStack.value = [];
      await fetchSongList();
      
      // Restore rate for this folder
      const savedRate = parseFloat(getStorage(getRateKey(folder)));
      if (!isNaN(savedRate)) {
        playbackRate.value = savedRate;
        if (audioRef.value) audioRef.value.playbackRate = savedRate;
      }

      if (playlist.value.length > 0) {
        playSongAtIndex(0);
      } else {
        if (audioRef.value) audioRef.value.src = '';
      }
    } else {
      alert('切换失败：' + result.error);
    }
  } catch (err) {
    console.error('切换音乐文件夹失败', err);
    alert('请求失败: ' + err.message);
  } finally {
    document.body.classList.remove('loading');
  }
}

async function fetchSongList() {
  try {
    const res = await fetch(`${PUBLIC_API_BASE}/songs/get`);
    const data = await res.json();
    playlist.value = shuffleArray(data || []);
  } catch (err) {
    playlist.value = [];
  }
}

function playSongAtIndex(index, fromHistory = false) {
  if (index < 0 || index >= playlist.value.length) return;

  if (!fromHistory && currentIndex.value !== -1 && currentIndex.value !== index) {
    historyStack.value.push(currentIndex.value);
  }

  currentIndex.value = index;
  const song = playlist.value[index];

  // Global State for OnlineStatus
  window.currentSongId = String(song.id);
  window.currentSongName = currentSongInfo.value.title;

  if (window.ws && window.ws.readyState === WebSocket.OPEN) {
    window.ws.send(JSON.stringify({
      songId: String(song.id),
      songName: window.currentSongName
    }));
  }

  if (audioRef.value) {
    audioRef.value.src = song.url;
    // Ensure rate is applied
    audioRef.value.playbackRate = playbackRate.value;
    audioRef.value.play().catch(console.warn);
  }
}

function playRandomSong() {
  if (!playlist.value.length) return;
  playSongAtIndex(Math.floor(Math.random() * playlist.value.length));
}

function handlePlayClick() {
  if (playMode.value === 'loop-list' && playlist.value.length > 0) {
    playSongAtIndex((currentIndex.value + 1) % playlist.value.length);
  } else {
    playRandomSong();
  }
}

function playPreviousSong() {
  if (historyStack.value.length === 0) {
    alert("没有上一首了！");
    return;
  }
  playSongAtIndex(historyStack.value.pop(), true);
}

function handlePlaybackEnd() {
  if (playMode.value === 'single-loop') {
    if (audioRef.value) {
      audioRef.value.currentTime = 0;
      audioRef.value.play();
    }
  } else if (playMode.value === 'loop-list') {
    playSongAtIndex((currentIndex.value + 1) % playlist.value.length);
  } else {
    playRandomSong();
  }
}

function handleAudioError() {
  console.warn(`❌ 无法播放，尝试下一首`);
  setTimeout(() => {
    if (playMode.value === 'loop-list') {
      playSongAtIndex((currentIndex.value + 1) % playlist.value.length);
    } else {
      playRandomSong();
    }
  }, 1000);
}

function handleFolderChange() {
  setStorage(STORAGE_KEYS.SELECTED_FOLDER, currentFolder.value);
  setFolder(currentFolder.value);
}

function handleVolumeChange() {
  if (audioRef.value) setStorage(STORAGE_KEYS.VOLUME, audioRef.value.volume);
}

function handleRateChangeNative() {
  if (audioRef.value) {
    playbackRate.value = audioRef.value.playbackRate;
    setStorage(getRateKey(currentFolder.value), playbackRate.value);
  }
}

function handleLoadedMetadata() {
  if (audioRef.value) audioRef.value.playbackRate = playbackRate.value;
}

// Watchers
watch(playbackRate, (val) => {
  if (audioRef.value) audioRef.value.playbackRate = val;
  setStorage(getRateKey(currentFolder.value), val);
});

function handleStorageEvent(e) {
  if (!e.key) return;
  if (e.key === STORAGE_KEYS.VOLUME && audioRef.value) {
    const v = parseFloat(e.newValue);
    if (!isNaN(v)) audioRef.value.volume = v;
  }
  if (e.key === getRateKey(currentFolder.value)) {
    const v = parseFloat(e.newValue);
    if (!isNaN(v)) {
      playbackRate.value = v;
      if (audioRef.value) audioRef.value.playbackRate = v;
    }
  }
}

// Utils
function parseSongNameWithBv(name) {
  const clean = name.replace(/\.mp3$/, '');
  const match = clean.match(/^(.*?)_?(BV[0-9A-Za-z]+)/);
  return match ? {title: match[1], bv: match[2]} : {title: clean, bv: null};
}

function shuffleArray(array) {
  const newArr = [...array];
  for (let i = newArr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [newArr[i], newArr[j]] = [newArr[j], newArr[i]];
  }
  return newArr;
}

function playNextInOrder() {
  if (!playlist.value.length) return;
  playSongAtIndex((currentIndex.value + 1) % playlist.value.length);
}

function playPrevInOrder() {
  if (!playlist.value.length) return;
  const prev = currentIndex.value <= 0 ? playlist.value.length - 1 : currentIndex.value - 1;
  playSongAtIndex(prev);
}

function handleSelectSong(songId) {
  const idx = playlist.value.findIndex(s => s.id === songId);
  if (idx !== -1) playSongAtIndex(idx);
}

defineExpose({
  playlist,
  currentIndex,
  playSongAtIndex,
  handleSelectSong
});
</script>

<style scoped>
.player-container {
  flex: 2 1 auto;
  background-color: var(--playlist-bg);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid var(--border-color);
  transition: background-color 0.3s ease, border-color 0.3s ease;
}

.controls {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  margin-top: 15px;
}

.controls button,
.controls select {
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  font-size: 14px;
  background-color: var(--playlist-item-bg);
  color: var(--text-color);
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.controls button:hover {
  background-color: var(--playlist-item-hover-bg);
  color: var(--playlist-item-hover-text);
}

button {
  padding: 10px 18px;
  background-color: var(--primary-color);
  color: #fff; /* Keep white or use variable if needed */
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
  transition: background-color 0.2s ease;
}

button:hover {
  opacity: 0.9;
}

.song-info {
  margin: 24px 0;
  padding: 16px;
  background-color: var(--playlist-item-bg);
  border-radius: 6px;
  border: 1px solid var(--border-color);
}

.song-info h2 {
  color: var(--song-title-color);
  margin-top: 0;
}

audio {
  width: 100%;
  margin-top: 12px;
}

.song-info-container {
  flex: 2 1 auto;
}

</style>