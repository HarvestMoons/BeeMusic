<template>
  <div class="settings-page">
    <h1>设置</h1>

    <p v-if="loading" class="hint">正在加载…</p>
    <template v-else-if="authStore.isAuthenticated && profile">
      <section class="card">
        <h2>基本信息</h2>
        <dl class="info-list">
          <div class="info-row">
            <dt>用户名</dt>
            <dd>
              {{ profile.username }}
              <span v-if="profile.role === 3" class="role-badge station-master">站长</span>
              <span v-else-if="profile.role === 2" class="role-badge admin">管理员</span>
            </dd>
          </div>
          <div class="info-row">
            <dt>注册时间</dt>
            <dd>{{ formattedCreatedAt }}</dd>
          </div>
        </dl>
      </section>
    </template>
    <p v-else-if="authStore.isAuthenticated" class="hint">资料加载失败，请刷新重试。</p>
    <p v-else class="hint">登录后可查看个人资料、修改密码与用户名。</p>

    <div class="card-grid">
      <section class="card">
        <button type="button" class="toggle-header" :aria-expanded="showDisplaySection"
                @click="showDisplaySection = !showDisplaySection">
          <span class="card-title">显示设置</span>
          <span class="chevron" :class="{ rotated: showDisplaySection }">›</span>
        </button>

        <div v-show="showDisplaySection" class="setting-list">
          <div class="setting-row">
            <span>夜间模式</span>
            <ToggleSwitch :model-value="themeStore.isDarkMode" @change="handleThemeToggle"/>
          </div>
          <div class="setting-row">
            <span>背景粒子</span>
            <ToggleSwitch :model-value="themeStore.showParticles" @change="handleParticlesToggle"/>
          </div>
        </div>
      </section>

      <section v-if="authStore.isAuthenticated" class="card">
        <button type="button" class="toggle-header" :aria-expanded="showUsernameSection"
                @click="toggleUsernameSection">
          <span class="card-title">修改用户名</span>
          <span class="chevron" :class="{ rotated: showUsernameSection }">›</span>
        </button>

        <form v-show="showUsernameSection" class="section-form" @submit.prevent="submitUsername">
          <label class="field">
            <span class="label-text">新用户名</span>
            <input v-model="usernameForm.newUsername" type="text" maxlength="64" required
                   autocomplete="username"/>
          </label>

          <p v-if="usernameError" class="error">{{ usernameError }}</p>

          <button class="action-btn" type="submit" :disabled="usernameLoading">
            {{ usernameLoading ? '提交中…' : '保存用户名' }}
          </button>
        </form>
      </section>

      <section v-if="authStore.isAuthenticated" class="card">
        <button type="button" class="toggle-header" :aria-expanded="showPasswordSection"
                @click="togglePasswordSection">
          <span class="card-title">修改密码</span>
          <span class="chevron" :class="{ rotated: showPasswordSection }">›</span>
        </button>

        <form v-show="showPasswordSection" class="section-form" @submit.prevent="submitPassword">
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

          <button class="action-btn" type="submit" :disabled="passwordLoading">
            {{ passwordLoading ? '提交中…' : '保存密码' }}
          </button>
          <p class="muted">修改成功后，其他设备上的登录状态会失效。</p>
        </form>
      </section>

      <section v-if="authStore.isStationMaster" class="card">
        <button type="button" class="toggle-header" :aria-expanded="showSiteSection"
                @click="showSiteSection = !showSiteSection">
          <span class="card-title">站长设置</span>
          <span class="chevron" :class="{ rotated: showSiteSection }">›</span>
        </button>

        <div v-show="showSiteSection" class="setting-list">
          <div class="setting-row">
            <span>全站评论</span>
            <ToggleSwitch :model-value="siteConfigStore.commentsEnabled" @change="handleCommentsToggle"/>
          </div>
          <button class="action-btn" type="button" :disabled="syncingDatabase" @click="handleSyncDatabase">
            {{ syncingDatabase ? '同步中…' : '同步数据库' }}
          </button>
          <p class="muted">会同步 OSS 新增歌曲，并永久删除 OSS 中已不存在的歌曲。</p>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import {computed, onMounted, reactive, ref, watch} from 'vue'
import {changePassword, changeUsername, getProfile} from '@/services/auth.js'
import {syncDatabase} from '@/services/siteConfig.js'
import {useAuthStore, useSiteConfigStore, useThemeStore} from '@/store/index.js'
import {eventBus, requestConfirm} from '@/utils/eventBus.js'
import ToggleSwitch from '@/components/common/ToggleSwitch.vue'

const authStore = useAuthStore()
const themeStore = useThemeStore()
const siteConfigStore = useSiteConfigStore()

const profile = ref(null)
const loading = ref(true)

// 各面板默认收起，点标题才展开
const showDisplaySection = ref(false)
const showUsernameSection = ref(false)
const showPasswordSection = ref(false)
const showSiteSection = ref(false)

const usernameForm = reactive({newUsername: ''})
const usernameError = ref('')
const usernameLoading = ref(false)

const passwordForm = reactive({oldPassword: '', newPassword: '', confirmPassword: ''})
const passwordError = ref('')
const passwordLoading = ref(false)

const syncingDatabase = ref(false)

const formattedCreatedAt = computed(() => {
  if (!profile.value?.createdAt) {
    return '未知'
  }
  const date = new Date(profile.value.createdAt)
  return Number.isNaN(date.getTime()) ? '未知' : date.toLocaleString('zh-CN')
})

function reloadProfile() {
  if (authStore.isAuthenticated) {
    loadProfile()
  } else {
    profile.value = null
    loading.value = false
  }
}

