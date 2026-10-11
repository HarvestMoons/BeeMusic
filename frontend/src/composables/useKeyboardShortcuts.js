let handler = null

export function useKeyboardShortcuts(
    getAudio,
    playPause,
    next,
    prev,
    toggleMute,
    random = null
    ) {
    // 清理旧的监听器（防止重复绑定）
    if (handler) {
        window.removeEventListener('keydown', handler)
    }
    handler = (e) => {

        if (['INPUT', 'TEXTAREA'].includes(document.activeElement.tagName)) return
        if (document.activeElement.isContentEditable) return
        if (e.repeat) return

        const audio = typeof getAudio === 'function' ? getAudio() : getAudio
        if (!audio) return

        switch (e.key.toLowerCase()) {
            case ' ':
                e.preventDefault()
                playPause()
                break
            case 'arrowright':
            case 'd':
                e.preventDefault()
                next()
                break
            case 'arrowleft':
            case 'a':
                e.preventDefault()
                prev()
                break
            case 'm':
                e.preventDefault()
                toggleMute()
                break
            case 'r':
                if (random) {
                    e.preventDefault()
                    random()
                }
                break
        }
    }

    window.addEventListener('keydown', handler)

    // 返回清理函数，调用方需要在组件卸载时调用，否则监听会一直留在 window 上
    return () => {
        window.removeEventListener('keydown', handler)
        handler = null
    }
}