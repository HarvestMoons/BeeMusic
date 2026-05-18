export function getSharedSongParams(search = '') {
    const urlParams = new URLSearchParams(search)
    const shareParam = urlParams.get('share')

    if (!shareParam) {
        return {
            hadShareParam: false,
            shareFolder: null,
            shareSongId: null
        }
    }

    try {
        const decoded = atob(shareParam)
        const data = JSON.parse(decoded)

        return {
            hadShareParam: true,
            shareFolder: data.f || null,
            shareSongId: data.id || null
        }
    } catch (error) {
        console.error('解析分享链接失败', error)
        return {
            hadShareParam: true,
            shareFolder: null,
            shareSongId: null
        }
    }
}

export function createSharedSongUrl({ origin, pathname, folder, songId }) {
    const encoded = btoa(JSON.stringify({ f: folder, id: songId }))
    return `${origin}${pathname}?share=${encoded}`
}