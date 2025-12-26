<!-- src/components/layout/Sidebar.vue -->
<template>
  <div :class="['sidebar', { open: isOpen }]">
    <!-- 汉堡按钮 -->
    <button class="hamburger" @click="toggleSidebar">
      <span></span>
      <span></span>
      <span></span>
    </button>

    <!-- 菜单 -->
    <nav class="menu" :class="{ visible: isOpen }">
      <ul>
        <li>
          <a href="#" @click.prevent="onAuthClick">
            {{ authStore.isAuthenticated ? '登出' : '登录' }}
          </a>
        </li>
        <li><a href="#" @click.prevent="showHome">首页</a></li>
        <li><a href="#" @click.prevent="showAbout">关于本站</a></li>
        <li><a href="#" @click.prevent="showPrivacy">隐私政策</a></li>
        <li>
          <a href="#" @click.prevent="showAuthor" class="theme-toggle-link">
            <img :src="linkIcon" class="svg-icon" alt="链接" />
            关于小蜜蜂
          </a>
        </li>
        <li>
          <a href="#" @click.prevent="toggleTheme" class="theme-toggle-link">
            <img :src="themeStore.isDarkMode ? sunIcon : moonIcon" class="svg-icon" alt="切换模式" />
            {{ themeStore.isDarkMode ? '日间模式' : '夜间模式' }}
          </a>
        </li>
      </ul>
    </nav>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore, useThemeStore } from '@/store/index.js'
import {eventBus} from "@/utils/eventBus.js";
import sunIcon from '@/assets/icons/theme/sun.svg'
import moonIcon from '@/assets/icons/theme/moon.svg'
import linkIcon from '@/assets/icons/link.svg'

const isOpen = ref(false)
const router = useRouter()
const authStore = useAuthStore()
const themeStore = useThemeStore()

// 定义父组件通信事件
const emit = defineEmits(['open-login', 'open-register', 'request-logout'])

function toggleSidebar() {
  isOpen.value = !isOpen.value
}

function toggleTheme() {
  themeStore.toggleTheme()
}

// 路由跳转
function showHome() {
  router.push('/')
  isOpen.value = false
}
function showPrivacy() {
  router.push('/privacy')
  isOpen.value = false
}
function showAbout() {
  router.push('/about')
  isOpen.value = false
}
function showAuthor() {
  window.open('https://github.com/HarvestMoons/HarvestMoons', '_blank')
  isOpen.value = false
}

// 登录 / 登出处理
function onAuthClick() {
  if (authStore.isAuthenticated) {
    eventBus.emit('request-logout')
  } else {
    eventBus.emit('open-login')
  }
}

</script>

<style scoped>
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  height: 100vh;
  width: 60px;
  background-color: var(--sidebar-bg, #2c2c2c);
  box-shadow: var(--sidebar-shadow);
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: width 0.3s ease, background-color 0.3s ease, box-shadow 0.3s ease;
  z-index: 1000;
  overflow: hidden;
}

.sidebar.open {
  width: 220px;
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
  flex-shrink: 0;
}

.hamburger span {
  display: block;
  height: 3px;
  width: 100%;
  background-color: var(--hamburger-color, #e0e0e0);
  border-radius: 2px;
  transition: all 0.3s ease;
}

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
  visibility: hidden;
  pointer-events: none;
  transition: opacity 0.2s ease, visibility 0s linear 0.3s;
}

.menu.visible {
  opacity: 1;
  visibility: visible;
  pointer-events: auto;
  transition: opacity 0.3s ease 0.15s, visibility 0s linear 0s;
}

.menu ul {
  list-style: none;
  padding: 0;
}

.menu li {
  margin-bottom: 20px;
}

.menu a {
  color: var(--sidebar-text, #e0e0e0);
  text-decoration: none;
  font-weight: 500;
  font-size: 16px;
  line-height: 1.8;
  letter-spacing: 1px;
  transition: color 0.2s ease;
}

.menu a:hover {
  color: var(--sidebar-text-hover, #ffffff);
  text-decoration: underline;
}

.theme-toggle-link {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
