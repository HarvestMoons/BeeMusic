<template>
  <div class="player-container">
    <div class="song-info-container">
      <div class="song-info">
        <h2 id="current-song">当前未播放</h2>
        <VoteControls
            v-if="playlist[currentIndex]"
            :songId="playlist[currentIndex].id"
        />
        <Playlist
            :playlist="playlist"
            :currentSongId="playlist[currentIndex]?.id"
            @select="handleSelectSong"
        />
        <audio id="audio-player" controls></audio>
      </div>

      <div class="controls">
        <button id="play-btn">随便听听</button>
        <button id="prev-btn">上一首</button>
        <button id="toggleSpectrumBtn">显示频谱</button>

        <select id="play-mode">
          <option value="random">连播模式：随机播放</option>
          <option value="loop-list">连播模式：列表循环</option>
          <option value="single-loop">连播模式：单曲循环</option>
        </select>

        <select id="folder-selector">
          <option value="ha_ji_mi">基米天堂</option>
          <option value="dian_gun">溜冰密室</option>
          <option value="da_si_ma">起飞基地</option>
          <option value="ding_zhen">烟雾缭绕</option>
          <option value="dxl">东洋雪莲</option>
        </select>
        <PlaybackRateControl v-model="playbackRate" @change="handleRateChange" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import portalIcon from '@/assets/icons/protal.svg'
import VoteControls from "./VoteControls.vue";
import PlaybackRateControl from "./PlaybackRateControl.vue";
import { PUBLIC_API_BASE } from '@/constants';

const DEFAULT_FOLDER = 'ha_ji_mi';

// STORAGE key 定义（注意：playbackRate 按 folder 存）
const STORAGE_KEYS = {
  VOLUME: 'music_volume',
  PLAYLIST_PREFIX: 'music_playlist_',         // + folder
  SELECTED_FOLDER_PREFIX: 'music_selected_folder_', // + selectorId
  PLAYBACK_RATE_PREFIX: 'music_playback_rate_' // + folder
};

// 响应式状态
const playlist = ref([]);           // 歌曲列表
const currentIndex = ref(-1);       // 当前播放索引（不再持久化）
const historyStack = ref([]);       // 历史播放栈
const playbackRate = ref(1.0)

// DOM元素
let audioPlayer, currentSongEl, playBtn, prevBtn, playModeSelect, folderSelector;


// ------------------- 存储与恢复 -------------------
function makeSelectedFolderKey(selectorId) { return STORAGE_KEYS.SELECTED_FOLDER_PREFIX + selectorId; }
function makePlaybackRateKey(folder) { return STORAGE_KEYS.PLAYBACK_RATE_PREFIX + folder; }

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

// 保存/加载 selector 的选中 folder（支持多个 selector，通过 selector.id 区分）
function saveSelectedFolder(selectorId, folder) {
  if (!selectorId) return;
  try { localStorage.setItem(makeSelectedFolderKey(selectorId), folder); } catch (e) {}
}
function loadSelectedFolder(selectorId) {
  if (!selectorId) return null;
  try { return localStorage.getItem(makeSelectedFolderKey(selectorId)); } catch (e) {}
}

// playback rate 按 folder 存储（用户在每个 folder 中的偏好）
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

// 跨标签页同步（可选）
function handleStorageEvent(e) {
  if (!e.key) return;

  // volume
  if (e.key === STORAGE_KEYS.VOLUME) {
    const v = parseFloat(e.newValue);
    if (!Number.isNaN(v) && audioPlayer) audioPlayer.volume = v;
  }

  // playback rate for current folder
  const curFolder = folderSelector?.value || DEFAULT_FOLDER;
  if (e.key === makePlaybackRateKey(curFolder)) {
    const v = parseFloat(e.newValue);
    if (!Number.isNaN(v)) {
      playbackRate.value = v;
      if (audioPlayer) audioPlayer.playbackRate = v;
    }
  }

  // selected folder for selectors: 如果任意 selector key 变化，尝试更新对应 selector 元素
  if (e.key && e.key.startsWith(STORAGE_KEYS.SELECTED_FOLDER_PREFIX)) {
    const selectorId = e.key.substring(STORAGE_KEYS.SELECTED_FOLDER_PREFIX.length);
    const sel = document.getElementById(selectorId);
    if (sel && sel.tagName === 'SELECT') {
      sel.value = e.newValue;
      // 如果是当前页面上的 folderSelector 并且值改变，触发一次 setFolder（谨慎：避免循环）
      if (selectorId === folderSelector?.id && e.newValue !== folderSelector.value) {
        // 触发切换但不保存（因为这是同步来的变化）
        setFolder(e.newValue).catch(() => {});
      }
    }
  }
}


