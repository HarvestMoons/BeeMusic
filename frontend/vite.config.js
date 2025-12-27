import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import {fileURLToPath, URL} from 'node:url'
import path from 'node:path'

// https://vite.dev/config/
export default defineConfig({
    plugins: [vue()],
    build: {
        sourcemap: true, // 生成 source map
        minify: false    // 关闭压缩，方便调试
    },
    resolve: {
        alias: {
            '@': path.resolve(fileURLToPath(new URL('./src', import.meta.url))),
        },
    },
})
