<!-- src/App.vue -->
<template>
  <div id="app" class="flex">
    <!-- 侧边栏：负责触发登录、注册、登出事件 -->
    <Sidebar
        @open-login="openLogin"
        @open-register="openRegister"
        @request-logout="handleLogout"
    />

    <!-- 主体部分 -->
    <div class="flex-1">
      <HomePage v-show="route.path === '/'"/>
      <router-view v-if="route.path !== '/'"/>
    </div>

    <!-- 登录弹窗 -->
    <LoginModal
        :visible="showLogin"
        @close="showLogin = false"
        @switch="onModalSwitch"
    />

    <!-- 注册弹窗 -->
    <RegisterModal
        :visible="showRegister"
        @close="showRegister = false"
        @switch="onModalSwitch"
    />

    <!-- 全局 Toast -->
    <Toast :visible="toastVisible" :message="toastMessage"/>
  </div>
</template>

<script setup>
import {onBeforeUnmount, onMounted, ref} from 'vue'
import {useRoute} from 'vue-router'
import Sidebar from '@/components/layout/Sidebar.vue'
import HomePage from '@/views/HomePage.vue'
import LoginModal from '@/components/feature/auth/LoginModal.vue'
import RegisterModal from "@/components/feature/auth/RegisterModal.vue";
import Toast from '@/components/common/Toast.vue'
import {logout} from '@/services/auth.js'
import {useAuthStore, useThemeStore} from '@/store/index.js'
import {eventBus} from "@/utils/eventBus.js";

const route = useRoute()
const showLogin = ref(false)
const showRegister = ref(false)
const authStore = useAuthStore()
const themeStore = useThemeStore()

// Toast 状态
const toastVisible = ref(false)
const toastMessage = ref('')

function showToast(msg) {
  toastMessage.value = msg
  toastVisible.value = true
  setTimeout(() => {
    toastVisible.value = false
  }, 3000)
}

onMounted(() => {
  themeStore.initTheme()
  eventBus.on('open-login', () => showLogin.value = true)
  eventBus.on('open-register', () => showRegister.value = true)
  eventBus.on('request-logout', handleLogout)
})

onBeforeUnmount(() => {
  eventBus.all.clear()
})

function openLogin() {
  showLogin.value = true
}

function openRegister() {
  showRegister.value = true
}

function onModalSwitch(type) {
  if (type === 'register') {
    showLogin.value = false
    showRegister.value = true
  } else if (type === 'login') {
    showRegister.value = false
    showLogin.value = true
  }
}

async function handleLogout() {
  // 移除 window.confirm，直接登出并显示 Toast
  try {
    await logout()
    authStore.logout()
    showToast('已成功退出登录')
  } catch (err) {
    console.error('登出失败:', err)
    showToast('登出失败，请重试')
  }
}
</script>

<style scoped>
:deep(.modal-overlay) {
  position: fixed !important;
  top: 0 !important;
  left: 0 !important;
  width: 100vw !important;
  height: 100vh !important;
  z-index: 4000 !important;
}
</style>
