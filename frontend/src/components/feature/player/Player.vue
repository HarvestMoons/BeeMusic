<template>
  <div class="player-wrapper">
    <div class="player-container">
      <Toast :visible="showToast" :message="toastMessage" />
      <div class="song-info-container">
        <FolderSelector v-model="selectedFolder" @change="handleFolderChange" />
        <div class="song-info">
          <h2 id="current-song">
            <span v-if="!currentSongInfo.title">当前未播放</span>
            <template v-else>
              <span class="song-title">{{ currentSongInfo.title }}</span>
              <a v-if="currentSongInfo.bv" :href="`https://www.bilibili.com/video/${currentSongInfo.bv}/`" target="_blank" class="portal-link">
                <img :src="portalIcon" alt="传送门" class="portal-icon" />
              </a>
            </template>
          </h2>
          <div class="meta-info">
            <VoteControls
                v-if="playlist[currentIndex]"
                :songId="playlist[currentIndex].id"
            />
            <OnlineStatus />
          </div>
          <Playlist
              :playlist="playlist"
              :currentSongId="playlist[currentIndex]?.id"
              @select="handleSelectSong"
          />
          <audio 
            id="audio-player" 
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
          <button id="toggleSpectrumBtn" @click="toggleSpectrum" :title="showSpectrum ? '隐藏频谱' : '显示频谱'">
            <img :src="spectrumIcon" alt="频谱" class="spectrum-icon" />
          </button>

          <button id="play-mode-btn" @click="cyclePlayMode" :title="playModeText">
            <img :src="currentPlayModeIcon" alt="模式" class="play-mode-icon" />
          </button>

          <PlaybackRateControl v-model="playbackRate" />
        </div>
      </div>
    </div>

    <!-- 评论侧边栏 + 频谱 -->
    <div class="sidebar-wrapper" :class="{ collapsed: !showComments }">
      <CommentDrawer 
        :visible="showComments" 
        :songId="playlist[currentIndex]?.id"
        @close="toggleComments"
        class="comment-drawer-flex"
      />
      <SpectrumVisualizer :visible="showSpectrum" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch, onUnmounted, computed } from 'vue'
import portalIcon from '@/assets/icons/protal.svg'
import randomIcon from '@/assets/icons/play_mode/random.svg'
import loopIcon from '@/assets/icons/play_mode/loop.svg'
import singleLoopIcon from '@/assets/icons/play_mode/repeat.svg'
import spectrumIcon from '@/assets/icons/spectrum.svg'
import VoteControls from "./VoteControls.vue";
import PlaybackRateControl from "./PlaybackRateControl.vue";
import FolderSelector from "./FolderSelector.vue";
import Toast from "../../common/Toast.vue";
import CommentDrawer from "./CommentDrawer.vue";
import SpectrumVisualizer from "../spectrum/SpectrumVisualizer.vue";
import { PUBLIC_API_BASE } from '@/constants';
import {useKeyboardShortcuts} from "../../../composables/useKeyboardShortcuts.js";
import OnlineStatus from "../../common/OnlineStatus.vue";

const DEFAULT_FOLDER = 'ha_ji_mi';
const STORAGE_KEYS = {
  VOLUME: 'music_volume',
  PLAYLIST_PREFIX: 'music_playlist_',
  SELECTED_FOLDER_PREFIX: 'music_selected_folder_',
  PLAYBACK_RATE_PREFIX: 'music_playback_rate_',
  SHOW_COMMENTS: 'music_show_comments'
};

// Refs
const audioRef = ref(null)
const playlist = ref([])
const currentIndex = ref(-1)
const historyStack = ref([])
const playbackRate = ref(1.0)
const playMode = ref('random')

const playModeIcons = {
  'random': randomIcon,
  'loop-list': loopIcon,
  'single-loop': singleLoopIcon
}
const currentPlayModeIcon = computed(() => playModeIcons[playMode.value])
const playModeText = computed(() => {
  const map = {
    'random': '连播模式：随机播放',
    'loop-list': '连播模式：列表循环',
    'single-loop': '连播模式：单曲循环'
  }
  return map[playMode.value]
})

function cyclePlayMode() {
  const modes = ['random', 'loop-list', 'single-loop']
  const nextIndex = (modes.indexOf(playMode.value) + 1) % modes.length
  playMode.value = modes[nextIndex]
}

