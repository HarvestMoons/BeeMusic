<template>
  <div class="comment-sidebar" :class="{ collapsed: !visible }">
    <div class="drawer-header">
      <h3>评论 ({{ totalComments }})</h3>
      <button class="close-btn" @click="$emit('close')">
        <span v-if="visible">»</span>
        <span v-else>«</span>
      </button>
    </div>

    <div v-show="visible" class="comment-content-wrapper">
      <div class="comment-list" ref="listRef">
        <div v-if="loading" class="loading-state">加载中...</div>
        <div v-else-if="comments.length === 0" class="empty-state">暂无评论，快来抢沙发吧~</div>
        
        <div v-else class="comment-items">
          <div v-for="comment in comments" :key="comment.id" class="comment-item">
            <!-- 主评论 -->
            <div class="comment-main">
              <div class="user-avatar">{{ comment.username.charAt(0).toUpperCase() }}</div>
              <div class="comment-content-box">
                <div class="comment-header">
                  <span class="username">{{ comment.username }}</span>
                  <span class="time">{{ formatDate(comment.createdAt) }}</span>
                </div>
                <div class="comment-text">{{ comment.content }}</div>
                <div class="comment-actions">
                  <button 
                    class="action-btn like-btn" 
                    :class="{ active: comment.liked }"
                    @click="toggleLike(comment)"
                  >
                    <span class="icon">👍</span> {{ comment.likeCount || 0 }}
                  </button>
                  <button class="action-btn" @click="startReply(comment)">回复</button>
                  <button v-if="comment.owner" class="action-btn delete-btn" @click="handleDelete(comment.id)">删除</button>
                </div>
              </div>
            </div>

            <!-- 子评论列表 -->
            <div v-if="comment.replies && comment.replies.length > 0" class="sub-comments">
              <div v-for="reply in comment.replies" :key="reply.id" class="sub-comment-item">
                <div class="user-avatar small">{{ reply.username.charAt(0).toUpperCase() }}</div>
                <div class="comment-content-box">
                  <div class="comment-header">
                    <span class="username">{{ reply.username }}</span>
                    <span v-if="reply.replyToUserId && reply.replyToUserId !== comment.userId" class="reply-target">
                      回复 <span class="target-name">@{{ reply.replyToUsername }}</span>
                    </span>
                    <span class="time">{{ formatDate(reply.createdAt) }}</span>
                  </div>
                  <div class="comment-text">{{ reply.content }}</div>
                  <div class="comment-actions">
                    <button 
                      class="action-btn like-btn" 
                      :class="{ active: reply.liked }"
                      @click="toggleLike(reply)"
                    >
                      <span class="icon">👍</span> {{ reply.likeCount || 0 }}
                    </button>
                    <button class="action-btn" @click="startReply(comment, reply)">回复</button>
                    <button v-if="reply.owner" class="action-btn delete-btn" @click="handleDelete(reply.id)">删除</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部输入框 -->
      <div class="drawer-footer">
        <div v-if="replyingTo" class="reply-indicator">
          回复 @{{ replyingTo.username }}:
          <button class="cancel-reply" @click="cancelReply">×</button>
        </div>
        <div class="input-area">
          <textarea 
            v-model="inputContent" 
            :placeholder="authStore.isAuthenticated ? '发一条友善的评论...' : '请先登录后评论'"
            :disabled="!authStore.isAuthenticated"
            @keydown.ctrl.enter="submitComment"
            rows="1"
          ></textarea>
          <button 
            class="send-btn" 
            :disabled="!authStore.isAuthenticated || !inputContent.trim()"
            @click="submitComment"
          >
            发送
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, nextTick } from 'vue'
import { useAuthStore } from '@/store'
import api from '@/services/auth'

const props = defineProps({
  visible: Boolean,
  songId: [String, Number]
})

const emit = defineEmits(['close'])

const authStore = useAuthStore()
const comments = ref([])
const loading = ref(false)
const inputContent = ref('')
const replyingTo = ref(null) // { commentId, userId, username, rootId }

const totalComments = computed(() => {
  let count = comments.value.length
  comments.value.forEach(c => {
    if (c.replies) count += c.replies.length
  })
  return count
})

watch(() => props.visible, (val) => {
  if (val && props.songId) {
    fetchComments()
  }
})

watch(() => props.songId, (val) => {
  if (props.visible && val) {
    fetchComments()
  }
})

async function fetchComments() {
  if (!props.songId) return
  loading.value = true
  try {
    const res = await api.get(`/public/comments/${props.songId}`)
    comments.value = res.data
  } catch (err) {
    console.error('加载评论失败', err)
  } finally {
    loading.value = false
  }
}

