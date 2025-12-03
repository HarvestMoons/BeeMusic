<template>
  <div class="playlist-container">
    <div class="header">
      <h3>当前歌单</h3>
      <SearchBar placeholder="搜索歌曲..." @search="handleSearch" />
    </div>

    <div class="sort-toolbar">
      <select v-model="sortField" class="sort-select">
        <option value="default">默认排序</option>
        <option value="playCount">播放次数</option>
        <option value="likeCount">点赞数</option>
        <option value="dislikeCount">点踩数</option>
        <option value="createdAt">上架日期</option>
      </select>
      <button class="sort-btn" @click="toggleSortOrder" :title="sortOrder === 'asc' ? '升序' : '降序'">
        <img v-if="sortOrder === 'asc'" :src="upArrowIcon" alt="升序" class="sort-icon" />
        <img v-else :src="downArrowIcon" alt="降序" class="sort-icon" />
      </button>
    </div>

    <ul class="playlist">
      <li
          v-for="song in displayPlaylist"
          :key="song.id"
          :class="{ active: song.id === currentSongId }"
          @click="$emit('select', song.id)"
      >
        <div class="song-item-content">
          <span class="song-title-text">{{ getSongTitle(song.name) }}</span>
          <span v-if="sortField !== 'default'" class="song-meta">{{ formatMeta(song) }}</span>
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import SearchBar from "@/components/common/SearchBar.vue";
import upArrowIcon from '@/assets/icons/up_arrow.svg'
import downArrowIcon from '@/assets/icons/down_arrow.svg'

const props = defineProps({
  playlist: { type: Array, required: true },
  currentSongId: { type: [String, Number], default: null }
})

const searchQuery = ref('')
const sortField = ref('default')
const sortOrder = ref('desc')

// LocalStorage Keys
const STORAGE_KEY_SORT_FIELD = 'music_playlist_sort_field'
const STORAGE_KEY_SORT_ORDER = 'music_playlist_sort_order'

onMounted(() => {
  const savedField = localStorage.getItem(STORAGE_KEY_SORT_FIELD)
  const savedOrder = localStorage.getItem(STORAGE_KEY_SORT_ORDER)
  if (savedField) sortField.value = savedField
  if (savedOrder) sortOrder.value = savedOrder
})

watch(sortField, (newVal) => {
  localStorage.setItem(STORAGE_KEY_SORT_FIELD, newVal)
})

watch(sortOrder, (newVal) => {
  localStorage.setItem(STORAGE_KEY_SORT_ORDER, newVal)
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
  let list = [...filteredPlaylist.value]
  if (sortField.value === 'default') return list

  return list.sort((a, b) => {
    let valA = a[sortField.value]
    let valB = b[sortField.value]

    if (sortField.value === 'createdAt') {
      valA = valA ? new Date(valA).getTime() : 0
      valB = valB ? new Date(valB).getTime() : 0
    } else {
      valA = Number(valA) || 0
      valB = Number(valB) || 0
    }

    if (valA === valB) return 0
    const result = valA > valB ? 1 : -1
    return sortOrder.value === 'asc' ? result : -result
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

function getSongTitle(name) {
  let title = name.replace(/\.(mp3)$/i, '')
  const match = title.match(/^(.*?)_?BV[0-9A-Za-z]+/i)
  if (match) title = match[1]
  return title
}
</script>

<style scoped>
.playlist-container {
  width: 320px;
  height: var(--content-height, 750px);
  background: var(--playlist-bg);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  padding: 16px;
  box-sizing: border-box;
  font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
  transition: background-color 0.3s ease;
  display: flex;
  flex-direction: column;
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
  box-shadow: inset 0 0 0 rgba(0,0,0,0);
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

.sort-icon {
  width: 20px;
  height: 20px;
  display: block;
  filter: var(--vote-icon-filter); /* 复用黑夜模式反色滤镜 */
}
</style>
