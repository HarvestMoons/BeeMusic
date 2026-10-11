import { PUBLIC_API_BASE } from '@/constants'
import { sortPlaylist } from '@/utils/playerPlaylist.js'

const DEFAULT_SELECTED_FOLDER_STORAGE_ID = 'folder-selector'

export function usePlayerPlaylistLoader({
    playlist,
    currentIndex,
    selectedFolder,
    playbackRate,
    audioRef,
    currentSongInfo,
    showToastMessage,
    saveSelectedFolder,
    loadPlaybackRateForFolder,
    loadPlaylistSortPreferences,
    resetQueueState,
    playSongAtIndex
}) {
    async function handleFolderChange() {
        saveSelectedFolder(DEFAULT_SELECTED_FOLDER_STORAGE_ID, selectedFolder.value)
        await setFolder(selectedFolder.value)
    }

    async function setFolder(folder, targetSongId = null) {
        try {
            document.body.classList.add('loading')
            resetQueueState()
            await fetchSongList(folder)

            const savedRate = loadPlaybackRateForFolder(folder)
            if (savedRate != null) {
                playbackRate.value = savedRate
                if (audioRef.value) audioRef.value.playbackRate = savedRate
            }

            if (playlist.value.length > 0) {
                let playIndex = getFirstSortedSongIndex()
                if (targetSongId) {
                    const foundIndex = playlist.value.findIndex(s => s.id === targetSongId)
                    if (foundIndex !== -1) playIndex = foundIndex
                }
                playSongAtIndex(playIndex)
            } else {
                currentSongInfo.value = { title: '', bv: null }
                if (audioRef.value) audioRef.value.src = ''
            }
        } catch (err) {
            console.error('切换音乐文件夹失败', err)
            showToastMessage('请求失败: ' + err.message)
        } finally {
            document.body.classList.remove('loading')
        }
    }

    async function fetchSongList(folder, clearOnError = true) {
        try {
            const query = new URLSearchParams({ folder }).toString()
            const res = await fetch(`${PUBLIC_API_BASE}/songs/get?${query}`)
            const data = await res.json()
            playlist.value = data || []
            return playlist.value
        } catch (err) {
            if (clearOnError) {
                playlist.value = []
            }
            return null
        }
    }

    function getFirstSortedSongIndex() {
        const sortedPlaylist = sortPlaylist(playlist.value, loadPlaylistSortPreferences())
        const firstSongId = sortedPlaylist[0]?.id
        const firstSongIndex = playlist.value.findIndex(song => song.id === firstSongId)
        return firstSongIndex === -1 ? 0 : firstSongIndex
    }

    /**
     * 重新拉取当前文件夹的歌单，用于站长同步结束后刷新内存中的列表。
     * 只替换歌曲列表和当前索引，不重置队列、不重新开始播放。
     */
    async function refreshSongList() {
        const currentSongId = playlist.value[currentIndex.value]?.id
        // clearOnError = false：刷新失败时保留原歌单，避免一次网络抖动把歌单清空
        const freshPlaylist = await fetchSongList(selectedFolder.value, false)

        if (!freshPlaylist || currentSongId == null) {
            return
        }

        const newIndex = freshPlaylist.findIndex(song => song.id === currentSongId)
        if (newIndex === -1) {
            // 正在播放的歌曲已被清理，停止播放并提示
            currentIndex.value = -1
            currentSongInfo.value = { title: '', bv: null }
            if (audioRef.value) {
                audioRef.value.pause()
                audioRef.value.src = ''
            }
            showToastMessage('当前播放的歌曲已被清理')
            return
        }
        currentIndex.value = newIndex
    }

    return {
        handleFolderChange,
        setFolder,
        refreshSongList
    }
}