export const STORAGE_KEYS = {
    VOLUME: 'music_volume',
    SELECTED_FOLDER_PREFIX: 'music_selected_folder_',
    PLAYBACK_RATE_PREFIX: 'music_playback_rate_',
    SHOW_COMMENTS: 'music_show_comments',
    PLAY_MODE: 'music_play_mode',
    PLAYLIST_SORT_FIELD: 'music_playlist_sort_field',
    PLAYLIST_SORT_ORDER: 'music_playlist_sort_order'
};

export function usePlayerStorage() {
    const makeSelectedFolderKey = (id) => STORAGE_KEYS.SELECTED_FOLDER_PREFIX + id
    const makePlaybackRateKey = (folder) => STORAGE_KEYS.PLAYBACK_RATE_PREFIX + folder

    function saveValue(key, value) {
        try {
            localStorage.setItem(key, String(value));
        } catch (e) {
        }
    }

    function loadValue(key) {
        try {
            return localStorage.getItem(key);
        } catch (e) {
        }
        return null;
    }

    function saveVolumeToStorage(vol) {
        saveValue(STORAGE_KEYS.VOLUME, vol)
    }

    function loadVolumeFromStorage() {
        const raw = loadValue(STORAGE_KEYS.VOLUME)
        const v = parseFloat(raw);
        if (!Number.isNaN(v)) return Math.min(1, Math.max(0, v));
        return null;
    }

    function saveSelectedFolder(id, folder) {
        saveValue(makeSelectedFolderKey(id), folder)
    }

    function loadSelectedFolder(id) {
        return loadValue(makeSelectedFolderKey(id))
    }

    function savePlaybackRateForFolder(folder, rate) {
        saveValue(makePlaybackRateKey(folder), rate)
    }

    function loadPlaybackRateForFolder(folder) {
        const raw = loadValue(makePlaybackRateKey(folder))
        const v = parseFloat(raw);
        if (!Number.isNaN(v)) return v;
        return null;
    }

    function savePlayMode(mode) {
        saveValue(STORAGE_KEYS.PLAY_MODE, mode)
    }

    function loadPlayMode() {
        return loadValue(STORAGE_KEYS.PLAY_MODE)
    }

    function savePlaylistSortField(field) {
        saveValue(STORAGE_KEYS.PLAYLIST_SORT_FIELD, field)
    }

    function savePlaylistSortOrder(order) {
        saveValue(STORAGE_KEYS.PLAYLIST_SORT_ORDER, order)
    }

    function loadPlaylistSortPreferences() {
        return {
            field: loadValue(STORAGE_KEYS.PLAYLIST_SORT_FIELD) || 'default',
            order: loadValue(STORAGE_KEYS.PLAYLIST_SORT_ORDER) || 'desc'
        }
    }

    function saveCommentsVisibility(value, key = STORAGE_KEYS.SHOW_COMMENTS) {
        saveValue(key, value)
    }

    function loadCommentsVisibility(key = STORAGE_KEYS.SHOW_COMMENTS, fallback = true) {
        const raw = loadValue(key)
        if (raw === null) {
            return fallback
        }
        return raw === 'true'
    }

    return {
        STORAGE_KEYS,
        saveVolumeToStorage,
        loadVolumeFromStorage,
        saveSelectedFolder,
        loadSelectedFolder,
        savePlaybackRateForFolder,
        loadPlaybackRateForFolder,
        savePlayMode,
        loadPlayMode,
        savePlaylistSortField,
        savePlaylistSortOrder,
        loadPlaylistSortPreferences,
        saveCommentsVisibility,
        loadCommentsVisibility
    }
}
