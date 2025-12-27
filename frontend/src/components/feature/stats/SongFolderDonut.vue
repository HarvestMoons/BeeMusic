<template>
  <section class="folder-chart" aria-labelledby="folder-chart-title">
    <header class="chart-header">
      <div>
        <p class="eyebrow">歌单分布</p>
        <h3 id="folder-chart-title">收录歌曲占比</h3>
      </div>
      <p v-if="totalCount" class="total-count">共 {{ totalCount }} 首</p>
    </header>

    <div class="chart-body">
      <div class="canvas-shell" :class="{ loading: isLoading }">
        <canvas ref="canvasRef" aria-label="歌曲分布环形图"></canvas>
        <div v-if="isLoading" class="loading-glow"></div>

        <!-- Custom Tooltip -->
        <div 
          class="chart-tooltip"
          :style="{ 
            opacity: tooltipModel.opacity, 
            left: tooltipModel.left + 'px', 
            top: tooltipModel.top + 'px' 
          }"
        >
          <div class="tooltip-title">{{ tooltipModel.title }}</div>
          <div v-for="(line, i) in tooltipModel.body" :key="i" class="tooltip-body">
            {{ line }}
          </div>
        </div>
        
        <!-- Easter Egg Component -->
        <HiddenUnlockTrigger 
          :unlocked="authStore.isHiddenPlaylistUnlocked"
          :playlist-name="hiddenPlaylistName"
          @on-unlock="handleUnlockSuccess"
        />
      </div>
      <ul class="legend" role="list">
        <li
          v-for="item in dataset"
          :key="item.folderKey"
          class="legend-item"
        >
          <span class="swatch" :style="{ backgroundColor: item.color }"></span>
          <div>
            <p class="label">{{ item.displayName }}</p>
            <p class="value">{{ item.count }} 首</p>
          </div>
        </li>
      </ul>
    </div>

    <p v-if="error" class="chart-error">{{ error }}</p>
  </section>
</template>

<script setup>
import { onBeforeUnmount, ref, computed, watch } from 'vue'
import Chart from 'chart.js/auto'
import { PUBLIC_API_BASE, FOLDER_INFO } from '@/constants'
import { FOLDER_DESCRIPTIONS } from '@/constants/folderDescriptions'
import { useAuthStore } from '@/store'
import HiddenUnlockTrigger from './HiddenUnlockTrigger.vue'

const palette = ['#6C63FF', '#FF6584', '#3EC8AF', '#FFB347', '#20A4F3', '#9C27B0', '#FF8CC6', '#00BFA6']

const authStore = useAuthStore()
const canvasRef = ref(null)
let chartInstance = null
const dataset = ref([])
const isLoading = ref(true)
const error = ref('')

const hiddenPlaylistName = ref('')

const totalCount = computed(() => dataset.value.reduce((sum, item) => sum + item.count, 0))

// Tooltip State
const tooltipModel = ref({ opacity: 0, left: 0, top: 0, title: '', body: [] })

const externalTooltipHandler = (context) => {
  const { chart, tooltip } = context
  if (tooltip.opacity === 0) {
    tooltipModel.value.opacity = 0
    return
  }

  tooltipModel.value = {
    opacity: 1,
    left: chart.canvas.offsetLeft + tooltip.caretX,
    top: chart.canvas.offsetTop + tooltip.caretY,
    title: tooltip.title[0] || '',
    // 获取 label 回调返回的数据 (lines: [数量, 描述])
    body: tooltip.body[0]?.lines || []
  }
}

function handleUnlockSuccess() {
  authStore.isHiddenPlaylistUnlocked = true
}

async function fetchCounts() {
  isLoading.value = true
  error.value = ''
  try {
    const res = await fetch(`${PUBLIC_API_BASE}/songs/folder-counts`)
    if (!res.ok) throw new Error('数据加载失败')
    let raw = await res.json()

    // 处理隐藏歌单逻辑
    const hiddenIndex = raw.findIndex(item => item.folderKey === 'true_music')
    if (hiddenIndex !== -1) {
      hiddenPlaylistName.value = FOLDER_INFO.true_music || raw[hiddenIndex].displayName
      // 如果未解锁，从数据中移除
      if (!authStore.isHiddenPlaylistUnlocked) {
        raw.splice(hiddenIndex, 1)
      }
    }

    dataset.value = raw.map((entry, idx) => ({
      ...entry,
      displayName: FOLDER_INFO[entry.folderKey] || entry.displayName,
      description: FOLDER_DESCRIPTIONS[entry.folderKey] || '...',
      color: palette[idx % palette.length]
    }))
    drawChart()
  } catch (err) {
    console.error('加载歌单数量失败', err)
    error.value = err.message || '无法获取歌单数据，请稍后再试'
  } finally {
    isLoading.value = false
  }
}

