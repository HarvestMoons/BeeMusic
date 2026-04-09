<template>
  <div class="countdown-ring" :style="ringStyle" :title="titleText">
    <svg class="ring-svg" viewBox="0 0 100 100" aria-hidden="true">
      <circle class="ring-track" cx="50" cy="50" :r="radius" />
      <circle
          class="ring-progress"
          cx="50"
          cy="50"
          :r="radius"
          :style="progressStyle"
      />
    </svg>
    <div class="ring-center">
      <span class="ring-label">{{ displayText }}</span>
      <span v-if="subtitle" class="ring-subtitle">{{ subtitle }}</span>
    </div>
  </div>
</template>

<script setup>
import {computed} from 'vue'

const props = defineProps({
  progress: {
    type: Number,
    default: 0
  },
  label: {
    type: [String, Number],
    default: ''
  },
  subtitle: {
    type: String,
    default: ''
  },
  size: {
    type: Number,
    default: 72
  },
  strokeWidth: {
    type: Number,
    default: 8
  }
})

const clampedProgress = computed(() => Math.min(Math.max(props.progress, 0), 1))
const radius = computed(() => 50 - props.strokeWidth)
const circumference = computed(() => 2 * Math.PI * radius.value)

const ringStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`
}))

const progressStyle = computed(() => ({
  strokeWidth: props.strokeWidth,
  strokeDasharray: circumference.value,
  strokeDashoffset: circumference.value * (1 - clampedProgress.value)
}))

const displayText = computed(() => String(props.label))
const titleText = computed(() => props.subtitle ? `${props.label} ${props.subtitle}` : String(props.label))
</script>

<style scoped>
.countdown-ring {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: color-mix(in srgb, var(--playlist-bg) 82%, transparent);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.12);
  border: 1px solid color-mix(in srgb, var(--border-color) 70%, transparent);
  backdrop-filter: blur(10px);
}

.ring-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.ring-track,
.ring-progress {
  fill: none;
  transform-origin: 50% 50%;
}

.ring-track {
  stroke: color-mix(in srgb, var(--border-color) 70%, transparent);
  stroke-width: 8;
}

.ring-progress {
  stroke: var(--primary-color);
  stroke-linecap: round;
  transition: stroke-dashoffset 0.12s linear, stroke 0.2s ease;
}

.ring-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-color);
  user-select: none;
}

.ring-label {
  font-size: 0.95rem;
  font-weight: 700;
  line-height: 1;
}

.ring-subtitle {
  margin-top: 3px;
  font-size: 0.58rem;
  letter-spacing: 0.08em;
  opacity: 0.72;
}
</style>