<template>
  <div class="folder-selector-container">
    <div class="carousel-stage">
      <div 
        v-for="(folder, index) in folders" 
        :key="folder.value"
        class="folder-card"
        :class="{ active: modelValue === folder.value }"
        :style="getCardStyle(index)"
        @click="selectFolder(folder.value)"
      >
        <div class="card-inner">
          <div class="image-container">
            <img :src="folder.img" :alt="folder.label" />
            <div class="active-indicator" v-if="modelValue === folder.value">
              <div class="playing-icon">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </div>
        <div class="card-info">
          <span class="folder-name">{{ folder.label }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits, computed } from 'vue'

// Import images
import ha_ji_mi from '@/assets/cover/哈基米.jpg'
import dian_gun from '@/assets/cover/电棍.jpg'
import da_si_ma from '@/assets/cover/大司马.jpg'
import ding_zhen from '@/assets/cover/丁真.jpg'
import dxl from '@/assets/cover/东雪莲.jpg'
import DDF from '@/assets/cover/哲学.jpg'

const props = defineProps({
  modelValue: {
    type: String,
    required: true
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const folders = [
  { value: 'ha_ji_mi', label: '基米天堂', img: ha_ji_mi },
  { value: 'dian_gun', label: '溜冰密室', img: dian_gun },
  { value: 'da_si_ma', label: '起飞基地', img: da_si_ma },
  { value: 'ding_zhen', label: '烟雾缭绕', img: ding_zhen },
  { value: 'dxl', label: '东洋雪莲', img: dxl },
  { value: 'DDF', label: '哲学圣地', img: DDF },
]

const currentIndex = computed(() => {
  const idx = folders.findIndex(f => f.value === props.modelValue)
  return idx === -1 ? 0 : idx
})

function getCardStyle(index) {
  const total = folders.length
  // Calculate circular distance
  let diff = (index - currentIndex.value + total) % total
  if (diff > total / 2) diff -= total
  
  const absDiff = Math.abs(diff)
  
  // Only show center and immediate neighbors (3 items total)
  if (absDiff > 1) {
    return {
      opacity: 0,
      transform: 'translateX(0) scale(0.5)',
      zIndex: 0,
      pointerEvents: 'none',
      visibility: 'hidden' // Ensure it doesn't block clicks
    }
  }

  // Visual parameters
  const xOffset = diff * 70 // 70% offset for neighbors
  const scale = diff === 0 ? 1 : 0.8
  const zIndex = 10 - absDiff
  const opacity = diff === 0 ? 1 : 0.6
  // Add slight rotation for 3D effect: left rotates right, right rotates left
  const rotateY = diff === 0 ? 0 : (diff > 0 ? -25 : 25)

  return {
    transform: `translateX(${xOffset}%) scale(${scale}) perspective(1000px) rotateY(${rotateY}deg)`,
    zIndex,
    opacity,
    visibility: 'visible'
  }
}

function selectFolder(val) {
  if (props.modelValue !== val) {
    emit('update:modelValue', val)
    emit('change', val)
  }
}
</script>

<style scoped>
.folder-selector-container {
  width: 100%;
  margin-bottom: 20px;
  padding: 20px 0;
  position: relative;
  display: flex;
  justify-content: center;
  overflow: hidden;
}

.carousel-stage {
  position: relative;
  width: 100%;
  max-width: 600px; /* Constrain width to keep cards centered */
  height: 220px; /* Fixed height for the carousel */
  display: flex;
  justify-content: center;
  align-items: center;
}

.folder-card {
  position: absolute;
  width: 240px; /* Fixed width for calculation consistency */
  cursor: pointer;
  transition: all 0.5s cubic-bezier(0.25, 0.8, 0.25, 1);
  transform-style: preserve-3d;
  /* Center the card initially */
  left: 50%;
  margin-left: -120px; /* Half of width */
}

.card-inner {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  background: var(--playlist-item-bg);
  box-shadow: 0 10px 20px rgba(0,0,0,0.15);
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.folder-card.active .card-inner {
  border-color: var(--primary-color);
  box-shadow: 0 15px 30px rgba(var(--primary-color-rgb), 0.4);
}

.image-container {
  width: 100%;
  aspect-ratio: 1146 / 716;
  position: relative;
  overflow: hidden;
}

.image-container img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.card-info {
  margin-top: 12px;
  text-align: center;
  opacity: 0;
  transform: translateY(-10px);
  transition: all 0.3s ease;
}

.folder-card.active .card-info,
.folder-card:hover .card-info {
  opacity: 1;
  transform: translateY(0);
}

.folder-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color);
  background: rgba(255, 255, 255, 0.5);
  padding: 4px 12px;
  border-radius: 20px;
  backdrop-filter: blur(4px);
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

/* Active State Styling */
.active-indicator {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  animation: fadeIn 0.5s forwards;
}

.playing-icon {
  display: flex;
  gap: 4px;
  height: 24px;
  align-items: flex-end;
}

.playing-icon span {
  width: 5px;
  background-color: #fff;
  border-radius: 2px;
  animation: bounce 1s infinite ease-in-out;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.playing-icon span:nth-child(1) { animation-delay: 0s; height: 60%; }
.playing-icon span:nth-child(2) { animation-delay: 0.2s; height: 100%; }
.playing-icon span:nth-child(3) { animation-delay: 0.4s; height: 50%; }

@keyframes bounce {
  0%, 100% { transform: scaleY(0.5); }
  50% { transform: scaleY(1); }
}

@keyframes fadeIn {
  to { opacity: 1; }
}

@media (prefers-color-scheme: dark) {
  .card-info {
    background: rgba(40, 40, 40, 0.95);
  }
  .folder-name {
    color: #eee;
  }
}
</style>
