<!-- src/components/layout/Sidebar.vue -->
<template>
  <div :class="['sidebar', { open: isOpen }]">
    <!-- 汉堡按钮 -->
    <button class="hamburger" @click="toggleSidebar" :aria-expanded="isOpen" :aria-label="isOpen ? '收起侧边栏' : '展开侧边栏'">
      <span></span>
      <span></span>
      <span></span>
    </button>

    <!-- 菜单 -->
    <nav class="menu" :class="{ visible: isOpen }">
      <ul class="nav-list">
        <li class="nav-item action-item">
          <button type="button" class="nav-link nav-button" @click="onAuthClick">
            {{ authStore.isAuthenticated ? '登出' : '登录' }}
          </button>
        </li>

        <li v-for="item in navItems" :key="item.to" class="nav-item">
          <RouterLink
              :to="item.to"
              class="nav-link"
              exact-active-class="is-active"
              @click="closeSidebar"
          >
            <img v-if="item.icon" :src="item.icon" class="svg-icon" alt=""/>
            <span>{{ item.label }}</span>
          </RouterLink>
        </li>
      </ul>
    </nav>
  </div>
</template>

<script setup>
import {ref} from 'vue'
import {useAuthStore} from '@/store/index.js'
import {eventBus} from "@/utils/eventBus.js";
import settingsIcon from '@/assets/icons/settings.svg'

const isOpen = ref(false)
const authStore = useAuthStore()

const navItems = [
  {label: '首页', to: '/'},
  {label: '关于本站', to: '/about'},
  {label: '迷因漂流瓶', to: '/meme'},
  {label: '设置', to: '/settings', icon: settingsIcon}
]

function toggleSidebar() {
  isOpen.value = !isOpen.value
}

function closeSidebar() {
  isOpen.value = false
}

// 登录 / 登出处理
function onAuthClick() {
  closeSidebar()
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
  width: 100%;
  box-sizing: border-box;
}

.menu.visible {
  opacity: 1;
  visibility: visible;
  pointer-events: auto;
  transition: opacity 0.3s ease 0.15s, visibility 0s linear 0s;
}

.nav-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.nav-item {
  margin-bottom: 20px;
}

.nav-link {
  color: var(--secondary-text-color, #e0e0e0);
  text-decoration: none;
  font-weight: 500;
  font-size: 16px;
  line-height: 1.8;
  letter-spacing: 1px;
  transition: color 0.2s ease, background-color 0.2s ease, transform 0.2s ease;
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 10px 12px;
  border-radius: 10px;
  box-sizing: border-box;
}

.nav-link:hover {
  color: var(--sidebar-text-hover, #ffffff);
  background: rgba(var(--primary-color-rgb, 249, 168, 37), 0.12);
  transform: translateX(2px);
}

.nav-link.is-active {
  color: var(--sidebar-text-hover, #ffffff);
  background: rgba(var(--primary-color-rgb, 249, 168, 37), 0.2);
  box-shadow: inset 3px 0 0 var(--primary-color);
}

.nav-button {
  border: none;
  background: transparent;
  cursor: pointer;
  font-family: inherit;
  text-align: left;
}
</style>
