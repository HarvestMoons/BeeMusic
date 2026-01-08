<template>
  <div class="meme-page">
    <div class="meme-content" v-if="loading && !currentMeme">
      <div class="loading">正在打捞漂流瓶...</div>
    </div>
    
    <div class="meme-content" v-else-if="currentMeme">
      <div class="image-container">
        <img 
          :src="currentMeme.url" 
          alt="Meme" 
          @load="loading = false" 
          @error="handleImageError" 
          @click="togglePreview"
          class="meme-main-img"
        />
        <button class="download-btn" @click.stop="downloadMeme" title="下载收藏">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
        </button>
      </div>
      
      <div class="action-bar">
        <button class="btn accent big-btn" @click="fetchRandomMeme" :disabled="loading">
          {{ loading ? '打捞中...' : '下一个漂流瓶' }}
        </button>

        <div class="admin-actions" v-if="authStore.isStationMaster">
          <button class="btn danger" @click="handleDelete" title="删除当前图片">删除</button>
          <button class="btn warning" @click="handleSync" :disabled="syncing" title="同步 OSS 图片库">
            {{ syncing ? '同步中' : '同步' }}
          </button>
        </div>
      </div>
    </div>

    <div class="meme-content" v-else>
      <div class="error">
        <p>这里空空如也...</p>
        <button class="btn" @click="fetchRandomMeme">再试一次</button>
        <div class="mt-4" v-if="authStore.isStationMaster">
             <button class="btn warning" @click="handleSync">初始化同步</button>
        </div>
      </div>
    </div>

    <!-- 图片预览模态框 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showPreview" class="preview-overlay" @click="togglePreview">
          <img :src="currentMeme.url" alt="Full Preview" @click.stop="togglePreview" />
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import {onMounted, onUnmounted, ref, watch} from 'vue';
import {deleteMeme, getRandomMeme, syncMemes} from '@/services/meme';
import {useAuthStore} from '@/store';

const authStore = useAuthStore();
const currentMeme = ref(null);
const loading = ref(true);
const syncing = ref(false);
const showPreview = ref(false);

watch(showPreview, (val) => {
  if (val) {
    document.body.style.overflow = 'hidden';
  } else {
    document.body.style.overflow = '';
  }
});

onUnmounted(() => {
  document.body.style.overflow = '';
});

const togglePreview = () => {
  if (currentMeme.value) {
    showPreview.value = !showPreview.value;
  }
};

// 预加载的 Promise
let nextMemePromise = null;

// 预加载下一张图片的函数
const preloadNext = () => {
  nextMemePromise = getRandomMeme()
    .then(meme => {
      // 如果获取成功，创建一个隐藏的 Image 对象触发浏览器下载并缓存
      if (meme && meme.url) {
        const img = new Image();
        img.src = meme.url;
      }
      return meme;
    })
    .catch(err => {
      console.warn('Preload failed:', err);
      return null;
    });
};

const fetchRandomMeme = async () => {
  loading.value = true;
  try {
    let meme = null;
    
    // 1. 如果有正在进行的预加载，优先使用
    if (nextMemePromise) {
      meme = await nextMemePromise;
      nextMemePromise = null; // 消费掉
    }
    
    // 2. 如果没有预加载（首次进入或预加载失败），则直接请求
    if (!meme) {
      meme = await getRandomMeme();
    }
    
    currentMeme.value = meme;
  } catch (error) {
    console.error('Failed to fetch meme:', error);
    currentMeme.value = null;
  } finally {
    // 3. 无论成功失败，都立即开始预加载下一张
    // 这样用户在看当前图片时，下一张已经在路上了
    preloadNext();

    if (!currentMeme.value) loading.value = false;
  }
};

const handleImageError = () => {
  console.warn('Image load failed, falling back to empty state');
  loading.value = false;
  currentMeme.value = null;
  // 如果预览打开时图片加载失败，关闭预览
  if (showPreview.value) showPreview.value = false;
};

const downloadMeme = async () => {
  if (!currentMeme.value) return;
  
  try {
    const response = await fetch(currentMeme.value.url);
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    
    // 生成文件名：随机哈希 + 时间戳
    const ext = currentMeme.value.key.split('.').pop() || 'jpg';
    const randomHash = Math.random().toString(36).substring(2, 8);
    const timestamp = Date.now();
    a.download = `${randomHash}_${timestamp}.${ext}`;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  } catch (error) {
    console.error('Download failed:', error);
    // 降级处理：在新标签页打开
    window.open(currentMeme.value.url, '_blank');
  }
};