function drawChart() {
  if (!canvasRef.value || dataset.value.length === 0) return
  if (chartInstance) {
    chartInstance.destroy()
  }
  chartInstance = new Chart(canvasRef.value, {
    type: 'doughnut',
    data: {
      labels: dataset.value.map(item => item.displayName),
      datasets: [
        {
          data: dataset.value.map(item => item.count),
          backgroundColor: dataset.value.map(item => item.color),
          borderWidth: 0,
          hoverOffset: 20,
          spacing: 2
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      cutout: '65%',
      layout: {
        padding: 18
      },
      plugins: {
        legend: { display: false },
        tooltip: {
          enabled: false, // 禁用默认 Canvas Tooltip
          external: externalTooltipHandler, // 使用自定义 HTML Tooltip
          callbacks: {
            label: context => {
              const item = dataset.value[context.dataIndex]
              return [`${item.count} 首`, `“${item.description}”`]
            }
          }
        }
      },
      animation: {
        duration: 900,
        easing: 'easeOutCubic'
      }
    }
  })
}

watch(() => authStore.isHiddenPlaylistUnlocked, () => {
  fetchCounts()
}, { immediate: true })

onBeforeUnmount(() => {
  if (chartInstance) chartInstance.destroy()
})
</script>

<style scoped>
.folder-chart {
  background: var(--playlist-bg);
  border-radius: 20px;
  padding: 24px;
  margin: 36px auto;
  border: 1px solid var(--border-color);
  box-shadow: 0 20px 45px rgba(20, 24, 38, 0.08);
  max-width: 880px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
}

.eyebrow {
  font-size: 13px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--text-color, #cbd5f5);
  opacity: 0.65;
  margin-bottom: 4px;
}

.chart-header h3 {
  margin: 0;
}

.total-count {
  font-weight: 600;
  font-size: 16px;
  color: var(--primary-color);
  margin: 0;
}

.chart-body {
  display: grid;
  grid-template-columns: minmax(220px, 260px) 1fr;
  gap: 24px;
  margin-top: 24px;
}

.canvas-shell {
  position: relative;
  width: 100%;
  padding: 0;
  background: transparent;
  border-radius: 50%;
  min-height: 260px;
}

canvas {
  width: 100% !important;
  height: 100% !important;
}

.legend {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 14px;
}

.legend-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  transition: transform 0.25s ease, border-color 0.25s ease;
}

.legend-item:hover {
  transform: translateY(-4px);
  border-color: rgba(255, 255, 255, 0.2);
}

.swatch {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  box-shadow: 0 0 12px rgba(0, 0, 0, 0.25);
  align-self: flex-start;
}

.label {
  margin: 0;
  font-weight: 600;
}

.value {
  margin: 2px 0 0;
  font-size: 13px;
  opacity: 0.75;
}

.chart-error {
  margin-top: 16px;
  color: #ff8a8a;
  font-size: 14px;
}

@keyframes shimmer {
  from {
    transform: translateX(-20%);
  }
  to {
    transform: translateX(120%);
  }
}

@media (max-width: 960px) {
  .chart-body {
    grid-template-columns: 1fr;
  }
  .legend {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  }
}

/* Tooltip Styles */
.chart-tooltip {
  position: absolute;
  background: var(--playlist-item-bg);
  backdrop-filter: blur(8px);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 12px;
  pointer-events: none;
  transform: translate(-50%, -100%);
  transition: opacity 0.2s ease, left 0.1s ease, top 0.1s ease;
  z-index: 100;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  min-width: 200px;
  max-width: 600px;
  width: max-content;
}

.tooltip-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-color);
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-color);
}

.tooltip-body {
  font-size: 13px;
  color: var(--text-color);
  opacity: 0.85;
  line-height: 1.5;
  white-space: pre-wrap;
}
</style>
