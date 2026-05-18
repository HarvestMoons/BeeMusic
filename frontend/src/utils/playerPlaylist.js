export const DEFAULT_PLAYLIST_SORT = {
    field: 'default',
    order: 'desc'
}

export function parseSongName(name = '') {
    const cleanName = name.replace(/\.mp3$/i, '')
    const match = cleanName.match(/^(.*?)_?(BV[0-9A-Za-z]+)/i)

    return match
        ? { title: match[1], bv: match[2] }
        : { title: cleanName, bv: null }
}

export function getSongTitle(name = '') {
    return parseSongName(name).title
}

export function sortPlaylist(list = [], sortOptions = DEFAULT_PLAYLIST_SORT) {
    const field = sortOptions?.field || DEFAULT_PLAYLIST_SORT.field
    const order = sortOptions?.order || DEFAULT_PLAYLIST_SORT.order

    if (field === 'default') {
        return list
    }

    return [...list].sort((a, b) => {
        let valA = a[field]
        let valB = b[field]

        if (field === 'createdAt') {
            valA = valA ? new Date(valA).getTime() : 0
            valB = valB ? new Date(valB).getTime() : 0
        } else {
            valA = Number(valA) || 0
            valB = Number(valB) || 0
        }

        if (valA === valB) {
            return 0
        }

        const result = valA > valB ? 1 : -1
        return order === 'asc' ? result : -result
    })
}