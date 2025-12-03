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
import { onBeforeUnmount, onMounted, ref, computed } from 'vue'
import Chart from 'chart.js/auto'
import { PUBLIC_API_BASE } from '@/constants'

const canvasRef = ref(null)
const chartInstance = ref(null)
const dataset = ref([])
const isLoading = ref(true)
const error = ref('')
const palette = ['#6C63FF', '#FF6584', '#3EC8AF', '#FFB347', '#20A4F3', '#9C27B0', '#FF8CC6', '#00BFA6']

const totalCount = computed(() => dataset.value.reduce((sum, item) => sum + item.count, 0))

async function fetchCounts() {
  isLoading.value = true
  error.value = ''
  try {
    const res = await fetch(`${PUBLIC_API_BASE}/songs/folder-counts`)
    if (!res.ok) throw new Error('数据加载失败')
    const raw = await res.json()
    dataset.value = raw.map((entry, idx) => ({
      ...entry,
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
  if (chartInstance.value) {
    chartInstance.value.destroy()
  }
  chartInstance.value = new Chart(canvasRef.value, {
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
          backgroundColor: 'rgba(15, 15, 26, 0.9)',
          padding: 12,
          cornerRadius: 10,
          titleFont: { family: 'inherit', size: 13, weight: '600' },
          bodyFont: { family: 'inherit', size: 13 },
          callbacks: {
            label: context => `${context.parsed} 首`
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

onMounted(fetchCounts)

onBeforeUnmount(() => {
  if (chartInstance.value) chartInstance.value.destroy()
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
</style>