// ------------------- 初始化 -------------------
async function init() {
  // 获取 DOM 元素
  audioPlayer = document.getElementById('audio-player');
  currentSongEl = document.getElementById('current-song');
  playBtn = document.getElementById('play-btn');
  prevBtn = document.getElementById('prev-btn');
  playModeSelect = document.getElementById('play-mode');
  folderSelector = document.getElementById('folder-selector');

  audioPlayer.crossOrigin = 'anonymous';

  // 恢复并设置音量
  const savedVol = loadVolumeFromStorage();
  audioPlayer.volume = savedVol != null ? savedVol : 0.2;

  // 如果 folderSelector 元素存在，尝试恢复该 selector 的上次选择（按 selector.id）
  if (folderSelector && folderSelector.id) {
    const saved = loadSelectedFolder(folderSelector.id);
    if (saved) folderSelector.value = saved;
  } else if (folderSelector) {
    // 如果没有 id，给它一个 id（保证持久化 key 的稳定）
    folderSelector.id = `folder-selector-${Date.now()}`;
  }

  // 恢复播放速率 —— 按当前 folder 恢复；（如果没有则使用全局默认）
  const savedRate = loadPlaybackRateForFolder(folderSelector.id);
  if (savedRate != null) playbackRate.value = savedRate;
  audioPlayer.playbackRate = playbackRate.value;

  setupEventListeners();

  // listen storage for multi-tab sync
  window.addEventListener('storage', handleStorageEvent);

  await setFolder(folderSelector?.value || DEFAULT_FOLDER);

  playRandomSong();
}

function setupEventListeners() {
  if (folderSelector) folderSelector.addEventListener('change', handleFolderChange);
  if (playBtn) playBtn.addEventListener('click', handlePlayClick);
  if (prevBtn) prevBtn.addEventListener('click', playPreviousSong);

  audioPlayer.addEventListener('ended', handlePlaybackEnd);
  audioPlayer.addEventListener('error', handleAudioError);

  // audio 元素 volume/rate 的变动也要保存
  audioPlayer.addEventListener('volumechange', () => {
    saveVolumeToStorage(audioPlayer.volume);
  });
  audioPlayer.addEventListener('ratechange', () => {
    // 将 audio 的 rate 写回到 playbackRate，并持久化为当前 folder 的偏好
      playbackRate.value = audioPlayer.playbackRate;
      const folder = folderSelector?.value || DEFAULT_FOLDER;
      savePlaybackRateForFolder(folder);
  });

  // 当 new source 加载完后，确保 playbackRate 被设置回去（有些浏览器在 load 后会重置）
  audioPlayer.addEventListener('loadedmetadata', () => {
    try {
      audioPlayer.playbackRate = playbackRate.value;
    } catch (e) {}
  });
}

// ------------------- 歌单与文件夹 -------------------
async function handleFolderChange(e) {
  const selectedFolder = e?.target?.value || folderSelector.value;
  // 保存该 selector 的选中值（按 selector.id）
  if (folderSelector && folderSelector.id) saveSelectedFolder(folderSelector.id, selectedFolder);
  await setFolder(selectedFolder);
}

