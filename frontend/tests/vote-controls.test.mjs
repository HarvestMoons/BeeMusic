import {test} from 'node:test'
import assert from 'node:assert/strict'
import {readFile} from 'node:fs/promises'
import vm from 'node:vm'

// 执行组件的 setup 脚本并替换网络与生命周期；覆盖请求乱序，无需浏览器或额外依赖。
const file = await readFile(new URL('../src/components/feature/player/VoteControls.vue', import.meta.url), 'utf8')
const source = file.match(/<script setup>([\s\S]*?)<\/script>/)[1].replace(/^import .*$/gm, '')

function deferred() {
    let resolve
    const promise = new Promise(r => { resolve = r })
    return {promise, resolve}
}

function setup(api) {
    const props = {songId: 1}
    const events = []
    let watcher, unmount
    const context = vm.createContext({
        defineProps: () => props, ref: value => ({value}),
        useAuthStore: () => ({isAuthenticated: true, username: 'listener'}), api,
        eventBus: {emit: (name, data) => events.push({name, data})}, console,
        watch: (_, callback) => { watcher = callback }, onBeforeUnmount: fn => { unmount = fn },
    })
    const component = vm.runInContext(source + '\n;({refreshVotes, handleLike, likes, dislikes, userVote})', context)
    return {props, events, component, change: async id => { props.songId = id; return watcher() }, unmount: () => unmount()}
}

test('a late vote refresh for song A cannot overwrite song B', async () => {
    const a = deferred(), b = deferred()
    const {component, change} = setup({get: url => url.endsWith('/1') ? a.promise : b.promise})
    const first = component.refreshVotes()
    const second = change(2)
    b.resolve({data: {likes: 20, dislikes: 3, userVote: -1}})
    await second
    a.resolve({data: {likes: 1, dislikes: 0, userVote: 1}})
    await first
    assert.equal(component.likes.value, 20)
    assert.equal(component.userVote.value, -1)
})

test('voting on A then switching to B updates the event for A and preserves B controls', async () => {
    const pending = deferred()
    const {component, change, events} = setup({
        post: () => pending.promise,
        get: async () => ({data: {likes: 10, dislikes: 5, userVote: -1}}),
    })
    const vote = component.handleLike()
    await change(2)
    pending.resolve({data: {likes: 2, dislikes: 0, userVote: 1}})
    await vote
    assert.equal(component.likes.value, 10)
    assert.equal(component.userVote.value, -1)
    const event = events.find(e => e.name === 'song-vote-updated')
    assert.equal(event.data.songId, 1)
    assert.equal(event.data.likes, 2)
})

test('a late response cannot update an unmounted component', async () => {
    const pending = deferred()
    const {component, unmount} = setup({get: () => pending.promise})
    const request = component.refreshVotes()
    unmount()
    pending.resolve({data: {likes: 99, dislikes: 0, userVote: 1}})
    await request
    assert.equal(component.likes.value, 0)
})