const selectedFolder = ref(DEFAULT_FOLDER)
const currentSongInfo = ref({ title: '', bv: null })
const toastMessage = ref('')
const showToast = ref(false)
const showComments = ref(true)
const showSpectrum = ref(false)
let toastTimer = null

function toggleComments() {
  if (showComments.value) {
    // 收起评论区时，同时关闭频谱
    showComments.value = false
    showSpectrum.value = false
  } else {
    showComments.value = true
  }
  localStorage.setItem(STORAGE_KEYS.SHOW_COMMENTS, String(showComments.value))
}

function toggleSpectrum() {
  if (showSpectrum.value) {
    showSpectrum.value = false
  } else {
    // 展开频谱时，强制展开评论区
    showSpectrum.value = true
    showComments.value = true
  }
}

function showToastMessage(msg) {
  toastMessage.value = msg
  showToast.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    showToast.value = false
  }, 2000)
}

// Storage Helpers
const makeSelectedFolderKey = (id) => STORAGE_KEYS.SELECTED_FOLDER_PREFIX + id
const makePlaybackRateKey = (folder) => STORAGE_KEYS.PLAYBACK_RATE_PREFIX + folder

function saveVolumeToStorage(vol) {
  try { localStorage.setItem(STORAGE_KEYS.VOLUME, String(vol)); } catch (e) {}
}
function loadVolumeFromStorage() {
  try {
    const v = parseFloat(localStorage.getItem(STORAGE_KEYS.VOLUME));
    if (!Number.isNaN(v)) return Math.min(1, Math.max(0, v));
  } catch (e) {}
  return null;
}
function saveSelectedFolder(id, folder) {
  try { localStorage.setItem(makeSelectedFolderKey(id), folder); } catch (e) {}
}
function loadSelectedFolder(id) {
  try { return localStorage.getItem(makeSelectedFolderKey(id)); } catch (e) {}
}
function savePlaybackRateForFolder(folder) {
  try { localStorage.setItem(makePlaybackRateKey(folder), String(playbackRate.value)); } catch (e) {}
}
function loadPlaybackRateForFolder(folder) {
  try {
    const raw = localStorage.getItem(makePlaybackRateKey(folder));
    const v = parseFloat(raw);
    if (!Number.isNaN(v)) return v;
  } catch (e) {}
  return null;
}

// Event Handlers
function handleVolumeChange() {
  if (audioRef.value) saveVolumeToStorage(audioRef.value.volume)
}

function handleRateChangeNative() {
  if (!audioRef.value) return
  playbackRate.value = audioRef.value.playbackRate
  savePlaybackRateForFolder(selectedFolder.value)
}

function handleLoadedMetadata() {
  if (audioRef.value) {
    try { audioRef.value.playbackRate = playbackRate.value } catch (e) {}
  }
}

// Watchers
watch(playbackRate, (val) => {
  if (audioRef.value) audioRef.value.playbackRate = val
  savePlaybackRateForFolder(selectedFolder.value)
})

// Logic
async function handleFolderChange() {
  saveSelectedFolder('folder-selector', selectedFolder.value)
  await setFolder(selectedFolder.value)
}

async function setFolder(folder) {
  try {
    document.body.classList.add('loading')
    const res = await fetch(`${PUBLIC_API_BASE}/songs/set-folder`, {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({folder})
    });
    const result = await res.json();

    if (result.status === 'ok') {
      historyStack.value = [];
      await fetchSongList(folder);

      const savedRate = loadPlaybackRateForFolder(folder);
      if (savedRate != null) {
        playbackRate.value = savedRate;
        if (audioRef.value) audioRef.value.playbackRate = savedRate;
      }

      if (playlist.value.length > 0) {
        playSongAtIndex(0);
      } else {
        currentSongInfo.value = { title: '', bv: null }
        if (audioRef.value) audioRef.value.src = '';
      }
    } else {
      alert('切换失败：' + result.error);
    }
  } catch (err) {
    console.error('切换音乐文件夹失败', err);
    alert('请求失败: ' + err.message);
  } finally {
    document.body.classList.remove('loading')
  }
}

async function fetchSongList() {
  try {
    const res = await fetch(`${PUBLIC_API_BASE}/songs/get`);
    const data = await res.json();
    playlist.value = shuffleArray(data || []);
    return playlist.value;
  } catch (err) {
    playlist.value = [];
    return [];
  }
}

function shuffleArray(array) {
  const newArr = [...array];
  for (let i = newArr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [newArr[i], newArr[j]] = [newArr[j], newArr[i]];
  }
  return newArr;
}

