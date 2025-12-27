<template>
  <div class="trigger-container">
    <!-- Interaction Layer -->
    <div 
      class="center-trigger" 
      @click="handleCenterClick"
      :class="{ 'disabled': unlocked }"
    >
      <div 
        class="resonance-wave" 
        v-if="clickCount > 0 && !unlocked"
        :style="{ 
          transform: `scale(${0.5 + clickCount * 0.15})`,
          opacity: clickCount * 0.15 
        }"
      ></div>
    </div>

    <!-- Unlock Success Overlay -->
    <div v-if="showUnlockSuccess" class="unlock-overlay">
      <div class="particles">
        <span v-for="n in 12" :key="n" class="particle" :style="`--i:${n}`"></span>
      </div>
      <div class="unlock-message">
        <span class="text">{{ playlistName }}<br>已解锁</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, defineProps, defineEmits } from 'vue'
import { unlockHiddenPlaylist } from '@/services/auth'

const props = defineProps({
  unlocked: {
    type: Boolean,
    required: true
  },
  playlistName: {
    type: String,
    default: '隐藏歌单'
  }
})

const emit = defineEmits(['on-unlock'])

const clickCount = ref(0)
const isUnlocking = ref(false)
const showUnlockSuccess = ref(false)
let clickResetTimer = null

async function handleCenterClick() {
  if (props.unlocked || isUnlocking.value) return

  clickCount.value++

  // 2秒内无操作重置计数
  if (clickResetTimer) clearTimeout(clickResetTimer)
  clickResetTimer = setTimeout(() => {
    clickCount.value = 0
  }, 2000)

  if (clickCount.value >= 7) {
    triggerUnlock()
  }
}

async function triggerUnlock() {
  isUnlocking.value = true
  try {
    await unlockHiddenPlaylist()
    // 显示成功动画
    showUnlockSuccess.value = true

    // 通知父组件更新状态（延迟以便动画播放）
    setTimeout(() => {
      emit('on-unlock')
      showUnlockSuccess.value = false
      isUnlocking.value = false
      clickCount.value = 0
    }, 3000)
  } catch (err) {
    console.error('Unlock failed', err)
    isUnlocking.value = false
    clickCount.value = 0
  }
}
</script>

<style scoped>
.trigger-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none; /* Let clicks pass through container */
}

.center-trigger {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 55%;
  height: 55%;
  border-radius: 50%;
  cursor: pointer;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent; 
  -webkit-tap-highlight-color: transparent;
  pointer-events: auto; /* Re-enable clicks for trigger */
}

.center-trigger.disabled {
  cursor: default;
  pointer-events: none;
}

.resonance-wave {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255,215,0,0.4) 0%, rgba(255,215,0,0) 70%);
  box-shadow: 0 0 20px rgba(255, 215, 0, 0.3);
  transition: all 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  pointer-events: none;
}

.center-trigger:active .resonance-wave {
  transform: scale(0.9) !important;
}

.unlock-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 20;
  background: rgba(0,0,0,0.6);
  backdrop-filter: blur(4px);
  animation: fadeIn 0.5s ease;
  pointer-events: auto;
}

.unlock-message {
  text-align: center;
  color: #FFD700;
  animation: popIn 0.6s cubic-bezier(0.68, -0.55, 0.265, 1.55);
  user-select: none;
}

.unlock-message .text {
  font-size: 14px;
  font-weight: 800;
  text-shadow: 0 2px 10px rgba(255, 215, 0, 0.5);
  line-height: 1.4;
}

.particles {
  position: absolute;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.particle {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 6px;
  height: 6px;
  background: #FFD700;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  animation: explode 1s ease-out forwards;
  --i: 0;
  --angle: calc(360deg / 12 * var(--i));
}

@keyframes explode {
  0% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 1;
  }
  100% {
    transform: translate(-50%, -50%) rotate(var(--angle)) translateY(-100px) scale(0);
    opacity: 0;
  }
}

@keyframes popIn {
  0% { transform: scale(0); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>
