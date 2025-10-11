<template>
  <div class="vote-controls">
    <button @click="handleLike">
      <img :src="likeIcon" alt="点赞" class="vote-icon" />
      <span>{{ likes }}</span>
    </button>
    <button @click="handleDislike">
      <img :src="dislikeIcon" alt="点踩" class="vote-icon" />
      <span>{{ dislikes }}</span>
    </button>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { PUBLIC_API_BASE,API_BASE } from '@/constants';

// 使用 props 接收当前歌曲 ID
const props = defineProps({
  songId: {
    type: [String, Number],
    required: true
  }
})

const likes = ref(0)
const dislikes = ref(0)

// 引入图标
import likeIcon from '@/assets/icons/likes.svg'
import dislikeIcon from '@/assets/icons/dislike.svg'
import api from '@/services/auth';

// 刷新投票数
async function refreshVotes() {
  try {
    const res = await fetch(`${PUBLIC_API_BASE}/songs/votes/${props.songId}`)
    const data = await res.json()
    likes.value = data.likes
    dislikes.value = data.dislikes
  } catch (err) {
    console.error('获取投票数失败', err)
  }
}

async function handleLike() {
  try {
    const res = await api.post(`/songs/like/${props.songId}`);
    likes.value = res.data.likes;
    dislikes.value = res.data.dislikes;
  } catch (err) {
    console.error('点赞失败', err.response?.data || err.message);
  }
}

async function handleDislike() {
  try {
    const res = await api.post(`/songs/dislike/${props.songId}`);
    likes.value = res.data.likes;
    dislikes.value = res.data.dislikes;
  } catch (err) {
    console.error('点踩失败', err.response?.data || err.message);
  }
}

// 监听 songId 变化
watch(() => props.songId, refreshVotes, {immediate: true})

onMounted(refreshVotes)
</script>

<style scoped>
.vote-controls {
  display: flex;
  gap: 12px;
}

.vote-controls button {
  display: flex;
  align-items: center;
  gap: 6px;
  background-color: #fff8c4;
  border: 1px solid #ccc;
  border-radius: 6px;
  padding: 6px 12px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s ease;
}

.vote-controls button:hover {
  background-color: #fff176;
}

.vote-icon {
  width: 18px;
  height: 18px;
}
</style>
