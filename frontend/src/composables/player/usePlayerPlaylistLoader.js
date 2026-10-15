import { PUBLIC_API_BASE } from '../../constants/index.js'
import { sortPlaylist } from '../../utils/playerPlaylist.js'

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
    let songListRequestId = 0
    let folderChangeId = 0
    async function handleFolderChange() {
        saveSelectedFolder(DEFAULT_SELECTED_FOLDER_STORAGE_ID, selectedFolder.value)
        await setFolder(selectedFolder.value)
    }

    async function setFolder(folder, targetSongId = null) {
        const changeId = ++folderChangeId
        try {
            document.body.classList.add('loading')
            resetQueueState()
            const result = await fetchSongList(folder)
            if (changeId !== folderChangeId || !result || result.requestId !== songListRequestId) return
            playlist.value = result.songs

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
            if (changeId === folderChangeId) document.body.classList.remove('loading')
        }
    }

    async function fetchSongList(folder, clearOnError = true) {
        const requestId = ++songListRequestId
        try {
            const query = new URLSearchParams({ folder }).toString()
            const res = await fetch(`${PUBLIC_API_BASE}/songs/get?${query}`)
            if (!res.ok) throw new Error(`加载歌单失败（${res.status}）`)
            const data = await res.json()
            if (!Array.isArray(data)) throw new Error('服务器返回的歌单格式不正确')
            if (requestId !== songListRequestId) return null
            return { songs: data, requestId }
        } catch (err) {
            if (requestId !== songListRequestId) return null
            showToastMessage(err.message || '加载歌单失败')
            if (clearOnError) {
                playlist.value = []
                currentIndex.value = -1
                currentSongInfo.value = { title: '', bv: null }
                if (audioRef.value) {
                    audioRef.value.pause()
                    audioRef.value.src = ''
                }
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
        // clearOnError = false：刷新失败时保留原歌单，避免一次网络抖动把歌单清空
        const result = await fetchSongList(selectedFolder.value, false)

        if (!result || result.requestId !== songListRequestId) return
        const currentSongId = playlist.value[currentIndex.value]?.id
        const freshPlaylist = result.songs
        playlist.value = freshPlaylist
        if (currentSongId == null) return

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
