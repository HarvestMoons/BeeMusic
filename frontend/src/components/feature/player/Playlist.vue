<template>
  <div class="playlist-container">
    <div class="header">
      <h3>当前歌单</h3>
      <SearchBar placeholder="搜索歌曲..." @search="handleSearch"/>
    </div>

    <div class="sort-toolbar">
      <label for="playlist-sort-field" class="sr-only">歌单排序方式</label>
      <select id="playlist-sort-field" v-model="sortField" class="sort-select">
        <option value="default">默认排序</option>
        <option value="playCount">播放次数</option>
        <option value="likeCount">点赞数</option>
        <option value="dislikeCount">点踩数</option>
        <option value="createdAt">上架日期</option>
      </select>
      <button
          class="sort-btn"
          @click="toggleSortOrder"
          :title="sortOrder === 'asc' ? '升序' : '降序'"
          :aria-label="sortOrder === 'asc' ? '当前升序，点击切换为降序' : '当前降序，点击切换为升序'"
      >
        <img v-if="sortOrder === 'asc'" :src="upArrowIcon" alt="升序" class="svg-icon"/>
        <img v-else :src="downArrowIcon" alt="降序" class="svg-icon"/>
      </button>
    </div>

    <ul class="playlist">
      <li
          v-for="song in displayPlaylist"
          :key="song.id"
          :class="{ active: song.id === currentSongId, 'deleted-song': song.isDeleted === 1 }"
          @click="$emit('select', song.id)"
      >
        <div class="song-item-content">
          <span class="song-title-text" :title="getSongTitle(song.name)">{{ getSongTitle(song.name) }}</span>
          <span v-if="sortField !== 'default'" class="song-meta">{{ formatMeta(song) }}</span>
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup>
import {computed, ref, watch} from 'vue'
import SearchBar from "@/components/common/SearchBar.vue";
import upArrowIcon from '@/assets/icons/up_arrow.svg'
import downArrowIcon from '@/assets/icons/down_arrow.svg'
import {usePlayerStorage} from '@/composables/player/usePlayerStorage.js'
import {getSongTitle, sortPlaylist} from '@/utils/playerPlaylist.js'

const props = defineProps({
  playlist: {type: Array, required: true},
  currentSongId: {type: [String, Number], default: null}
})

defineEmits(['select'])

const {
  loadPlaylistSortPreferences,
  savePlaylistSortField,
  savePlaylistSortOrder
} = usePlayerStorage()

const savedSortPreferences = loadPlaylistSortPreferences()

const searchQuery = ref('')
const sortField = ref(savedSortPreferences.field)
const sortOrder = ref(savedSortPreferences.order)


watch(sortField, (newVal) => {
  savePlaylistSortField(newVal)
})

watch(sortOrder, (newVal) => {
  savePlaylistSortOrder(newVal)
})

const handleSearch = (keyword) => {
  searchQuery.value = keyword
}

const filteredPlaylist = computed(() => {
  if (!searchQuery.value) return props.playlist
  const query = searchQuery.value.toLowerCase()
  return props.playlist.filter(song =>
      getSongTitle(song.name).toLowerCase().includes(query)
  )
})

const displayPlaylist = computed(() => {
  return sortPlaylist(filteredPlaylist.value, {
    field: sortField.value,
    order: sortOrder.value
  })
})

function toggleSortOrder() {
  sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
}

function formatMeta(song) {
  if (sortField.value === 'playCount') return `${song.playCount || 0}次`
  if (sortField.value === 'likeCount') return `${song.likeCount || 0}赞`
  if (sortField.value === 'dislikeCount') return `${song.dislikeCount || 0}踩`
  if (sortField.value === 'createdAt') {
    if (!song.createdAt) return ''
    return new Date(song.createdAt).toLocaleDateString()
  }
  return ''
}
</script>

<style scoped>
.playlist-container {
  width: 320px;
  height: var(--content-height, 750px);
  background: var(--playlist-bg);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  padding: 16px;
  box-sizing: border-box;
  font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
  transition: background-color 0.3s ease;
  display: flex;
  flex-direction: column;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header h3 {
  margin: 0;
  font-size: 18px;
  color: var(--text-color);
  font-weight: 600;
}

.sort-toolbar {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  margin-bottom: 8px;
}

.sort-select {
  flex: 1;
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid var(--border-color, rgba(128, 128, 128, 0.3));
  background-color: var(--playlist-item-bg);
  color: var(--text-color);
  font-size: 13px;
  outline: none;
  cursor: pointer;
  transition: border-color 0.2s;
}

.sort-select:hover, .sort-select:focus {
  border-color: var(--primary-color, #4a90e2);
}

.sort-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid var(--border-color, rgba(128, 128, 128, 0.3));
  background-color: var(--playlist-item-bg);
  color: var(--text-color);
  cursor: pointer;
  transition: background-color 0.2s;
}

.sort-btn:hover {
  background-color: var(--playlist-item-hover-bg);
}

.playlist {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0;
  margin: 4px 0 0 0;
}

.playlist li {
  list-style: none;
  padding: 10px 12px;
  margin-bottom: 6px;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s ease;
  color: var(--playlist-item-text);
  background: var(--playlist-item-bg);
  box-shadow: inset 0 0 0 rgba(0, 0, 0, 0);
}

.playlist li:hover {
  background: var(--playlist-item-hover-bg);
  color: var(--playlist-item-hover-text);
  transform: translateX(4px);
}

.playlist li.active {
  background: var(--playlist-active-bg);
  color: var(--playlist-active-text);
  font-weight: 600;
  box-shadow: var(--playlist-active-shadow);
}

.deleted-song {
  opacity: 0.6;
  background-color: #f0f0f0;
}

.deleted-song .song-title-text {
  text-decoration: line-through;
  color: #999;
}

.song-item-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.song-title-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.song-meta {
  font-size: 12px;
  opacity: 0.7;
  margin-left: 8px;
  white-space: nowrap;
  font-weight: normal;
}

.admin-controls {
  display: flex;
  gap: 8px;
  margin-left: 10px;
}

.icon-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.2em;
  padding: 2px;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.icon-btn:hover {
  opacity: 1;
}

.restore-btn {
  color: #4caf50;
}

.delete-btn {
  color: #f44336;
}
</style>
