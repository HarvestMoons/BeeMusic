<template>
  <div class="search-bar">
    <!-- 搜索图标 -->
    <img
        src="@/assets/icons/search.svg"
        alt="search"
        class="search-icon"
        :class="{ active: showInput }"
        @click="toggleSearch"
    />

    <!-- 输入框，出现在图标下方 -->
    <transition name="fade">
      <input
          v-if="showInput"
          type="text"
          v-model="query"
          @input="onSearch"
          :placeholder="placeholder"
          class="search-input"
      />
    </transition>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  placeholder: {
    type: String,
    default: '搜索...'   // 默认占位符
  }
})

const query = ref('')
const showInput = ref(false)
const emit = defineEmits(['search'])

const toggleSearch = () => {
  showInput.value = !showInput.value
}

const onSearch = () => {
  emit('search', query.value.trim())
}
</script>

<style scoped>
.search-bar {
  position: relative;
  display: inline-block;
}

.search-icon {
  width: 20px;
  height: 20px;
  cursor: pointer;
  transition: transform 0.2s ease, filter 0.2s ease;
}

.search-icon.active {
  transform: translateY(-3px) scale(1.1);
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
}

.search-input {
  position: absolute;
  top: 28px;
  left: 0;
  width: 200px;
  border: 1px solid var(--input-border);
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 14px;
  outline: none;
  background: var(--input-bg);
  color: var(--input-text);
  z-index: 10;
}

.search-input:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 4px rgba(249, 168, 37, 0.4);
}

/* 输入框淡入淡出动画 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
  transform: translateY(-5px);
}
</style>
