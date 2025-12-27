<template>
  <teleport to="body">
    <div v-if="visible" class="modal-overlay" @click.self="close" role="dialog" aria-modal="true">
      <div class="modal-card" @keydown.esc="close">
        <button class="modal-close" @click="close" aria-label="关闭">✕</button>

        <h2 class="modal-title">登录</h2>

        <form @submit.prevent="handleLogin" class="modal-form" autocomplete="on">
          <label class="field">
            <span class="label-text">用户名</span>
            <input v-model="username" id="login-username" type="text" required autocomplete="username"/>
          </label>

          <label class="field">
            <span class="label-text">密码</span>
            <input v-model="password" id="login-password" type="password" required autocomplete="current-password"/>
          </label>

          <button class="primary-btn" type="submit" :disabled="loading">
            <span v-if="!loading">登录</span>
            <span v-else>登录中…</span>
          </button>

          <p class="muted">
            还没有账号？
            <a href="#" @click.prevent="switchToRegister">注册</a>
          </p>

          <p v-if="error" class="error">{{ error }}</p>
        </form>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import {ref} from 'vue'
import {useRouter} from 'vue-router'
import {login} from '@/services/auth.js'
import {useAuthStore} from '@/store/index.js'

const props = defineProps({
  visible: {type: Boolean, default: false}
})
const emit = defineEmits(['close', 'switch'])

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

const router = useRouter()
const authStore = useAuthStore()

function close() {
  error.value = ''
  username.value = ''
  password.value = ''
  loading.value = false
  emit('close')
}

function switchToRegister() {
  emit('switch', 'register')
}

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    await login({username: username.value, password: password.value}) // 你的现有逻辑
    await authStore.fetchUserStatus() // 保持原逻辑：刷新认证状态
    emit('close')
    await router.push('/') // 保持原逻辑：跳转首页
  } catch (err) {
    console.error('Login error:', err)
    error.value = '登录失败，请检查用户名和密码'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  z-index: 3000;
}

.modal-card {
  width: 380px;
  max-width: 90vw;
  background: var(--modal-bg);
  color: var(--modal-text);
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  position: relative;
  transform-origin: center;
  animation: pop 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: 1px solid var(--border-color);
}

@keyframes pop {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.modal-close {
  position: absolute;
  right: 16px;
  top: 16px;
  background: transparent;
  border: none;
  color: var(--text-color);
  opacity: 0.5;
  font-size: 20px;
  cursor: pointer;
  transition: opacity 0.2s;
  line-height: 1;
}

.modal-close:hover {
  opacity: 1;
}

.modal-title {
  margin: 0 0 24px 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--modal-text);
  text-align: center;
}

.modal-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
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
  width: 100%;
  padding: 12px;
  margin-top: 8px;
  background: var(--primary-color);
  color: #fff; /* 假设 primary color 较深，或者根据主题调整 */
  border-radius: 10px;
  border: none;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.1s, filter 0.2s;
}

/* 针对浅色主题下 primary color #f9a825 (黄色) 的文字颜色优化 */
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

.muted {
  font-size: 14px;
  color: var(--text-color);
  opacity: 0.7;
  text-align: center;
  margin-top: 16px;
}

.muted a {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
  margin-left: 4px;
}

.muted a:hover {
  text-decoration: underline;
}

.error {
  color: #ff5252;
  font-size: 14px;
  text-align: center;
  margin-top: 8px;
  background: rgba(255, 82, 82, 0.1);
  padding: 8px;
  border-radius: 8px;
}
</style>
