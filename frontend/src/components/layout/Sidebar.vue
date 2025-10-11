<template>
  <div :class="['sidebar', { open: isOpen }]">
    <!-- 汉堡按钮 -->
    <button class="hamburger" @click="toggleSidebar">
      <span></span>
      <span></span>
      <span></span>
    </button>

    <!-- 菜单内容（始终存在，用透明度和可见性控制） -->
    <nav class="menu" :class="{ visible: isOpen }">
      <ul>
        <li>
          <a href="#" @click.prevent="handleAuthAction">
            {{ authStore.isAuthenticated ? '登出' : '登录' }}
          </a>
        </li>
        <li><a href="#" @click.prevent="showHome">首页</a></li>
        <li><a href="#" @click.prevent="showAbout">关于本站</a></li>
        <li><a href="#" @click.prevent="showPrivacy">隐私政策</a></li>
        <li><a href="#" @click.prevent="showAuthor">🔗关于小蜜蜂</a></li>
      </ul>
    </nav>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import {logout} from "../../services/auth.js";
import {useAuthStore} from "../../store/index.js";

const isOpen = ref(false);
const router = useRouter();
const authStore = useAuthStore();

function toggleSidebar() {
  isOpen.value = !isOpen.value;
}

function showHome() {
  router.push('/');
}

function showPrivacy() {
  router.push('/privacy');
}

function showAbout() {
  router.push('/about');
}

function showAuthor() {
  window.open("https://github.com/HarvestMoons/HarvestMoons", "_blank");
}

async function handleAuthAction() {
  if (authStore.isAuthenticated) {
    try {
      await logout(); // 调用注销 API
      authStore.logout(); // 更新 Pinia 状态
      await router.push('/'); // 重定向到首页
    } catch (error) {
      console.error('登出失败:', error);
    }
  } else {
    await router.push('/login'); // 未登录时跳转到登录页面
  }
}
</script>

<style scoped>
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  height: 100vh;
  width: 60px; /* 收起状态 */
  background-color: #2c2c2c; /* 偏灰暗色 */
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: width 0.3s ease;
  z-index: 1000;
  overflow: hidden;
}

.sidebar.open {
  width: 220px; /* 展开宽度 */
  align-items: flex-start;
}

.hamburger {
  margin: 20px auto;
  width: 30px;
  height: 25px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 0;
}

.hamburger span {
  display: block;
  height: 3px;
  width: 100%;
  background-color: #e0e0e0; /* 浅灰色横线 */
  border-radius: 2px;
  transition: all 0.3s ease;
}

/* 汉堡按钮动画 */
.sidebar.open .hamburger span:nth-child(1) {
  transform: rotate(45deg) translate(5px, 5px);
}
.sidebar.open .hamburger span:nth-child(2) {
  opacity: 0;
}
.sidebar.open .hamburger span:nth-child(3) {
  transform: rotate(-45deg) translate(5px, -5px);
}

.menu {
  margin-top: 60px;
  padding: 10px 20px;
  opacity: 0;
  visibility: hidden; /* 防止收起时文字占位/被挤压 */
  pointer-events: none;
  transition: opacity 0.2s ease, visibility 0s linear 0.3s;
  /* 收起时延迟隐藏，等宽度动画结束 */
}

.menu.visible {
  opacity: 1;
  visibility: visible;
  pointer-events: auto;
  transition: opacity 0.3s ease 0.15s, visibility 0s linear 0s;
  /* 展开时稍微延迟淡入，避免文字提前出现 */
}

.menu ul {
  list-style: none;
  padding: 0;
}

.menu li {
  margin-bottom: 20px; /* 选项之间更宽 */
}

.menu a {
  color: #e0e0e0;
  text-decoration: none;
  font-weight: 500;
  font-size: 16px; /* 字体更大 */
  line-height: 1.8; /* 增加行高 */
  letter-spacing: 1px; /* 字间距稍微拉开 */
  transition: color 0.2s ease;
}

.menu a:hover {
  color: #ffffff;
  text-decoration: underline;
}
</style>