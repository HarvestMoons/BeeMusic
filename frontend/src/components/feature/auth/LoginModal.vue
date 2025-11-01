<template>
  <teleport to="body">
    <div v-if="visible" class="modal-overlay" @click.self="close" role="dialog" aria-modal="true">
      <div class="modal-card" @keydown.esc="close">
        <button class="modal-close" @click="close" aria-label="关闭">✕</button>

        <h2 class="modal-title">登录</h2>

        <form @submit.prevent="handleLogin" class="modal-form" autocomplete="on">
          <label class="field">
            <span class="label-text">用户名</span>
            <input v-model="username" id="login-username" type="text" required autocomplete="username" />
          </label>

          <label class="field">
            <span class="label-text">密码</span>
            <input v-model="password" id="login-password" type="password" required autocomplete="current-password" />
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
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../../../services/auth.js'     // <- 若路径不同请调整
import { useAuthStore } from '../../../store/index.js' // <- 若路径不同请调整

const props = defineProps({
  visible: { type: Boolean, default: false }
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
    await login({ username: username.value, password: password.value }) // 你的现有逻辑
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
  background: rgba(0,0,0,0.5);
  z-index: 3000; /* 高于 sidebar(1000) */
}

.modal-card {
  width: 420px;
  max-width: calc(100vw - 48px);
  background: #2c2c2c; /* 不改变你的配色 */
  color: #ffffff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.6);
  position: relative;
  transform-origin: center;
  animation: pop 180ms ease;
}

@keyframes pop {
  from { opacity: 0; transform: scale(0.96); }
  to { opacity: 1; transform: scale(1); }
}

.modal-close {
  position: absolute;
  right: 12px;
  top: 10px;
  background: transparent;
  border: none;
  color: #cfcfcf;
  font-size: 16px;
  cursor: pointer;
}

.modal-title {
  margin: 0 0 12px 0;
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  text-align: left;
}

.modal-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.label-text {
  font-size: 13px;
  color: #cfcfcf;
  text-align: left;
}

input[type="text"],
input[type="password"] {
  width: 100%;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid rgba(255,255,255,0.06);
  background: #1a1a1a;
  color: #fff;
  outline: none;
  font-size: 14px;
}
input:focus {
  box-shadow: 0 0 0 3px rgba(100,111,255,0.08);
  border-color: rgba(100,111,255,0.2);
}

.primary-btn {
  width: 100%;
  padding: 10px 12px;
  background: #5ab9ea; /* 保留与原控件接近的主色 */
  color: #0b2540;
  border-radius: 8px;
  border: none;
  font-weight: 600;
  cursor: pointer;
}
.primary-btn:disabled {
  opacity: 0.6;
  cursor: default;
}

.muted {
  font-size: 13px;
  color: #bdbdbd;
  text-align: center;
  margin-top: 6px;
}
.muted a { color: #646cff; text-decoration: none; }
.muted a:hover { text-decoration: underline; }

.error {
  color: #ff7b7b;
  font-size: 13px;
  text-align: center;
  margin-top: 6px;
}
</style>
