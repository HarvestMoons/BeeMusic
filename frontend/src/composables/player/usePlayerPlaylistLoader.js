import { PUBLIC_API_BASE } from '@/constants'
import { sortPlaylist } from '@/utils/playerPlaylist.js'

const DEFAULT_SELECTED_FOLDER_STORAGE_ID = 'folder-selector'

export function usePlayerPlaylistLoader({
    playlist,
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

    async function fetchSongList(folder) {
        try {
            const query = new URLSearchParams({ folder }).toString()
            const res = await fetch(`${PUBLIC_API_BASE}/songs/get?${query}`)
            const data = await res.json()
            playlist.value = data || []
            return playlist.value
        } catch (err) {
            playlist.value = []
            return []
        }
    }

    function getFirstSortedSongIndex() {
        const sortedPlaylist = sortPlaylist(playlist.value, loadPlaylistSortPreferences())
        const firstSongId = sortedPlaylist[0]?.id
        const firstSongIndex = playlist.value.findIndex(song => song.id === firstSongId)
        return firstSongIndex === -1 ? 0 : firstSongIndex
    }

    return {
        handleFolderChange,
        setFolder
    }
}