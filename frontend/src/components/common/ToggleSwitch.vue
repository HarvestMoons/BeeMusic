<template>
  <div
      class="toggle-switch"
      :class="{ 'is-checked': modelValue, 'is-disabled': disabled }"
      @click="toggle"
      role="switch"
      :aria-checked="modelValue"
  >
    <div class="toggle-track"></div>
    <div class="toggle-thumb"></div>
  </div>
</template>

<script setup>

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

function toggle() {
  if (props.disabled) return
  const newValue = !props.modelValue
  emit('update:modelValue', newValue)
  emit('change', newValue)
}
</script>

<style scoped>
.toggle-switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 22px;
  cursor: pointer;
  vertical-align: middle;
  transition: opacity 0.3s;
  user-select: none;
}

.toggle-switch.is-disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.toggle-track {
  width: 100%;
  height: 100%;
  border-radius: 999px;
  background-color: #d1d5db; /* Gray-300 */
  transition: background-color 0.2s ease-in-out;
}

.toggle-switch.is-checked .toggle-track {
  background-color: var(--primary-color, #f9a825);
}

.toggle-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  background-color: #ffffff;
  border-radius: 50%;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  transition: transform 0.2s ease-in-out;
}

.toggle-switch.is-checked .toggle-thumb {
  transform: translateX(22px);
}

[data-theme="dark"] .toggle-track {
  background-color: #4b5563; /* Gray-600 */
}
</style>