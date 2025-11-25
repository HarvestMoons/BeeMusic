<template>
  <div class="rate-control">
    <label for="rate-slider" class="rate-label">倍速</label>
    <input
        id="rate-slider"
        type="range"
        min="0.5"
        max="2"
        step="0.1"
        v-model.number="rate"
        @input="updateRate"
        class="rate-slider"
    />
    <span class="rate-display" @click="resetRate" title="点击恢复 1x">
      {{ rate.toFixed(1) }}x
    </span>
  </div>
</template>

<script setup>
import {ref, watch, onMounted} from 'vue'

const props = defineProps({
  modelValue: {type: Number, default: 1.0},
})
const emits = defineEmits(['update:modelValue', 'change'])

const rate = ref(props.modelValue)

watch(() => props.modelValue, val => rate.value = val)

function updateRate() {
  emits('update:modelValue', rate.value)
  emits('change', rate.value)
}

function resetRate() {
  rate.value = 1.0
  updateRate()
}

onMounted(() => {
  emits('change', rate.value)
})
</script>

<style scoped>
.rate-control {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 8px 14px;
  border-radius: 12px;
  background: var(--rate-control-bg);
  border: 1px solid var(--rate-control-border);
  box-shadow: 0 2px 6px rgba(0,0,0,0.05);
  backdrop-filter: blur(6px);
  font-size: 14px;
  transition: all 0.3s ease;
}

.rate-control:hover {
  background: var(--rate-control-hover-bg);
  box-shadow: 0 3px 10px rgba(0,0,0,0.08);
}

.rate-label {
  font-weight: 500;
  color: var(--rate-control-text);
}

.rate-slider {
  width: 130px;
  height: 5px;
  appearance: none;
  background: var(--rate-slider-track);
  border-radius: 5px;
  outline: none;
  cursor: pointer;
  transition: filter 0.2s;
}

.rate-slider:hover {
  filter: brightness(1.1);
}

/* Chrome / Safari 手柄 */
.rate-slider::-webkit-slider-thumb {
  appearance: none;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--rate-slider-thumb);
  border: 1px solid var(--rate-slider-thumb-border);
  box-shadow: 0 1px 3px rgba(0,0,0,0.15);
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.rate-slider::-webkit-slider-thumb:hover {
  transform: scale(1.1);
  box-shadow: 0 2px 6px rgba(0,0,0,0.25);
}

/* Firefox */
.rate-slider::-moz-range-thumb {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--rate-slider-thumb);
  border: 1px solid var(--rate-slider-thumb-border);
}

.rate-display {
  background: var(--rate-display-bg);
  color: var(--rate-display-text);
  font-weight: 600;
  border-radius: 8px;
  padding: 2px 10px;
  box-shadow: 0 2px 5px rgba(0,0,0,0.15);
  cursor: pointer;
  user-select: none;
  transition: all 0.2s ease;
}

.rate-display:hover {
  background: var(--rate-display-hover-bg);
  transform: scale(1.08);
  box-shadow: 0 3px 8px rgba(0,0,0,0.25);
}
</style>