function playSongAtIndex(index, fromHistory = false) {
  if (index < 0 || index >= playlist.value.length) {
    currentSongInfo.value = { title: '播放失败：索引越界', bv: null }
    return;
  }

  if (!fromHistory && currentIndex.value !== -1 && currentIndex.value !== index) {
    historyStack.value.push(currentIndex.value);
  }

  currentIndex.value = index;
  const song = playlist.value[index];

  // Global state for OnlineStatus
  window.currentSongId = String(song.id)
  const parsed = parseSongNameWithBv(song.name);
  window.currentSongName = parsed.title || song.name.replace(/\.mp3$/, '')
  currentSongInfo.value = parsed

  if (window.ws && window.ws.readyState === WebSocket.OPEN) {
    window.ws.send(JSON.stringify({
      songId: String(song.id),
      songName: window.currentSongName
    }))
  }

  if (audioRef.value) {
    audioRef.value.src = song.url;
    // Ensure rate is applied
    try { audioRef.value.playbackRate = playbackRate.value; } catch (e) {}
    
    audioRef.value.play().then(() => {
      // 播放成功后，增加播放次数
      fetch(`${PUBLIC_API_BASE}/songs/play/${song.id}`, { method: 'POST' }).catch(console.error);
    }).catch(err => {
      console.warn('播放未启动：', err);
    });
  }
}

function parseSongNameWithBv(name) {
  const clean = name.replace(/\.mp3$/, '');
  const match = clean.match(/^(.*?)_?(BV[0-9A-Za-z]+)/);
  return match ? {title: match[1], bv: match[2]} : {title: clean, bv: null};
}

function playPreviousSong() {
  if (historyStack.value.length === 0) {
    showToastMessage("没有上一首了");
    return;
  }
  const prev = historyStack.value.pop();
  playSongAtIndex(prev, true);
}

function handlePlaybackEnd() {
  if (!audioRef.value) return
  const diff = Math.abs(audioRef.value.currentTime - audioRef.value.duration);
  if (audioRef.value.duration > 0 && diff < 0.5) {
    if (playMode.value === 'single-loop') {
      audioRef.value.currentTime = 0;
      audioRef.value.play();
    } else if (playMode.value === 'loop-list') {
      const next = (currentIndex.value + 1) % playlist.value.length;
      playSongAtIndex(next);
    } else {
      playRandomSong();
    }
  }
}

function handleAudioError() {
  console.warn(`❌ 无法播放：${playlist.value[currentIndex.value]?.name}，尝试下一首`);
  setTimeout(() => {
    if (playMode.value === 'loop-list') {
      const next = (currentIndex.value + 1) % playlist.value.length;
      playSongAtIndex(next);
    } else {
      playRandomSong();
    }
  }, 1000);
}

function playRandomSong() {
  if (!playlist.value || playlist.value.length === 0) {
    currentSongInfo.value = { title: '播放失败：歌曲列表为空', bv: null }
    return;
  }
  const rand = Math.floor(Math.random() * playlist.value.length);
  playSongAtIndex(rand);
}

function handlePlayClick() {
  if (playMode.value === 'loop-list' && playlist.value.length > 0) {
    const next = (currentIndex.value + 1) % playlist.value.length;
    playSongAtIndex(next);
  } else {
    playRandomSong();
  }
}

function handleSelectSong(songId) {
  const idx = playlist.value.findIndex(s => s.id === songId)
  if (idx !== -1) {
    playSongAtIndex(idx)
  }
}

// Keyboard shortcuts helpers
function playNextInOrder() {
  if (playlist.value.length === 0) return
  const next = (currentIndex.value + 1) % playlist.value.length
  playSongAtIndex(next)
}

function playPrevInOrder() {
  if (playlist.value.length === 0 || currentIndex.value === -1) return
  const prev = currentIndex.value === 0
      ? playlist.value.length - 1
      : currentIndex.value - 1
  playSongAtIndex(prev)
}

