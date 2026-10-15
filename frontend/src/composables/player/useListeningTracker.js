import {onMounted, onUnmounted, watch} from 'vue'
import api from '@/services/http'
import {eventBus} from '@/utils/eventBus'

const REPORT_INTERVAL_MS = 15000
const SAMPLE_INTERVAL_MS = 1000

export function useListeningTracker(audioRef, getSong, authStore) {
    let current = null
    let running = false
    let lastTime = 0
    let lastPosition = 0
    let lastRate = 1
    let timer = null
    let removeListeners = () => {}
    let pendingFinish = Promise.resolve()

    function baseline() {
        lastTime = performance.now()
        lastPosition = (audioRef.value?.currentTime || 0) * 1000
        lastRate = audioRef.value?.playbackRate || 1
    }

    function sample() {
        const audio = audioRef.value
        if (!audio) return
        const now = performance.now()
        const position = audio.currentTime * 1000
        const elapsed = now - lastTime
        const advance = position - lastPosition
        // 仅累计连续播放的实际时间，拖动、缓冲、暂停不计时。
        if (current && running && elapsed > 0 && advance > 0 &&
                advance <= elapsed * lastRate + 350 && !audio.seeking) {
            current.listenedMs += Math.min(elapsed, advance / lastRate)
        }
        baseline()
    }

    function begin() {
        const song = getSong()
        if (current || !authStore.isAuthenticated || !song?.id) return
        const state = {
            id: crypto.randomUUID(), songId: song.id, owner: authStore.username,
            listenedMs: 0, sequence: 0, lastSent: performance.now(),
        }
        current = state
        state.created = pendingFinish.then(() => {
            if (!authStore.isAuthenticated || authStore.username !== state.owner) return null
            return api.post('/listening/sessions', {playbackId: state.id, songId: song.id})
        })
            .then(response => {
                const active = response?.data.active === true
                if (!active && current === state) current = null
                return active
            }).catch(error => {
                if (current === state) current = null
                console.error('开始记录听歌时长失败', error)
                return false
            })
    }

    function send(state, closed = false) {
        if (!state) return Promise.resolve()
        const report = {
            sequence: ++state.sequence, listenedMs: Math.round(state.listenedMs), closed,
        }
        state.lastSent = performance.now()
        return state.created.then(async created => {
            if (!created || !authStore.isAuthenticated || authStore.username !== state.owner) return
            try {
                const response = await api.post(`/listening/sessions/${state.id}`, report,
                    {adapter: 'fetch', fetchOptions: {keepalive: true}})
                if (response.data.active === false && current === state) current = null
                eventBus.emit('listening-updated')
            } catch (error) {
                // 下一次心跳继续携带累计值和更大序号；重发和乱序不会重复累计。
                console.error('保存听歌记录失败', error)
            }
        })
    }

    function finish() {
        sample()
        if (current) pendingFinish = send(current, true)
        current = null
        running = false
        baseline()
    }

    watch(() => [authStore.isAuthenticated, authStore.username], () => {
        current = null
        baseline()
        if (audioRef.value && !audioRef.value.paused && authStore.isAuthenticated) begin()
    })

    onMounted(() => {
        const audio = audioRef.value
        const play = () => {
            baseline()
            if (!current) { begin(); return }
            const state = current
            state.created.then(async created => {
                if (!created || current !== state || !authStore.isAuthenticated || authStore.username !== state.owner) return
                try {
                    const response = await api.post('/listening/sessions', {playbackId: state.id, songId: state.songId})
                    if (response.data.active === false && current === state && authStore.username === state.owner &&
                            !audio.paused && getSong()?.id === state.songId && authStore.isAuthenticated) {
                        current = null
                        begin()
                        baseline()
                    }
                } catch (error) {
                    console.error('恢复听歌记录失败', error)
                }
            })
        }
        const playing = () => { sample(); running = true; begin(); baseline() }
        const pause = () => { sample(); running = false; send(current) }
        const seeking = () => { sample(); running = false }
        const seeked = () => { baseline(); running = !audio.paused && audio.readyState >= 3 }
        const pagehide = () => { sample(); send(current) }
        const listeners = {play, playing, pause, waiting: pause, seeking, seeked, ratechange: sample, timeupdate: sample}
        for (const [name, handler] of Object.entries(listeners)) audio.addEventListener(name, handler)
        window.addEventListener('pagehide', pagehide)
        removeListeners = () => {
            for (const [name, handler] of Object.entries(listeners)) audio.removeEventListener(name, handler)
            window.removeEventListener('pagehide', pagehide)
        }
        baseline()
        timer = setInterval(() => {
            sample()
            if (current && performance.now() - current.lastSent >= REPORT_INTERVAL_MS) send(current)
        }, SAMPLE_INTERVAL_MS)
    })

    onUnmounted(() => {
        finish()
        clearInterval(timer)
        removeListeners()
    })

    return {finish}
}
