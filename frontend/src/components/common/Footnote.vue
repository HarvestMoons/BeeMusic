<template>
  <span class="footnote-wrapper" @mouseenter="showPayload = true" @mouseleave="showPayload = false" @click="toggle">
    <sup class="footnote-ref">[{{ index }}]</sup>
    <transition name="fade">
      <span v-if="showPayload" class="footnote-content">
        <slot></slot>
      </span>
    </transition>
  </span>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  index: {
    type: [Number, String],
    required: true
  }
})

const showPayload = ref(false)

function toggle() {
  showPayload.value = !showPayload.value
}
</script>

<style scoped>
.footnote-wrapper {
  position: relative;
  display: inline;
  cursor: pointer;
}

.footnote-ref {
  color: var(--primary-color);
  font-weight: bold;
  font-size: 0.75em;
  transition: color 0.2s;
  vertical-align: super;
  margin: 0 1px; /* Slight margin for separation, but very tight */
}

.footnote-ref:hover {
  text-decoration: underline;
}

.footnote-content {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%) translateY(5px);
  
  width: max-content;
  max-width: 250px;
  padding: 8px 12px;
  
  background: var(--playlist-bg, #ffffff);
  color: var(--playlist-item-text, #333333);
  border: 1px solid var(--border-color, #ccc);
  
  border-radius: 6px;
  font-size: 13px;
  line-height: 1.5;
  text-align: left;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  pointer-events: none;
}
</style>
