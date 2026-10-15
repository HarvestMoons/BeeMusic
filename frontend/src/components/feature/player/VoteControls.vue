<template>
  <div class="vote-controls">
    <button :class="{ active: userVote === 1 }" :disabled="isVoting" @click="handleLike">
      <img :src="likeIcon" alt="点赞" class="vote-icon svg-icon"/>
      <span>{{ likes }}</span>
    </button>
    <button :class="{ active: userVote === -1 }" :disabled="isVoting" @click="handleDislike">
      <img :src="dislikeIcon" alt="点踩" class="vote-icon svg-icon"/>
      <span>{{ dislikes }}</span>
    </button>
  </div>
</template>

<script setup>
import {onBeforeUnmount, ref, watch} from 'vue'
import {useAuthStore} from '@/store';
import {eventBus} from '@/utils/eventBus.js';
// 引入图标
import likeIcon from '@/assets/icons/like.svg'
import dislikeIcon from '@/assets/icons/dislike.svg'
import api from '@/services/http';

// 使用 props 接收当前歌曲 ID
const props = defineProps({
  songId: {
    type: [String, Number],
    required: true
  }
})

const likes = ref(0)
const dislikes = ref(0)
const userVote = ref(0) // 1=已点赞, -1=已点踩, 0=无
const isVoting = ref(false)

const authStore = useAuthStore()
let votesRequestId = 0
let disposed = false
onBeforeUnmount(() => { disposed = true; votesRequestId++ })

function showToastMessage(msg) {
  eventBus.emit('show-toast', msg)
}

// 刷新投票数
async function refreshVotes() {
  const songId = props.songId
  const requestId = ++votesRequestId
  likes.value = 0
  dislikes.value = 0
  userVote.value = 0
  try {
    const res = await api.get(`/public/songs/votes/${songId}`)
    if (disposed || requestId !== votesRequestId || songId !== props.songId) return
    const data = res.data
    likes.value = data.likes
    dislikes.value = data.dislikes
    if (typeof data.userVote === 'number') {
      userVote.value = data.userVote
    } else {
      userVote.value = 0
    }
  } catch (err) {
    console.error('获取投票数失败', err)
  }
}

/**
 * 提交投票。targetVote 是目标状态（1=点赞，-1=点踩）：
 * 当前已经是该状态时，调同一个接口的 DELETE 表示取消。
 */
async function submitVote(targetVote, loginTip, failureTip) {
  if (isVoting.value) return
  if (!authStore.isAuthenticated) {
    showToastMessage(loginTip)
    return
  }
  isVoting.value = true
  const songId = props.songId
  const requestId = ++votesRequestId
  try {
    const isCancelling = userVote.value === targetVote
    const res = isCancelling
        ? await api.delete(`/songs/vote/${songId}`)
        : await api.post(`/songs/${targetVote === 1 ? 'like' : 'dislike'}/${songId}`)
    applyVoteResult(res.data, isCancelling ? 0 : targetVote, songId, requestId)
  } catch (err) {
    console.error(failureTip, err)
    showToastMessage(err.response?.data?.message || failureTip)
  } finally {
    isVoting.value = false
  }
}

/** 统一处理投票结果：更新本地票数，并通知播放器同步内存中的歌单 */
function applyVoteResult(data, voted, songId, requestId) {
  if (disposed) return
  if (songId === props.songId && requestId === votesRequestId) {
    likes.value = data.likes
    dislikes.value = data.dislikes
    userVote.value = data.userVote ?? voted
  }
  eventBus.emit('song-vote-updated', {
    songId,
    likes: data.likes,
    dislikes: data.dislikes
  })
}

function handleLike() {
  return submitVote(1, '请先登录再点赞哦', '点赞失败')
}

function handleDislike() {
  return submitVote(-1, '请先登录再点踩哦', '点踩失败')
}

// 监听 songId 变化（immediate: true 会在 setup 阶段立刻拉取一次，
// 因此不需要再在 onMounted 里重复请求同一个接口）
watch(() => [props.songId, authStore.isAuthenticated, authStore.username], refreshVotes, {immediate: true})
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
  background-color: var(--vote-btn-bg);
  border: 1px solid var(--vote-btn-border);
  border-radius: 6px;
  padding: 6px 12px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s ease;
  color: var(--text-color);
}

.vote-controls button:hover {
  background-color: var(--vote-btn-hover-bg);
}

.vote-controls button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.vote-controls button.active {
  background-color: var(--vote-btn-active-bg);
  border-color: var(--vote-btn-active-border);
  box-shadow: 0 0 0 2px rgba(255, 202, 58, 0.3);
}

.vote-icon {
  width: 18px;
  height: 18px;
}
</style>
