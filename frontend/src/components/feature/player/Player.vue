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
          <option value="ha_ji_mi">哈基咪咪</option>
          <option value="dian_gun">溜冰密室</option>
          <option value="da_si_ma">起飞基地</option>
          <option value="ding_zhen">烟雾缭绕</option>
          <option value="dxl">东洋雪莲</option>
        </select>
      </div>
    </div>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue'
import portalIcon from '@/assets/icons/protal.svg'
import VoteControls from "./VoteControls.vue";


const API_BASE = '/api';
const DEFAULT_FOLDER = 'ha_ji_mi';

// 响应式状态
const playlist = ref([]);           // 歌曲列表
const currentIndex = ref(-1);       // 当前播放索引
const historyStack = ref([]);       // 历史播放栈

// DOM元素
let audioPlayer, currentSongEl, playBtn, prevBtn, playModeSelect, folderSelector;
let likeBtn, dislikeBtn, likeCountEl, dislikeCountEl;

// ------------------- 初始化 -------------------
async function init() {
  // 获取 DOM 元素
  audioPlayer = document.getElementById('audio-player');
  currentSongEl = document.getElementById('current-song');
  playBtn = document.getElementById('play-btn');
  prevBtn = document.getElementById('prev-btn');
  playModeSelect = document.getElementById('play-mode');
  folderSelector = document.getElementById('folder-selector');
  likeBtn = document.getElementById('like-btn');
  dislikeBtn = document.getElementById('dislike-btn');
  likeCountEl = document.getElementById('like-count');
  dislikeCountEl = document.getElementById('dislike-count');

  audioPlayer.crossOrigin = 'anonymous';
  audioPlayer.volume = 0.2;
  folderSelector.value = DEFAULT_FOLDER;

  setupEventListeners();

  await setFolder(DEFAULT_FOLDER);
}

function setupEventListeners() {
  folderSelector.addEventListener('change', handleFolderChange);
  playBtn.addEventListener('click', handlePlayClick);
  prevBtn.addEventListener('click', playPreviousSong);
  audioPlayer.addEventListener('ended', handlePlaybackEnd);
  audioPlayer.addEventListener('error', handleAudioError);
}

// ------------------- 歌单与文件夹 -------------------
async function handleFolderChange() {
  const selectedFolder = folderSelector.value;
  await setFolder(selectedFolder);
}

async function setFolder(folder) {
  try {
    showLoading(true);
    const res = await fetch(`${API_BASE}/songs/set-folder`, {
      method: 'POST',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({folder})
    });
    const result = await res.json();

    if (result.status === 'ok') {
      historyStack.value = [];
      await fetchSongList();
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
    const res = await fetch(`${API_BASE}/songs/get`);
    const data = await res.json();
    playlist.value = shuffleArray(data);
    return playlist.value;
  } catch (err) {
    console.error("获取歌曲列表失败", err);
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

  audioPlayer.src = song.url;
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

  audioPlayer.play();
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

// ------------------- 工具 -------------------
function showLoading(show) {
  document.body.classList.toggle('loading', show);
}

onMounted(() => {
  init();
});

// 对外暴露响应式对象和方法
defineExpose({
  playlist,          // 暴露响应式 playlist
  currentIndex,      // 暴露响应式 currentIndex
  playSongAtIndex,    // 暴露播放方法
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