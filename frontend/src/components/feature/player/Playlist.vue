<template>
  <div class="playlist-container">
    <div class="header">
      <h3>{{ isGlobalSearch ? '全站搜索' : '当前歌单' }}</h3>
      <SearchBar placeholder="搜索歌曲..." @search="handleSearch">
        <button type="button" class="search-scope" :aria-pressed="searchEverywhere"
                @click="searchEverywhere = !searchEverywhere">
          当前搜索方式：{{ searchEverywhere ? '全站' : '本歌单' }} ⇄
        </button>
      </SearchBar>
    </div>

    <div class="sort-toolbar">
      <label for="playlist-sort-field" class="sr-only">歌单排序方式</label>
      <select id="playlist-sort-field" v-model="sortField" class="sort-select">
        <option value="createdAt">上架日期</option>
        <option value="playCount">播放次数</option>
        <option value="likeCount">点赞数</option>
        <option value="dislikeCount">点踩数</option>
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

    <p v-if="isGlobalSearch && globalLoading" class="search-message">正在搜索…</p>
    <p v-else-if="isGlobalSearch && globalError" class="search-message">{{ globalError }}</p>
    <ul v-else class="playlist">
      <li
          v-for="song in displayPlaylist"
          :key="song.id"
          :class="{ active: song.id === currentSongId, 'deleted-song': song.isDeleted === 1 }"
          @click="selectSong(song)"
      >
        <div class="song-item-content">
          <span class="song-title-text" :title="getSongTitle(song.name)">
            {{ getSongTitle(song.name) }}
            <small v-if="isGlobalSearch"> · {{ FOLDER_INFO[song.folderKey] }}</small>
          </span>
          <span class="song-meta">{{ formatMeta(song) }}</span>
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
import {FOLDER_INFO} from '@/constants'
import {useAuthStore} from '@/store'
import api from '@/services/http'

const props = defineProps({
  playlist: {type: Array, required: true},
  currentSongId: {type: [String, Number], default: null}
})

const emit = defineEmits(['select', 'select-global'])
const authStore = useAuthStore()

const {
  loadPlaylistSortPreferences,
  savePlaylistSortField,
  savePlaylistSortOrder
} = usePlayerStorage()

const savedSortPreferences = loadPlaylistSortPreferences()

const searchQuery = ref('')
const searchEverywhere = ref(false)
const globalSongs = ref([])
const globalLoading = ref(false)
const globalError = ref('')
const isGlobalSearch = computed(() => searchEverywhere.value && !!searchQuery.value)
const sortField = ref(savedSortPreferences.field)
const sortOrder = ref(savedSortPreferences.order)

watch(() => [searchEverywhere.value, authStore.isHiddenPlaylistUnlocked, authStore.role, authStore.username],
    async ([enabled], _, onCleanup) => {
      globalSongs.value = []
      globalError.value = ''
      globalLoading.value = enabled
      if (!enabled) return
      const controller = new AbortController()
      onCleanup(() => controller.abort())
      try {
        const folders = Object.keys(FOLDER_INFO).filter(folder => folder !== 'true_music' || authStore.isHiddenPlaylistUnlocked)
        const results = await Promise.all(folders.map(async folderKey => {
          const {data} = await api.get('/public/songs/get', {params: {folder: folderKey}, signal: controller.signal})
          if (!Array.isArray(data)) throw new Error('服务器返回的歌单格式不正确')
          return data.map(song => ({...song, folderKey}))
        }))
        if (!controller.signal.aborted) globalSongs.value = results.flat()
      } catch (error) {
        if (!controller.signal.aborted) globalError.value = '全站搜索加载失败，请切换搜索方式后重试。'
      } finally {
        if (!controller.signal.aborted) globalLoading.value = false
      }
    })

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
  const query = String(searchQuery.value ?? '').toLowerCase()
  const songs = isGlobalSearch.value ? globalSongs.value : props.playlist
  return songs.filter(song =>
      String(getSongTitle(song.name) ?? '').toLowerCase().includes(query)
  )
})

function selectSong(song) {
  if (isGlobalSearch.value) emit('select-global', {songId: song.id, folder: song.folderKey})
  else emit('select', song.id)
}

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

.search-scope {
  width: 100%;
  margin-top: 6px;
  padding: 5px 0;
  border: 0;
  background: transparent;
  color: var(--secondary-text-color);
  font-size: 12px;
  cursor: pointer;
}

.search-scope:hover { color: var(--primary-color); }
.search-message { color: var(--secondary-text-color); font-size: 13px; }
.song-title-text small { font-weight: normal; opacity: .7; }

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
