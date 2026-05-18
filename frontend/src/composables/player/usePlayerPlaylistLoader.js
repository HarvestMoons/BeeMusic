import { PUBLIC_API_BASE } from '@/constants'

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
                let playIndex = 0
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
            playlist.value = shuffleArray(data || [])
            return playlist.value
        } catch (err) {
            playlist.value = []
            return []
        }
    }

    function shuffleArray(array) {
        const newArr = [...array]
        for (let i = newArr.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1))
                ;[newArr[i], newArr[j]] = [newArr[j], newArr[i]]
        }
        return newArr
    }

    return {
        handleFolderChange,
        setFolder
    }
}