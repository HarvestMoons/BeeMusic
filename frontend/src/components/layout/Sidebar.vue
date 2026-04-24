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
            {{ item.label }}
          </RouterLink>
        </li>

        <li class="nav-item settings-group">
          <button type="button" @click="toggleDisplaySettings" class="nav-link nav-button theme-toggle-link">
            <img :src="settingsIcon" class="svg-icon" alt="显示设置"/>
            <span>显示设置</span>
            <span class="chevron" :class="{ rotated: showDisplaySettings }">›</span>
          </button>

          <ul v-show="showDisplaySettings" class="sub-menu">
            <li class="sub-item" @click.stop>
              <span>夜间模式</span>
              <ToggleSwitch
                  v-model="isDarkMode"
                  @change="handleThemeToggle"
              />
            </li>
            <li class="sub-item" @click.stop>
              <span>背景粒子</span>
              <ToggleSwitch
                  v-model="showParticles"
                  @change="handleParticlesToggle"
              />
            </li>
          </ul>
        </li>

        <li v-if="authStore.isStationMaster" class="nav-item settings-group">
          <button type="button" @click="toggleSiteSettings" class="nav-link nav-button theme-toggle-link">
            <img :src="settingsIcon" class="svg-icon" alt="站长设置"/>
            <span>站长设置</span>
            <span class="chevron" :class="{ rotated: showSiteSettings }">›</span>
          </button>

          <ul v-show="showSiteSettings" class="sub-menu">
            <li class="sub-item" @click.stop>
              <span>全站评论</span>
              <ToggleSwitch
                  v-model="commentsEnabledProxy"
                  @change="handleCommentsToggle"
              />
            </li>
            <li class="sub-item">
              <button type="button" @click="handleSyncDatabase" class="sub-action-button theme-toggle-link">
                <img :src="restoreIcon" class="svg-icon" alt="同步" style="width:16px;height:16px;"/>
                <span>同步数据库</span>
              </button>
            </li>
          </ul>
        </li>
      </ul>
    </nav>
  </div>
</template>

<script setup>
import {onMounted, ref, watch} from 'vue'
import {useAuthStore, useSiteConfigStore, useThemeStore} from '@/store/index.js'
import {eventBus, requestConfirm} from "@/utils/eventBus.js";
import settingsIcon from '@/assets/icons/settings.svg'
import ToggleSwitch from '@/components/common/ToggleSwitch.vue'
import {syncDatabase} from "@/services/siteConfig.js";
import restoreIcon from '@/assets/icons/restore.svg'

const isOpen = ref(false)
const showDisplaySettings = ref(false)
const showSiteSettings = ref(false)
const authStore = useAuthStore()
const themeStore = useThemeStore()
const siteConfigStore = useSiteConfigStore()

const navItems = [
  {label: '首页', to: '/'},
  {label: '关于本站', to: '/about'},
  {label: '迷因漂流瓶', to: '/meme'},
  {label: '隐私政策', to: '/privacy'}
]

// Local state proxies for toggles
const isDarkMode = ref(themeStore.isDarkMode)
const showParticles = ref(themeStore.showParticles)
const commentsEnabledProxy = ref(false)

// Watch store changes to sync local state (e.g. if changed elsewhere)
watch(() => themeStore.isDarkMode, (val) => isDarkMode.value = val)
watch(() => themeStore.showParticles, (val) => showParticles.value = val)
watch(() => siteConfigStore.commentsEnabled, (val) => commentsEnabledProxy.value = val)

// 定义父组件通信事件
const emit = defineEmits(['open-login', 'open-register', 'request-logout'])

onMounted(() => {
  siteConfigStore.fetchConfig()
})

function toggleSidebar() {
  isOpen.value = !isOpen.value
}

function closeSidebar() {
  isOpen.value = false
}

function toggleDisplaySettings() {
  showDisplaySettings.value = !showDisplaySettings.value
}

function toggleSiteSettings() {
  showSiteSettings.value = !showSiteSettings.value
}

function handleThemeToggle(val) {
  if (val !== themeStore.isDarkMode) {
    themeStore.toggleTheme()
  }
}

function handleParticlesToggle(val) {
  themeStore.setParticles(val)
}

function handleCommentsToggle(val) {
  siteConfigStore.updateCommentsEnabled(val)
}

async function handleSyncDatabase() {
  const confirmed = await requestConfirm({
    title: '同步数据库',
    message: '确定要立即从 OSS 同步数据库吗？',
    confirmText: '立即同步'
  })
  if (!confirmed) return
  try {
    await syncDatabase();
    eventBus.emit('show-toast', "数据库同步触发成功！");
  } catch (error) {
    console.error("Sync failed", error);
    eventBus.emit('show-toast', "同步失败，请重试。");
  }
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

.theme-toggle-link:hover {
  color: var(--sidebar-text-hover, #ffffff);
}

.theme-toggle-link {
  justify-content: flex-start;
}

.settings-group {
  display: flex;
  flex-direction: column;
}

.chevron {
  margin-left: auto;
  transition: transform 0.3s ease;
  font-size: 1.2em;
  line-height: 1;
}

.chevron.rotated {
  transform: rotate(90deg);
}

.sub-menu {
  list-style: none;
  padding-left: 32px;
  margin-top: 15px;
  display: flex;
  flex-direction: column;
  gap: 8px;
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

.sub-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: var(--secondary-text-color, #e0e0e0);
}

.sub-item span {
  flex: 1;
}

.sub-action-button {
  border: none;
  background: transparent;
  color: inherit;
  font: inherit;
  width: 100%;
  padding: 0;
  cursor: pointer;
  text-align: left;
}
</style>
