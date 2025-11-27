<template>
  <Transition name="fade">
    <div v-if="visible" class="toast-notification">
      {{ message }}
    </div>
  </Transition>
</template>

<script setup>
import { defineProps } from 'vue'

defineProps({
  message: {
    type: String,
    default: ''
  },
  visible: {
    type: Boolean,
    default: false
  }
})
</script>

<style scoped>
.toast-notification {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  /* 提高透明度，从 0.75 调整为 0.6 */
  background-color: rgba(0, 0, 0, 0.6);
  color: #fff;
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  pointer-events: none;
  z-index: 100;
  backdrop-filter: blur(4px);
  white-space: nowrap;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
