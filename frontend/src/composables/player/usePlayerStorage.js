export const STORAGE_KEYS = {
    VOLUME: 'music_volume',
    PLAYLIST_PREFIX: 'music_playlist_',
    SELECTED_FOLDER_PREFIX: 'music_selected_folder_',
    PLAYBACK_RATE_PREFIX: 'music_playback_rate_',
    SHOW_COMMENTS: 'music_show_comments',
    PLAY_MODE: 'music_play_mode'
};

export function usePlayerStorage() {
    const makeSelectedFolderKey = (id) => STORAGE_KEYS.SELECTED_FOLDER_PREFIX + id
    const makePlaybackRateKey = (folder) => STORAGE_KEYS.PLAYBACK_RATE_PREFIX + folder

    function saveVolumeToStorage(vol) {
        try {
            localStorage.setItem(STORAGE_KEYS.VOLUME, String(vol));
        } catch (e) {
        }
    }

    function loadVolumeFromStorage() {
        try {
            const v = parseFloat(localStorage.getItem(STORAGE_KEYS.VOLUME));
            if (!Number.isNaN(v)) return Math.min(1, Math.max(0, v));
        } catch (e) {
        }
        return null;
    }

    function saveSelectedFolder(id, folder) {
        try {
            localStorage.setItem(makeSelectedFolderKey(id), folder);
        } catch (e) {
        }
    }

    function loadSelectedFolder(id) {
        try {
            return localStorage.getItem(makeSelectedFolderKey(id));
        } catch (e) {
        }
    }

    function savePlaybackRateForFolder(folder, rate) {
        try {
            localStorage.setItem(makePlaybackRateKey(folder), String(rate));
        } catch (e) {
        }
    }

    function loadPlaybackRateForFolder(folder) {
        try {
            const raw = localStorage.getItem(makePlaybackRateKey(folder));
            const v = parseFloat(raw);
            if (!Number.isNaN(v)) return v;
        } catch (e) {
        }
        return null;
    }

    return {
        STORAGE_KEYS,
        saveVolumeToStorage,
        loadVolumeFromStorage,
        saveSelectedFolder,
        loadSelectedFolder,
        savePlaybackRateForFolder,
        loadPlaybackRateForFolder
    }
}