async function setFolder(folder) {
  try {
    showLoading(true);
    const res = await fetch(`${PUBLIC_API_BASE}/songs/set-folder`, {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({folder})
    });
    const result = await res.json();

    if (result.status === 'ok') {
      historyStack.value = [];
      await fetchSongList(folder);

      // 尝试恢复该 folder 对应的播放速率偏好
      const savedRate = loadPlaybackRateForFolder(folder);
      if (savedRate != null) {
        playbackRate.value = savedRate;
        if (audioPlayer) audioPlayer.playbackRate = savedRate;
      }

      // 不再恢复 currentIndex（按你要求不保存 currentIndex），从 0 开始播放或提示无歌
      if (playlist.value.length > 0) {
        playSongAtIndex(0);
      } else {
        currentSongEl.textContent = '无可播放歌曲';
        audioPlayer.src = '';
      }
    } else {
      alert('切换失败：' + result.error);
    }
  } catch (err) {
    console.error('切换音乐文件夹失败', err);
    alert('请求失败: ' + err.message);
  } finally {
    showLoading(false);
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

// ------------------- 播放控制 -------------------

function playSongAtIndex(index, fromHistory = false) {
  if (index < 0 || index >= playlist.value.length) {
    currentSongEl.textContent = '播放失败：索引越界';
    return;
  }

  if (!fromHistory && currentIndex.value !== -1 && currentIndex.value !== index) {
    historyStack.value.push(currentIndex.value);
  }

  currentIndex.value = index;
  const song = playlist.value[index];

  // 设置 src 之前/之后都确保 playbackRate 被恢复（避免某些浏览器把 rate 恢复为 1）
  audioPlayer.src = song.url;

  // 在设置 src 后强制将 playbackRate 写回
  try {
    audioPlayer.playbackRate = playbackRate.value;
  } catch (e) {}

  const parsed = parseSongNameWithBv(song.name);
  if (parsed.bv) {
    currentSongEl.innerHTML = `
  <span class="song-title">${parsed.title}</span>
  <a href="https://www.bilibili.com/video/${parsed.bv}/" target="_blank" class="portal-link">
    <img src="${portalIcon}" alt="传送门" class="portal-icon" />
  </a>
`
  } else {
    currentSongEl.textContent = parsed.title;
  }

  // 在 metadata 载入时进一步确保 playbackRate 没被重置
  const once = () => {
    try { audioPlayer.playbackRate = playbackRate.value; } catch (e) {}
    audioPlayer.removeEventListener('loadedmetadata', once);
  };
  audioPlayer.addEventListener('loadedmetadata', once);

  audioPlayer.play().catch(err => {
    console.warn('播放未启动：', err);
  });
}

function parseSongNameWithBv(name) {
  const clean = name.replace(/\.mp3$/, '');
  const match = clean.match(/^(.*?)_?(BV[0-9A-Za-z]+)/);
  return match ? {title: match[1], bv: match[2]} : {title: clean, bv: null};
}

function playPreviousSong() {
  if (historyStack.value.length === 0) {
    alert("没有上一首了！");
    return;
  }
  const prev = historyStack.value.pop();
  playSongAtIndex(prev, true);
}

function handlePlaybackEnd() {
  const diff = Math.abs(audioPlayer.currentTime - audioPlayer.duration);
  if (audioPlayer.duration > 0 && diff < 0.5) {
    const mode = playModeSelect.value;
    if (mode === 'single-loop') {
      audioPlayer.currentTime = 0;
      audioPlayer.play();
    } else if (mode === 'loop-list') {
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
    const mode = playModeSelect.value;
    if (mode === 'loop-list') {
      const next = (currentIndex.value + 1) % playlist.value.length;
      playSongAtIndex(next);
    } else {
      playRandomSong();
    }
  }, 1000);
}

function playRandomSong() {
  if (!playlist.value || playlist.value.length === 0) {
    currentSongEl.textContent = '播放失败：歌曲列表为空';
    return;
  }
  const rand = Math.floor(Math.random() * playlist.value.length);
  playSongAtIndex(rand);
}

function handlePlayClick() {
  const mode = playModeSelect.value;
  if (mode === 'loop-list' && playlist.value.length > 0) {
    const next = (currentIndex.value + 1) % playlist.value.length;
    playSongAtIndex(next);
  } else {
    playRandomSong();
  }
}

// 已暴露的 playSongAtIndex 方法
function handleSelectSong(songId) {
  const idx = playlist.value.findIndex(s => s.id === songId)
  if (idx !== -1) {
    playSongAtIndex(idx)
  }
}

// ------------------- watch 与其他 -------------------

// 当 playbackRate 改变时，同步到 audioPlayer 并持久化为当前 folder 的偏好
watch(playbackRate, (val) => {
  if (audioPlayer) audioPlayer.playbackRate = val;
  const folder = folderSelector?.value || DEFAULT_FOLDER;
  savePlaybackRateForFolder(folder);
});
// ------------------- 工具 -------------------
function showLoading(show) {
  document.body.classList.toggle('loading', show);
}

onMounted(() => {
  init();
});

// 对外暴露响应式对象和方法
defineExpose({
  playlist,
  currentIndex,
  playSongAtIndex,
  handleSelectSong
})
</script>

<style scoped>
.player-container {
  flex: 2 1 auto;
  background-color: #fff9d6; /* 浅米黄，和页面背景协调 */
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid #e0e4e8;
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
  border: 1px solid #ccc;
  font-size: 14px;
  background-color: #fef9e0; /* 按钮浅黄，与容器协调 */
  color: #333;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.controls button:hover {
  background-color: #fff176; /* hover柔和黄色 */
}

button {
  padding: 10px 18px;
  background-color: #fdd835; /* 主操作按钮温暖黄色 */
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
  transition: background-color 0.2s ease;
}

button:hover {
  background-color: #fbc02d;
}

.song-info {
  margin: 24px 0;
  padding: 16px;
  background-color: #fffce0; /* 信息卡片浅黄 */
  border-radius: 6px;
  border: 1px solid #e5e8eb;
}

audio {
  width: 100%;
  margin-top: 12px;
}

.song-info-container {
  flex: 2 1 auto;
}

</style>