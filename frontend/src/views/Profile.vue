<template>
  <div class="profile-page">
    <h1>我的</h1>

    <p v-if="loading" class="hint">正在加载…</p>
    <p v-else-if="!profile" class="hint">请先登录后再查看个人资料。</p>

    <template v-else>
      <section class="card">
        <h2>基本信息</h2>
        <dl class="info-list">
          <div class="info-row">
            <dt>用户名</dt>
            <dd>{{ profile.username }}</dd>
          </div>
          <div class="info-row">
            <dt>身份</dt>
            <dd>{{ roleLabel }}</dd>
          </div>
          <div class="info-row">
            <dt>注册时间</dt>
            <dd>{{ formattedCreatedAt }}</dd>
          </div>
        </dl>
      </section>

      <section class="card">
        <h2>修改用户名</h2>
        <form class="profile-form" @submit.prevent="submitUsername">
          <label class="field">
            <span class="label-text">新用户名</span>
            <input v-model="usernameForm.newUsername" type="text" maxlength="64" required
                   autocomplete="username"/>
          </label>

          <p v-if="usernameError" class="error">{{ usernameError }}</p>

          <button class="primary-btn" type="submit" :disabled="usernameLoading">
            {{ usernameLoading ? '提交中…' : '保存用户名' }}
          </button>
        </form>
      </section>

      <section class="card">
        <h2>修改密码</h2>
        <form class="profile-form" @submit.prevent="submitPassword">
          <label class="field">
            <span class="label-text">当前密码</span>
            <input v-model="passwordForm.oldPassword" type="password" required autocomplete="current-password"/>
          </label>

          <label class="field">
            <span class="label-text">新密码</span>
            <input v-model="passwordForm.newPassword" type="password" required autocomplete="new-password"/>
          </label>

          <label class="field">
            <span class="label-text">确认新密码</span>
            <input v-model="passwordForm.confirmPassword" type="password" required autocomplete="new-password"/>
          </label>

          <p v-if="passwordError" class="error">{{ passwordError }}</p>

          <button class="primary-btn" type="submit" :disabled="passwordLoading">
            {{ passwordLoading ? '提交中…' : '保存密码' }}
          </button>
          <p class="muted">修改成功后，其他设备上的登录状态会失效。</p>
        </form>
      </section>
    </template>
  </div>
</template>

<script setup>
import {computed, onMounted, reactive, ref, watch} from 'vue'
import {changePassword, changeUsername, getProfile} from '@/services/auth.js'
import {useAuthStore} from '@/store/index.js'
import {eventBus} from '@/utils/eventBus.js'

const ROLE_LABELS = {1: '普通用户', 2: '管理员', 3: '站长'}

const authStore = useAuthStore()
const profile = ref(null)
const loading = ref(true)

const usernameForm = reactive({newUsername: ''})
const usernameError = ref('')
const usernameLoading = ref(false)

const passwordForm = reactive({oldPassword: '', newPassword: '', confirmPassword: ''})
const passwordError = ref('')
const passwordLoading = ref(false)

const roleLabel = computed(() => ROLE_LABELS[profile.value?.role] || '未知')

const formattedCreatedAt = computed(() => {
  if (!profile.value?.createdAt) {
    return '未知'
  }
  const date = new Date(profile.value.createdAt)
  return Number.isNaN(date.getTime()) ? '未知' : date.toLocaleString('zh-CN')
})

onMounted(loadProfile)

// 直接输 URL 进到这个页面时可能还没登录，或页面打开期间登录状态发生了变化
watch(() => authStore.isAuthenticated, (isAuthenticated) => {
  if (isAuthenticated) {
    loadProfile()
  } else {
    profile.value = null
    loading.value = false
  }
})

async function loadProfile() {
  loading.value = true
  try {
    profile.value = await getProfile()
    usernameForm.newUsername = profile.value.username
  } catch (error) {
    console.error('Failed to load profile', error)
    profile.value = null
  } finally {
    loading.value = false
  }
}

async function submitUsername() {
  usernameError.value = ''
  const newUsername = usernameForm.newUsername.trim()
  if (!newUsername) {
    usernameError.value = '用户名不能为空'
    return
  }
  if (newUsername === profile.value.username) {
    usernameError.value = '新用户名与当前用户名相同'
    return
  }

  usernameLoading.value = true
  try {
    const response = await changeUsername({newUsername})
    // 后端已重建当前会话的身份，这里同步前端 store，避免各处仍显示旧用户名
    authStore.applyUserStatus(response)
    profile.value = {...profile.value, username: response.username}
    usernameForm.newUsername = response.username
    eventBus.emit('show-toast', '用户名已修改')
  } catch (error) {
    console.error('Change username error:', error)
    usernameError.value = error?.response?.data?.message || '修改失败，请重试'
  } finally {
    usernameLoading.value = false
  }
}

async function submitPassword() {
  passwordError.value = ''
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordError.value = '两次输入的新密码不一致'
    return
  }

  passwordLoading.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    eventBus.emit('show-toast', '密码已修改')
  } catch (error) {
    console.error('Change password error:', error)
    passwordError.value = error?.response?.data?.message || '修改失败，请重试'
  } finally {
    passwordLoading.value = false
  }
}
</script>

<style scoped>
.profile-page {
  text-align: left;
  padding: 20px;
  margin-left: 120px;
  box-sizing: border-box;
  max-width: 720px;
}

.card {
  margin-top: 24px;
  padding: 24px;
  border-radius: 16px;
  border: 1px solid var(--border-color);
  background: var(--modal-bg);
  color: var(--modal-text);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.card h2 {
  margin: 0 0 20px 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--title-color);
}

.info-list {
  margin: 0;
}

.info-row {
  display: flex;
  gap: 16px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--border-color);
}

.info-row:last-child {
  border-bottom: none;
}

.info-row dt {
  width: 90px;
  flex-shrink: 0;
  color: var(--text-color);
  opacity: 0.7;
}

.info-row dd {
  margin: 0;
  color: var(--text-color);
  word-break: break-all;
}

.profile-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 360px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.label-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-color);
  text-align: left;
}

input[type="text"],
input[type="password"] {
  width: 100%;
  padding: 12px 16px;
  border-radius: 10px;
  border: 1px solid var(--input-border);
  background: var(--input-bg);
  color: var(--input-text);
  outline: none;
  font-size: 15px;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

input:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(249, 168, 37, 0.15);
}

.primary-btn {
  padding: 12px;
  background: var(--primary-color);
  color: #fff;
  border-radius: 10px;
  border: none;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.1s, filter 0.2s;
}

/* 浅色主题下 primary color 偏黄，深色文字更清晰 */
:root:not([data-theme="dark"]) .primary-btn {
  color: #333;
}

.primary-btn:hover {
  filter: brightness(1.1);
}

.primary-btn:active {
  transform: scale(0.98);
}

.primary-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.hint,
.muted {
  font-size: 14px;
  color: var(--text-color);
  opacity: 0.7;
}

.error {
  color: var(--danger-color);
  font-size: 14px;
  margin: 0;
  background: rgba(255, 82, 82, 0.1);
  padding: 8px;
  border-radius: 8px;
}
</style>
