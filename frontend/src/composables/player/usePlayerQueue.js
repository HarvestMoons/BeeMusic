import { ref } from 'vue'
import { sortPlaylist } from '@/utils/playerPlaylist.js'

export function usePlayerQueue({ playlist, playMode, playSongAtIndex, showToastMessage, loadPlaylistSortPreferences }) {
    const currentIndex = ref(-1)
    const historyStack = ref([])

    function resetQueueState() {
        historyStack.value = []
    }

    function getCurrentSortedPlaylist() {
        return sortPlaylist(playlist.value, loadPlaylistSortPreferences())
    }

    function findPlaylistIndexBySongId(songId) {
        return playlist.value.findIndex(song => song.id === songId)
    }

    function playPreviousSong() {
        if (historyStack.value.length === 0) {
            showToastMessage('没有上一首了')
            return
        }

        const prev = historyStack.value.pop()
        playSongAtIndex(prev, true)
    }

    function playRandomSong() {
        const rand = Math.floor(Math.random() * playlist.value.length)
        playSongAtIndex(rand)
    }

    function handleSelectSong(songId) {
        const idx = playlist.value.findIndex(s => s.id === songId)
        playSongAtIndex(idx)
    }

    function playNextInOrder() {
        if (playlist.value.length === 0) return

        const orderedPlaylist = getCurrentSortedPlaylist()
        const currentSongId = playlist.value[currentIndex.value]?.id
        const orderedCurrentIndex = orderedPlaylist.findIndex(song => song.id === currentSongId)
        const nextOrderedIndex = orderedCurrentIndex === -1
            ? 0
            : (orderedCurrentIndex + 1) % orderedPlaylist.length
        const nextSongId = orderedPlaylist[nextOrderedIndex]?.id
        const nextIndex = findPlaylistIndexBySongId(nextSongId)

        if (nextIndex !== -1) {
            playSongAtIndex(nextIndex)
        }
    }

    function playPrevInOrder() {
        if (playlist.value.length === 0 || currentIndex.value === -1) return

        const orderedPlaylist = getCurrentSortedPlaylist()
        const currentSongId = playlist.value[currentIndex.value]?.id
        const orderedCurrentIndex = orderedPlaylist.findIndex(song => song.id === currentSongId)
        const prevOrderedIndex = orderedCurrentIndex === -1
            ? orderedPlaylist.length - 1
            : (orderedCurrentIndex - 1 + orderedPlaylist.length) % orderedPlaylist.length
        const prevSongId = orderedPlaylist[prevOrderedIndex]?.id
        const prevIndex = findPlaylistIndexBySongId(prevSongId)

        if (prevIndex !== -1) {
            playSongAtIndex(prevIndex)
        }
    }

    function handlePlaybackEnd() {
        if (playMode.value === 'single-loop') {
            playSongAtIndex(currentIndex.value, false)
        } else if (playMode.value === 'loop-list') {
            playNextInOrder()
        } else if (playMode.value === 'random') {
            playRandomSong()
        }
    }

    return {
        currentIndex,
        historyStack,
        resetQueueState,
        playPreviousSong,
        playRandomSong,
        handleSelectSong,
        playNextInOrder,
        playPrevInOrder,
        handlePlaybackEnd
    }
}