// Storage Sync
function handleStorageEvent(e) {
  if (!e.key) return;

  if (e.key === STORAGE_KEYS.VOLUME) {
    const v = parseFloat(e.newValue);
    if (!Number.isNaN(v) && audioRef.value) audioRef.value.volume = v;
  }

  const curFolder = selectedFolder.value;
  if (e.key === makePlaybackRateKey(curFolder)) {
    const v = parseFloat(e.newValue);
    if (!Number.isNaN(v)) {
      playbackRate.value = v;
      if (audioRef.value) audioRef.value.playbackRate = v;
    }
  }

  if (e.key && e.key.startsWith(STORAGE_KEYS.SELECTED_FOLDER_PREFIX)) {
    const selectorId = e.key.substring(STORAGE_KEYS.SELECTED_FOLDER_PREFIX.length);
    // Assuming selectorId is 'folder-selector'
    if (selectorId === 'folder-selector') {
       if (e.newValue !== selectedFolder.value) {
         selectedFolder.value = e.newValue
         setFolder(e.newValue).catch(() => {})
       }
    }
  }
}

onMounted(async () => {
  // Init Volume
  const savedVol = loadVolumeFromStorage();
  if (audioRef.value) audioRef.value.volume = savedVol != null ? savedVol : 0.2;

  // Init Folder
  const savedFolder = loadSelectedFolder('folder-selector');
  if (savedFolder) selectedFolder.value = savedFolder;

  // Init Rate
  const savedRate = loadPlaybackRateForFolder(selectedFolder.value);
  if (savedRate != null) playbackRate.value = savedRate;
  if (audioRef.value) audioRef.value.playbackRate = playbackRate.value;

  // Init Comments Visibility
  const savedShowComments = localStorage.getItem(STORAGE_KEYS.SHOW_COMMENTS);
  if (savedShowComments !== null) {
    showComments.value = savedShowComments === 'true';
  }

  window.addEventListener('storage', handleStorageEvent);

  await setFolder(selectedFolder.value);

  useKeyboardShortcuts(
      () => audioRef.value,
      () => audioRef.value && (audioRef.value.paused ? audioRef.value.play() : audioRef.value.pause()),
      playNextInOrder,
      playPrevInOrder,
      () => audioRef.value && (audioRef.value.muted = !audioRef.value.muted),
      playRandomSong,
      handlePlayClick
  )
});

onUnmounted(() => {
  window.removeEventListener('storage', handleStorageEvent)
})

defineExpose({
  playlist,
  currentIndex,
  playSongAtIndex,
  handleSelectSong
})
</script>

<style scoped>
.player-wrapper {
  display: flex;
  gap: 16px;
  flex: 1;
  overflow: hidden;
  height: 100%;
  --content-height: 750px;
}

.player-container {
  position: relative;
  flex: 1;
  background-color: var(--playlist-bg);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid var(--border-color);
  transition: background-color 0.3s ease, border-color 0.3s ease;
  display: flex;
  flex-direction: column;
  min-width: 0; /* 防止 flex 子项溢出 */
  overflow-y: auto; /* 允许左侧滚动，如果内容超出 */
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

.meta-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

audio {
  width: 100%;
  margin-top: 12px;
}
.song-info-container {
  flex: 2 1 auto;
}

.sidebar-wrapper {
  width: 400px;
  height: var(--content-height);
  display: flex;
  flex-direction: column;
  background: var(--playlist-bg);
  border-left: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.sidebar-wrapper.collapsed {
  width: 60px;
}

.sidebar-wrapper.collapsed :deep(.drawer-header h3) {
  display: none;
}

.sidebar-wrapper.collapsed :deep(.drawer-header) {
  justify-content: center;
  padding: 16px 0;
}

.sidebar-wrapper.collapsed :deep(.spectrum-container) {
  display: none;
}

:deep(.spectrum-container) {
  border-top: 1px solid var(--border-color);
}

.comment-drawer-flex {
  flex: 1;
  min-height: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
}

:deep(.comment-sidebar) {
  height: 100%;
  display: flex;
  flex-direction: column;
  width: 100%;
  background: transparent; /* Use wrapper background */
  box-shadow: none; /* Remove drawer shadow if any */
  transform: none !important; /* Disable drawer transform animation */
}

/* Remove border from header when comments are collapsed to avoid double border with spectrum */
:deep(.comment-sidebar.collapsed .drawer-header) {
  border-bottom: none;
}

:deep(.comment-content-wrapper) {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

:deep(.comment-list) {
  flex: 1;
  overflow-y: auto;
}

.song-info-container {
  flex: 2 1 auto;
}

.play-mode-icon,
.spectrum-icon {
  width: 20px;
  height: 20px;
  display: block;
  filter: var(--vote-icon-filter);
  transition: filter 0.3s ease;
}
</style>