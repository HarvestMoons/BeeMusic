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
        <li><a href="#" @click.prevent="showMeme">迷因漂流瓶</a></li>
        <li><a href="#" @click.prevent="showPrivacy">隐私政策</a></li>
        <li>
          <a href="#" @click.prevent="showAuthor" class="theme-toggle-link">
            <img :src="linkIcon" class="svg-icon" alt="链接"/>
            关于小蜜蜂
          </a>
        </li>

        <li class="settings-group">
          <a href="#" @click.prevent="toggleDisplaySettings" class="theme-toggle-link">
            <img :src="settingsIcon" class="svg-icon" alt="显示设置"/>
            <span>显示设置</span>
            <span class="chevron" :class="{ rotated: showDisplaySettings }">›</span>
          </a>

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

        <li v-if="authStore.isStationMaster" class="settings-group">
          <a href="#" @click.prevent="toggleSiteSettings" class="theme-toggle-link">
            <img :src="settingsIcon" class="svg-icon" alt="站长设置"/>
            <span>站长设置</span>
            <span class="chevron" :class="{ rotated: showSiteSettings }">›</span>
          </a>

          <ul v-show="showSiteSettings" class="sub-menu">
            <li class="sub-item" @click.stop>
              <span>全站评论</span>
              <ToggleSwitch
                  v-model="commentsEnabledProxy"
                  @change="handleCommentsToggle"
              />
            </li>
          </ul>
        </li>
      </ul>
    </nav>
  </div>
</template>

<script setup>
import {ref, watch, onMounted} from 'vue'
import {useRouter} from 'vue-router'
import {useAuthStore, useThemeStore, useSiteConfigStore} from '@/store/index.js'
import {eventBus} from "@/utils/eventBus.js";
import settingsIcon from '@/assets/icons/settings.svg'
import linkIcon from '@/assets/icons/link.svg'
import ToggleSwitch from '@/components/common/ToggleSwitch.vue'

const isOpen = ref(false)
const showDisplaySettings = ref(false)
const showSiteSettings = ref(false)
const router = useRouter()
const authStore = useAuthStore()
const themeStore = useThemeStore()
const siteConfigStore = useSiteConfigStore()

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

// 路由跳转
const showHome = () => {
  router.push('/');
  isOpen.value = false;
};

const showMeme = () => {
  router.push('/meme');
  isOpen.value = false;
};

function showAbout() {
  router.push('/about')
  isOpen.value = false
}

function showPrivacy() {
  router.push('/privacy')
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
  width: 100%;
  box-sizing: border-box;
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
  color: var(--secondary-text-color, #e0e0e0);
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

.menu a.theme-toggle-link:hover {
  text-decoration: none;
  color: var(--sidebar-text-hover, #ffffff);
}

.menu a.theme-toggle-link:hover span:not(.chevron) {
  text-decoration: underline;
}

.theme-toggle-link {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  cursor: pointer;
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
</style>
