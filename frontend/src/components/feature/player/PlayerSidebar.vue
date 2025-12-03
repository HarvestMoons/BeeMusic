<template>
  <div class="sidebar-wrapper" :class="{ collapsed: !showComments }">
    <CommentDrawer
      :visible="showComments"
      :songId="songId"
      @close="handleDrawerClose"
      class="comment-drawer-flex"
    />
    <SpectrumVisualizer :visible="showSpectrum" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import CommentDrawer from '@/components/feature/player/CommentDrawer.vue'
import SpectrumVisualizer from '@/components/feature/spectrum/SpectrumVisualizer.vue'

const props = defineProps({
  songId: {
    type: [Number, String],
    default: null
  },
  showCommentsStorageKey: {
    type: String,
    default: 'music_show_comments'
  }
})

const showComments = ref(true)
const showSpectrum = ref(false)

function persistShowComments() {
  try {
    localStorage.setItem(props.showCommentsStorageKey, String(showComments.value))
  } catch (e) {}
}

function setCommentsVisibility(next) {
  if (showComments.value === next) return
  showComments.value = next
  if (!next) showSpectrum.value = false
  persistShowComments()
}

function toggleComments() {
  setCommentsVisibility(!showComments.value)
}

function toggleSpectrum() {
  if (showSpectrum.value) {
    showSpectrum.value = false
  } else {
    showSpectrum.value = true
    showComments.value = true
    persistShowComments()
  }
}

function handleDrawerClose() {
  toggleComments()
}

onMounted(() => {
  try {
    const saved = localStorage.getItem(props.showCommentsStorageKey)
    if (saved !== null) showComments.value = saved === 'true'
  } catch (e) {}
})

defineExpose({
  showSpectrum,
  toggleSpectrum,
  toggleComments,
  showComments
})
</script>

<style scoped>
.sidebar-wrapper {
  width: 400px;
  height: var(--content-height);
  display: flex;
  flex-direction: column;
  background: var(--playlist-bg);
  border-left: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.sidebar-wrapper.collapsed {
  width: 60px;
}

.sidebar-wrapper.collapsed :deep(.drawer-header h3) {
  display: none;
}

.sidebar-wrapper.collapsed :deep(.drawer-header) {
  justify-content: center;
  padding: 16px 0;
}

.sidebar-wrapper.collapsed :deep(.spectrum-container) {
  display: none;
}

:deep(.spectrum-container) {
  border-top: 1px solid var(--border-color);
}

.comment-drawer-flex {
  flex: 1;
  min-height: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
}

:deep(.comment-sidebar) {
  height: 100%;
  display: flex;
  flex-direction: column;
  width: 100%;
  background: transparent;
  box-shadow: none;
  transform: none !important;
}

:deep(.comment-sidebar.collapsed .drawer-header) {
  border-bottom: none;
}

:deep(.comment-content-wrapper) {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

:deep(.comment-list) {
  flex: 1;
  overflow-y: auto;
}
</style>