function formatDate(isoStr) {
  const d = new Date(isoStr)
  return d.toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

// 开始回复
// rootComment: 根评论对象
// replyComment: 被回复的子评论对象 (可选)
function startReply(rootComment, replyComment = null) {
  if (!authStore.isAuthenticated) return
  
  const target = replyComment || rootComment
  replyingTo.value = {
    rootId: rootComment.id,
    userId: target.userId,
    username: target.username
  }
  // 聚焦输入框 (简单处理)
  const input = document.querySelector('.input-area textarea')
  if (input) input.focus()
}

function cancelReply() {
  replyingTo.value = null
}

async function submitComment() {
  if (!inputContent.value.trim()) return
  
  const payload = {
    songId: props.songId,
    content: inputContent.value,
    parentId: replyingTo.value ? replyingTo.value.rootId : null,
    replyToUserId: replyingTo.value ? replyingTo.value.userId : null
  }

  try {
    const res = await api.post('/comments/add', payload)
    const newComment = res.data
    
    if (payload.parentId) {
      // 插入到对应根评论的 replies 中
      const root = comments.value.find(c => c.id === payload.parentId)
      if (root) {
        if (!root.replies) root.replies = []
        root.replies.push(newComment)
      }
    } else {
      // 插入到列表顶部
      comments.value.unshift(newComment)
    }
    
    inputContent.value = ''
    cancelReply()
  } catch (err) {
    console.error('发表评论失败', err)
    alert('发表失败: ' + (err.response?.data?.message || err.message))
  }
}

async function toggleLike(comment) {
  if (!authStore.isAuthenticated) return
  
  try {
    if (comment.liked) {
      await api.delete(`/comments/like/${comment.id}`)
      comment.liked = false
      comment.likeCount--
    } else {
      await api.post(`/comments/like/${comment.id}`)
      comment.liked = true
      comment.likeCount++
    }
  } catch (err) {
    console.error('操作失败', err)
  }
}

async function handleDelete(commentId) {
  if (!confirm('确定删除这条评论吗？')) return
  try {
    await api.delete(`/comments/${commentId}`)
    // 从列表中移除
    // 可能是根评论，也可能是子评论
    const rootIdx = comments.value.findIndex(c => c.id === commentId)
    if (rootIdx !== -1) {
      comments.value.splice(rootIdx, 1)
    } else {
      // 找子评论
      for (const root of comments.value) {
        if (root.replies) {
          const subIdx = root.replies.findIndex(r => r.id === commentId)
          if (subIdx !== -1) {
            root.replies.splice(subIdx, 1)
            break
          }
        }
      }
    }
  } catch (err) {
    console.error('删除失败', err)
  }
}
</script>

<style scoped>
.comment-drawer-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.5);
  z-index: 2000;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.3s ease;
  display: flex;
  justify-content: flex-end;
}

.comment-drawer-overlay.open {
  opacity: 1;
  pointer-events: auto;
}

.comment-drawer {
  width: 400px;
  max-width: 90vw;
  height: 100%;
  background: var(--playlist-bg);
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  transform: translateX(100%);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.comment-drawer-overlay.open .comment-drawer {
  transform: translateX(0);
}

.drawer-header {
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.drawer-header h3 {
  margin: 0;
  color: var(--text-color);
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: var(--text-color);
  cursor: pointer;
  padding: 0 8px;
}

.comment-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.loading-state, .empty-state {
  text-align: center;
  color: var(--text-color);
  opacity: 0.6;
  margin-top: 40px;
}

.comment-item {
  margin-bottom: 20px;
}

.comment-main {
  display: flex;
  gap: 12px;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--primary-color);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  flex-shrink: 0;
}

.user-avatar.small {
  width: 24px;
  height: 24px;
  font-size: 12px;
}

.comment-content-box {
  flex: 1;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.username {
  font-weight: 600;
  color: var(--text-color);
  font-size: 14px;
}

.time {
  font-size: 12px;
  color: var(--text-color);
  opacity: 0.5;
}

.comment-text {
  color: var(--text-color);
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 6px;
  word-break: break-word;
  white-space: pre-wrap;
}

.comment-actions {
  display: flex;
  gap: 12px;
}

.action-btn {
  background: none;
  border: none;
  padding: 0;
  font-size: 12px;
  color: var(--text-color);
  opacity: 0.6;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}

.action-btn:hover {
  opacity: 1;
  color: var(--primary-color);
}

.like-btn.active {
  color: var(--primary-color);
  opacity: 1;
}

.sub-comments {
  margin-left: 48px;
  margin-top: 12px;
  background: var(--playlist-item-bg);
  border-radius: 8px;
  padding: 12px;
}

.sub-comment-item {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.sub-comment-item:last-child {
  margin-bottom: 0;
}

.reply-target {
  font-size: 12px;
  color: var(--text-color);
  opacity: 0.7;
}

.target-name {
  color: var(--primary-color);
}

.drawer-footer {
  padding: 12px;
  border-top: 1px solid var(--border-color);
  background: var(--playlist-bg);
}

.reply-indicator {
  font-size: 12px;
  color: var(--text-color);
  margin-bottom: 8px;
  display: flex;
  justify-content: space-between;
  background: var(--playlist-item-bg);
  padding: 4px 8px;
  border-radius: 4px;
}

.cancel-reply {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-color);
}

.input-area {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.input-area textarea {
  flex: 1;
  padding: 8px 12px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: var(--playlist-item-bg);
  color: var(--text-color);
  outline: none;
  resize: none;
  min-height: 36px;
  max-height: 100px;
  font-family: inherit;
  line-height: 1.4;
}

.input-area textarea:focus {
  border-color: var(--primary-color);
}

.send-btn {
  height: 36px;
  padding: 0 16px;
  border-radius: 18px;
  background: var(--primary-color);
  color: #fff;
  border: none;
  cursor: pointer;
  font-size: 14px;
  flex-shrink: 0;
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