const handleDelete = async () => {
  if (!currentMeme.value || !confirm('确定要将这张图片标记为删除吗？')) return;
  
  try {
    await deleteMeme(currentMeme.value.id);
    alert('已删除');
    fetchRandomMeme();
  } catch (error) {
    alert('删除失败');
  }
};

const handleSync = async () => {
  syncing.value = true;
  try {
    const res = await syncMemes();
    alert(`同步完成! 新增了 ${res.added} 张图片。`);
    if (!currentMeme.value) fetchRandomMeme();
  } catch (error) {
    alert('同步失败: ' + error.message);
  } finally {
    syncing.value = false;
  }
};

onMounted(() => {
  fetchRandomMeme();
});
</script>

<style scoped>
.meme-page {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  width: 100%;
  padding: 20px;
  box-sizing: border-box;
}

.meme-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30px;
  max-width: 800px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  color: var(--text-color);
}

.image-container {
  width: 100%;
  height: 65vh; /* 固定高度保持一致性 */
  min-height: 400px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 25px;
  border-radius: 12px;
  overflow: hidden;
  background: transparent;
  position: relative;
}

.image-container img {
  max-width: 100%;
  max-height: 100%;
  width: auto;
  height: auto;
  object-fit: contain;
  display: block;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.3);
  border-radius: 8px;
}

.download-btn {
  position: absolute;
  bottom: 20px;
  right: 20px;
  background: rgba(30, 30, 30, 0.6);
  backdrop-filter: blur(4px);
  color: white;
  border: 1px solid rgba(255,255,255,0.2);
  border-radius: 50%;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transform: translateY(10px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.image-container:hover .download-btn {
  opacity: 1;
  transform: translateY(0);
}

.download-btn:hover {
  background: rgba(30, 30, 30, 0.85);
  transform: scale(1.1);
}

.action-bar {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  margin-top: 10px;
}

.admin-actions {
  position: absolute;
  right: 0;
  display: flex;
  gap: 10px;
}

/* 移动端适配：空间不足时垂直排列 */
@media (max-width: 600px) {
  .action-bar {
    flex-direction: column;
    gap: 15px;
  }
  .admin-actions {
    position: static;
    width: 100%;
    justify-content: center;
  }
}

.big-btn {
    padding: 12px 40px;
    font-size: 1.1rem;
    box-shadow: 0 4px 15px rgba(0,0,0,0.1);
}

.btn {
  padding: 10px 20px;
  border: 1px solid var(--secondary-text-color);
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s;
  color: var(--secondary-text-color);
  background-color: var(--playlist-item-bg);
}

.btn:hover {
  background-color: var(--playlist-item-hover-bg);
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
  border-color: var(--border-color);
  color: var(--secondary-text-color);
}

/* Accent Button (Primary Action) */
.btn.accent {
  background: var(--playlist-item-bg);
  border-color: var(--secondary-text-color);
  color: var(--secondary-text-color);
}

.btn.accent:hover {
  background: var(--primary-color);
  border-color: var(--primary-color);
  color: #fff;
}

.btn.danger {
  color: var(--secondary-text-color);
  border: 1px solid var(--secondary-text-color);
  background: var(--playlist-item-bg);
}

.btn.danger:hover {
  background: var(--danger-color, #ff4757);
  border-color: var(--danger-color, #ff4757);
  color: white;
}

.btn.warning {
  color: var(--secondary-text-color);
  border: 1px solid var(--secondary-text-color);
  background: var(--playlist-item-bg);
}

.btn.warning:hover {
  background: var(--warning-color, #ffa502);
  border-color: var(--warning-color, #ffa502);
  color: white;
}

.loading, .error {
  text-align: center;
  padding: 40px;
  font-size: 1.2rem;
}

.meme-main-img {
  cursor: zoom-in;
  transition: transform 0.2s ease;
}

.meme-main-img:active {
  transform: scale(0.98);
}

/* 预览模态框样式 */
.preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.9);
  backdrop-filter: blur(8px);
  z-index: 9999;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: zoom-out;
}

.preview-overlay img {
  max-width: 95vw;
  max-height: 95vh;
  object-fit: contain;
  box-shadow: 0 0 30px rgba(0,0,0,0.5);
  user-select: none;
  animation: zoomIn 0.3s cubic-bezier(0.2, 0, 0.2, 1);
}

@keyframes zoomIn {
  from { transform: scale(0.9); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}
</style>
