import {test} from 'node:test'
import assert from 'node:assert/strict'
import {readFile} from 'node:fs/promises'
import vm from 'node:vm'

const file = await readFile(new URL('../src/components/feature/player/CommentDrawer.vue', import.meta.url), 'utf8')
const source = file.match(/<script setup>([\s\S]*?)<\/script>/)[1].replace(/^import .*$/gm, '')

test('a comment submitted for A cannot be inserted or cached under B', async () => {
    let resolve
    const pending = new Promise(r => { resolve = r })
    const props = {songId: 1, visible: false}
    const watchers = []
    const context = vm.createContext({
        defineProps: () => props, defineEmits() {}, ref: value => ({value}),
        computed: fn => ({get value() { return fn() }}),
        watch: (getter, callback) => watchers.push({getter, callback}),
        useAuthStore: () => ({isAuthenticated: true, username: 'listener'}),
        api: {post: () => pending}, eventBus: {emit() {}}, requestConfirm: async () => true,
        setTimeout, clearTimeout, console,
    })
    const component = vm.runInContext(source + '\n;({submitComment, comments, inputContent, commentCache})', context)
    component.inputContent.value = 'hello A'
    const request = component.submitComment()
    props.songId = 2
    const songWatcher = watchers.find(w => w.getter() === 2)
    songWatcher.callback(2)
    resolve({data: {id: 9, songId: 1, content: 'hello A'}})
    await request
    assert.equal(component.comments.value.length, 0)
    assert.equal(component.commentCache.has(2), false)
})
