<template>
  <teleport to="body">
    <div v-if="visible" class="modal-overlay" @click.self="cancel" role="dialog" aria-modal="true">
      <div class="modal-card" @keydown.esc="cancel">
        <h2 class="modal-title">{{ title }}</h2>
        <p class="modal-message">{{ message }}</p>

        <div class="modal-actions">
          <button class="secondary-btn" @click="cancel">{{ cancelText }}</button>
          <button class="primary-btn" @click="confirm">{{ confirmText }}</button>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup>
const props = defineProps({
  visible: {type: Boolean, default: false},
  title: {type: String, default: '确认'},
  message: {type: String, default: ''},
  confirmText: {type: String, default: '确定'},
  cancelText: {type: String, default: '取消'}
})

const emit = defineEmits(['update:visible', 'confirm', 'cancel'])

function cancel() {
  emit('update:visible', false)
  emit('cancel')
}

function confirm() {
  emit('update:visible', false)
  emit('confirm')
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  z-index: 3000;
}

.modal-card {
  width: 320px;
  max-width: 90vw;
  background: var(--modal-bg);
  color: var(--modal-text);
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  border: 1px solid var(--border-color);
  animation: pop 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  text-align: center;
}

@keyframes pop {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.modal-title {
  margin: 0 0 12px 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--modal-text);
}

.modal-message {
  margin: 0 0 24px 0;
  font-size: 15px;
  color: var(--text-color);
  line-height: 1.5;
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

button {
  flex: 1;
  padding: 10px;
  border-radius: 8px;
  border: none;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: filter 0.2s;
}

.primary-btn {
  background: var(--primary-color);
  color: #fff;
}

.secondary-btn {
  background: var(--playlist-item-bg);
  color: var(--text-color);
  border: 1px solid var(--border-color);
}

button:hover {
  filter: brightness(1.1);
}
</style>
