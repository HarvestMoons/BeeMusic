<script setup>
import { onMounted, onBeforeUnmount } from 'vue';

const props = defineProps({
  apiBase: { type: String, default: '/api' },
  swRegister: { type: Boolean, default: false },
});

let darkenInterval = null;
let overlay = null;
let longPressTimer = null;
let isTriggered = false;
let videoElement = null;
let clickCount = 0;

const CACHE_NAME = 'video-cache-v1';
const CACHE_EXPIRY_TIME = 24 * 60 * 60 * 1000;

function now() { return Date.now(); }

// -------------------- SW 注册 --------------------
async function tryRegisterSW() {
  if (!props.swRegister || !('serviceWorker' in navigator)) return;
  try {
    const reg = await navigator.serviceWorker.register('/sw.js');
    console.log('SW registered in component: ', reg);
  } catch (err) {
    console.warn('SW registration failed (component):', err);
  }
}

// -------------------- 缓存管理 --------------------
async function isVideoCached(videoId) {
  if (!('caches' in window)) return false;
  try {
    const cache = await caches.open(CACHE_NAME);
    const res = await cache.match(`/video/${videoId}`);
    if (!res) return false;
    const expiry = res.headers.get('x-cache-expiry');
    if (expiry && now() > parseInt(expiry, 10)) {
      await cache.delete(`/video/${videoId}`);
      return false;
    }
    return true;
  } catch (e) {
    console.warn('检查缓存失败:', e);
    return false;
  }
}

async function addVideoToCache(videoId, videoUrl) {
  if (!('caches' in window)) return;
  try {
    const cache = await caches.open(CACHE_NAME);
    const response = await fetch(videoUrl, { credentials: 'omit' }); // 显式设置 credentials
    if (!response || response.status !== 200) return;

    const headers = new Headers(response.headers);
    headers.set('x-cache-expiry', String(now() + CACHE_EXPIRY_TIME));
    headers.set('x-video-id', String(videoId));

    await cache.put(`/video/${videoId}`, new Response(response.body, {
      status: response.status,
      statusText: response.statusText,
      headers
    }));
    console.log('视频已缓存，ID:', videoId);
  } catch (e) {
    console.warn('缓存视频失败:', e);
  }
}
async function getCachedVideoUrl(videoId) {
  if (!('caches' in window)) return null;
  try {
    const cache = await caches.open(CACHE_NAME);
    const res = await cache.match(`/video/${videoId}`);
    if (!res) return null;
    return URL.createObjectURL(await res.blob());
  } catch (e) {
    console.warn('获取缓存视频失败:', e);
    return null;
  }
}

// -------------------- 播放控制 --------------------
function stopVideoInternal() {
  if (videoElement) {
    try { if (videoElement.src?.startsWith('blob:')) URL.revokeObjectURL(videoElement.src); } catch {}
    videoElement.pause();
    videoElement.remove();
    videoElement = null;
  }

  if (overlay) { overlay.remove(); overlay = null; }

  clearInterval(darkenInterval);
  clearTimeout(longPressTimer);
  isTriggered = false;

  const mainAudio = document.getElementById('audio-player');
  if (mainAudio) try { mainAudio.play(); } catch {}
}

async function playCachedVideo(selectedVideo, fromCache = false) {
  videoElement = document.createElement('video');

  if (fromCache) {
    const cachedUrl = await getCachedVideoUrl(selectedVideo.id);
    videoElement.src = cachedUrl || selectedVideo.url;
    console.log(cachedUrl ? '✅ 从缓存播放视频:' : '⚠️ 缓存获取失败，从网络播放:', selectedVideo.id);
  } else {
    try {
      const response = await fetch(selectedVideo.url, { credentials: 'omit' }); // 添加 fetch 并设置 credentials
      if (!response.ok) throw new Error('Failed to fetch video');
      const blob = await response.blob();
      videoElement.src = URL.createObjectURL(blob);
      console.log('🌐 从网络播放视频:', selectedVideo.id);
    } catch (e) {
      console.error('网络播放视频失败:', e);
      stopVideoInternal();
      return;
    }
  }

  videoElement.preload = 'auto';
  videoElement.autoplay = true;
  videoElement.loop = false;
  Object.assign(videoElement.style, {
    position: 'fixed', top: '0', left: '0', width: '100vw', height: '100vh',
    objectFit: 'cover', pointerEvents: 'none', zIndex: '10000'
  });

  const mainAudio = document.getElementById('audio-player');
  if (mainAudio) mainAudio.pause();

  videoElement.addEventListener('playing', () => {
    if (!overlay) return;

    overlay.classList.add('video-playing'); // 开启事件拦截

    // 阻止除点击外的所有穿透事件
    ['mousedown','mouseup','mousemove','wheel','touchstart','touchmove'].forEach(ev =>
        overlay.addEventListener(ev, e => { e.stopPropagation(); e.preventDefault(); }, true)
    );

    // 监听 overlay 点击实现三次点击退出
    clickCount = 0;
    const onOverlayClick = () => {
      clickCount++;
      if (clickCount >= 3) {
        stopVideoInternal();
        overlay.removeEventListener('click', onOverlayClick);
      }
    };
    overlay.addEventListener('click', onOverlayClick);
  });

  videoElement.addEventListener('ended', stopVideoInternal);
  videoElement.addEventListener('error', stopVideoInternal);

  document.body.appendChild(videoElement);
}

