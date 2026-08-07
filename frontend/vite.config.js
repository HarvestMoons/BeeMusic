import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import {fileURLToPath, URL} from 'node:url'
import path from 'node:path'

// https://vite.dev/config/
export default defineConfig({
    plugins: [vue()],
    server: {
        proxy: {
            '/ws': {
                target: 'ws://localhost:8082',
                ws: true,
            },
        },
    },
    build: {
        sourcemap: false,
        minify: true
    },
    resolve: {
        alias: {
            '@': path.resolve(fileURLToPath(new URL('./src', import.meta.url))),
        },
    },
})
