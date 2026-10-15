import {test, afterEach} from 'node:test'
import assert from 'node:assert/strict'
import {usePlayerPlaylistLoader} from '../src/composables/player/usePlayerPlaylistLoader.js'

const originalFetch = globalThis.fetch
afterEach(() => { globalThis.fetch = originalFetch; delete globalThis.document })

function deferred() {
    let resolve
    const promise = new Promise(r => { resolve = r })
    return {promise, resolve}
}

function setup() {
    const state = {
        playlist: {value: []}, currentIndex: {value: -1}, selectedFolder: {value: 'a'},
        playbackRate: {value: 1}, audioRef: {value: {src: '', pause() {}}},
        currentSongInfo: {value: {}},
    }
    const played = []
    const notices = []
    const classes = new Set()
    globalThis.document = {body: {classList: {add: c => classes.add(c), remove: c => classes.delete(c)}}}
    const loader = usePlayerPlaylistLoader({
        ...state, showToastMessage: m => notices.push(m), saveSelectedFolder() {},
        loadPlaybackRateForFolder: () => 1, loadPlaylistSortPreferences: () => ({field: 'id', order: 'asc'}),
        resetQueueState() {}, playSongAtIndex: i => played.push(state.playlist.value[i].id),
    })
    return {state, played, notices, classes, loader}
}

test('a slow previous folder response cannot replace or play over the latest folder', async () => {
    const {state, loader, played, classes} = setup()
    const first = deferred(), second = deferred()
    globalThis.fetch = url => url.includes('folder=a') ? first.promise : second.promise
    const a = loader.setFolder('a')
    const b = loader.setFolder('b')
    second.resolve({ok: true, json: async () => [{id: 2}]})
    await b
    first.resolve({ok: true, json: async () => [{id: 1}]})
    await a
    assert.deepEqual(state.playlist.value, [{id: 2}])
    assert.deepEqual(played, [2])
    assert.equal(classes.has('loading'), false)
})

test('HTTP errors and malformed responses do not become playlists', async () => {
    for (const response of [{ok: false, status: 500}, {ok: true, json: async () => ({message: 'error'})}]) {
        const {state, loader, notices, played} = setup()
        globalThis.fetch = async () => response
        await loader.setFolder('a')
        assert.deepEqual(state.playlist.value, [])
        assert.deepEqual(played, [])
        assert.equal(notices.length, 1)
    }
})

test('refresh failure keeps the current playlist and audio', async () => {
    const {state, loader, notices} = setup()
    state.playlist.value = [{id: 4}]
    state.currentIndex.value = 0
    state.audioRef.value.src = 'current.mp3'
    globalThis.fetch = async () => { throw new Error('offline') }
    await loader.refreshSongList()
    assert.deepEqual(state.playlist.value, [{id: 4}])
    assert.equal(state.audioRef.value.src, 'current.mp3')
    assert.equal(notices.length, 1)
})

test('refresh preserves the song selected while the request was pending', async () => {
    const {state, loader} = setup()
    state.playlist.value = [{id: 4}, {id: 5}]
    state.currentIndex.value = 0
    const pending = deferred()
    globalThis.fetch = () => pending.promise
    const refresh = loader.refreshSongList()
    state.currentIndex.value = 1
    pending.resolve({ok: true, json: async () => [{id: 5}, {id: 4}]})
    await refresh
    assert.equal(state.currentIndex.value, 0)
    assert.equal(state.playlist.value[state.currentIndex.value].id, 5)
})
