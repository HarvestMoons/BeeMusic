<!-- src/App.vue -->
<template>
  <div id="app" class="flex">
    <Sidebar
        @open-login="showLogin = true"
        @open-register="showRegister = true"
        @request-logout="handleLogout"
    />

    <div class="flex-1">
      <!-- 临时调试按钮：发布到页面右上角，构建检查用，部署后可删 -->
      <div style="position:fixed; right:18px; top:18px; z-index:4000;">
        <button @click="showLogin = true" style="padding:6px 10px; border-radius:8px;">打开登录弹窗（调试）</button>
      </div>

      <router-view />
    </div>

    <LoginModal
        :visible="showLogin"
        @close="showLogin = false"
        @switch="onModalSwitch"
    />
    <RegisterModal
        :visible="showRegister"
        @close="showRegister = false"
        @switch="onModalSwitch"
    />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
import Sidebar from './components/layout/Sidebar.vue';
import LoginModal from './components/feature/auth/LoginModal.vue';
import RegisterModal from './components/feature/auth/RegisterModal.vue';
import { logout } from './services/auth.js';
import { useAuthStore } from './store/index.js';

const showLogin = ref(false);
const showRegister = ref(false);
const authStore = useAuthStore();

watch(showLogin, v => console.log('App: showLogin=', v));
watch(showRegister, v => console.log('App: showRegister=', v));

function onModalSwitch(type) {
  if (type === 'register') {
    showLogin.value = false;
    showRegister.value = true;
  } else if (type === 'login') {
    showRegister.value = false;
    showLogin.value = true;
  }
}

async function handleLogout() {
  const confirmLogout = window.confirm('确定要登出吗？');
  if (!confirmLogout) return;
  try {
    await logout();
    authStore.logout();
  } catch (err) {
    console.error('登出失败:', err);
  }
}
</script>

<style scoped>
/* 保持你不想改的配色与布局，暂不额外样式 */
:deep(.modal-overlay) {
  position: fixed !important;
  top: 0 !important;
  left: 0 !important;
  width: 100vw !important;
  height: 100vh !important;
  z-index: 4000 !important; /* 确保高于 sidebar */
}
</style>