// -------------------- Overlay & 长按触发 --------------------
function clearOverlay() {
  clearTimeout(longPressTimer);
  clearInterval(darkenInterval);
  if (overlay) { overlay.remove(); overlay = null; }
}

onMounted(() => {
  void tryRegisterSW();

  const mainAudio = document.getElementById('audio-player');

  // -------------------- 长按E键触发 --------------------
  const keydownHandler = (e) => {
    if (e.key.toLowerCase() !== 'e' || mainAudio && videoElement || e.repeat) return;

    // 清理旧 overlay（未触发状态）
    if (!isTriggered) clearOverlay();

    longPressTimer = setTimeout(async () => {
      isTriggered = true; // ✅ 标记动画已触发

      overlay = document.createElement('div');
      overlay.className = 'ripple-overlay';
      document.body.appendChild(overlay);

      const ripple = document.createElement('div');
      ripple.className = 'ripple-circle';
      ripple.style.left = '50%'; // 居中显示
      ripple.style.top = '50%';
      overlay.appendChild(ripple);

      const darkBg = document.createElement('div');
      darkBg.className = 'dark-background';
      overlay.appendChild(darkBg);

      setTimeout(() => overlay.classList.add('active'), 10);

      darkenInterval = setTimeout(async () => {
        if (mainAudio) mainAudio.pause();

        try {
          const res = await fetch(`${props.apiBase}/public/videos/random`);
          if (!res.ok) throw new Error('HTTP ' + res.status);
          const videos = await res.json();
          if (!videos || videos.length === 0) {
            clearOverlay();
            return;
          }

          const selected = videos[Math.floor(Math.random() * videos.length)];
          if (await isVideoCached(selected.id)) await playCachedVideo(selected, true);
          else {
            await playCachedVideo(selected, false);
            void addVideoToCache(selected.id, selected.url);
          }

        } catch (err) {
          console.error(err);
          clearOverlay();
        }
      }, 2000); // ✅ 动画 2 秒后播放视频，无论键盘是否松开

    }, 3000); // 长按 E 键 3 秒触发
  };

  // -------------------- keyup --------------------
  const keyupHandler = (e) => {
    if (e.key.toLowerCase() === 'e' && !isTriggered) clearOverlay();
  };

  document.addEventListener('keydown', keydownHandler);
  document.addEventListener('keyup', keyupHandler);

  document.__rippleCleanup = () => {
    document.removeEventListener('keydown', keydownHandler);
    document.removeEventListener('keyup', keyupHandler);
    stopVideoInternal();
  };
});

onBeforeUnmount(() => {
  if (document.__rippleCleanup) {
    try {
      document.__rippleCleanup();
    } catch {
    }
    delete document.__rippleCleanup;
  }
});
</script>

<style>
.ripple-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 9999;
  pointer-events: none;
  overflow: hidden;
}

.ripple-circle {
  position: absolute;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(0, 0, 0, 0.9) 0%, rgba(0, 0, 0, 0) 70%);
  transform: translate(-50%, -50%) scale(0);
  opacity: 0;
  transition: all 2s ease-out;
}

.dark-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: #000;
  opacity: 0;
  transition: opacity 2s ease-in-out;
}

/* 视频播放时遮挡下方控件，开启事件拦截 */
.ripple-overlay.video-playing {
  pointer-events: auto;
}

.ripple-overlay.active .ripple-circle {
  opacity: 1;
  transform: translate(-50%, -50%) scale(30);
}

.ripple-overlay.active .dark-background {
  opacity: 1;
}

.ripple-overlay.video-playing .ripple-circle {
  opacity: 0;
  transition: opacity 0.5s ease-in;
}

.ripple-overlay.video-playing .dark-background {
  opacity: 0;
  transition: opacity 0.5s ease-in;
}

.ripple-overlay.fade-out .ripple-circle,
.ripple-overlay.fade-out .dark-background {
  opacity: 0 !important;
  transition: opacity 0.3s ease-in;
}
</style>