onMounted(reloadProfile)

// 直接输 URL 进到这个页面时可能还没登录，或页面打开期间登录状态发生了变化
watch(() => authStore.isAuthenticated, reloadProfile)

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

function handleThemeToggle(val) {
  if (val !== themeStore.isDarkMode) {
    themeStore.toggleTheme()
  }
}

function handleParticlesToggle(val) {
  themeStore.setParticles(val)
}

async function handleCommentsToggle(val) {
  try {
    await siteConfigStore.updateCommentsEnabled(val)
  } catch (error) {
    console.error('Failed to update comments setting', error)
    eventBus.emit('show-toast', '设置失败，请重试')
  }
}

async function handleSyncDatabase() {
  if (syncingDatabase.value) return
  const confirmed = await requestConfirm({
    title: '同步数据库',
    message: '将立即从 OSS 同步数据库：新增 OSS 中的新歌，并永久删除 OSS 中已不存在的歌曲及其投票、评论和评论点赞。删除不可恢复，确定继续吗？',
    confirmText: '立即同步'
  })
  if (!confirmed) return
  syncingDatabase.value = true
  try {
    const res = await syncDatabase()
    const added = res?.added ?? 0
    const removed = res?.removed ?? 0
    eventBus.emit('show-toast', `同步完成：新增 ${added} 首，清理 ${removed} 首`)
    // 通知播放器刷新内存歌单：新增/被清理的歌曲立即生效
    eventBus.emit('songs-updated')
  } catch (error) {
    console.error('Sync failed', error)
    eventBus.emit('show-toast', '同步失败，请重试。')
  } finally {
    syncingDatabase.value = false
  }
}

function toggleUsernameSection() {
  showUsernameSection.value = !showUsernameSection.value
  if (showUsernameSection.value) {
    usernameError.value = ''
  }
}

function togglePasswordSection() {
  showPasswordSection.value = !showPasswordSection.value
  if (showPasswordSection.value) {
    passwordError.value = ''
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
    showUsernameSection.value = false
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
    showPasswordSection.value = false
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
.settings-page {
  text-align: left;
  padding: 20px;
  margin-left: 120px;
  box-sizing: border-box;
}

.card {
  max-width: 880px;
  margin: 24px auto;
  padding: 24px;
  box-sizing: border-box;
  background: var(--playlist-bg);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  box-shadow: 0 20px 45px rgba(20, 24, 38, 0.08);
  transition: background-color 0.3s ease, border-color 0.3s ease;
}

.card h2 {
  margin: 0 0 16px 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-color);
}

.info-list {
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-row {
  display: flex;
  align-items: baseline;
  gap: 16px;
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--playlist-item-bg);
}

.info-row dt {
  width: 76px;
  flex-shrink: 0;
  font-size: 14px;
  opacity: 0.75;
}

.info-row dd {
  margin: 0;
  font-weight: 600;
  word-break: break-all;
}

/* 与评论区 .role-badge 保持同一套写法 */
.role-badge {
  font-size: 10px;
  padding: 1px 4px;
  border-radius: 4px;
  margin-left: 4px;
  font-weight: normal;
  line-height: 1.2;
  display: inline-block;
  vertical-align: middle;
  transform: translateY(-1px);
}

.role-badge.station-master {
  background-color: rgba(255, 77, 79, 0.1);
  color: #ff4d4f;
  border: 1px solid rgba(255, 77, 79, 0.2);
}

.role-badge.admin {
  background-color: rgba(24, 144, 255, 0.1);
  color: #1890ff;
  border: 1px solid rgba(24, 144, 255, 0.2);
}

/* 各设置面板并排，窄屏自动叠成一列 */
.card-grid {
  max-width: 880px;
  margin: 24px auto 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 24px;
  align-items: start;
}

.card-grid .card {
  margin: 0;
  max-width: none;
}

.toggle-header {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--text-color);
  font-family: inherit;
  cursor: pointer;
}

.toggle-header:hover .card-title {
  color: var(--primary-color);
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  transition: color 0.2s;
}

.chevron {
  margin-left: auto;
  font-size: 1.2em;
  line-height: 1;
  opacity: 0.7;
  transition: transform 0.3s ease;
}

.chevron.rotated {
  transform: rotate(90deg);
}

.setting-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
  animation: slideDown 0.3s ease-out;
}

.setting-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 10px 14px;
  border-radius: 12px;
  background: var(--playlist-item-bg);
  font-size: 14px;
}

.section-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 16px;
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.label-text {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color);
  text-align: left;
}

input[type="text"],
input[type="password"] {
  width: 100%;
  padding: 8px 12px;
  border-radius: 12px;
  border: 1px solid var(--border-color);
  background: var(--playlist-item-bg);
  color: var(--text-color);
  outline: none;
  font-size: 15px;
  font-family: inherit;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

input:focus {
  border-color: var(--primary-color);
}

.action-btn {
  align-self: flex-start;
  padding: 10px 20px;
  border-radius: 8px;
  border: 1px solid var(--secondary-text-color);
  background: var(--playlist-item-bg);
  color: var(--secondary-text-color);
  font-family: inherit;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover:not(:disabled) {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: #fff;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  border-color: var(--border-color);
  color: var(--text-color);
}

.hint,
.muted {
  margin: 0;
  font-size: 13px;
  color: var(--text-color);
  opacity: 0.75;
}

.hint {
  max-width: 880px;
  margin: 24px auto 0;
}

.error {
  margin: 0;
  padding: 8px 12px;
  border-radius: 8px;
  color: var(--danger-color);
  font-size: 14px;
  background: rgba(255, 82, 82, 0.1);
}
</style>
