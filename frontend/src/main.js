// src/main.js
import { createApp } from 'vue'
import App from './App.vue'
import router from "./router/index.js";
// 引入全局 CSS
import './assets/css/global.css'
import {createPinia} from "pinia";

const app = createApp(App)
app.use(createPinia()); // 确保这一行存在
app.use(router).mount('#app')

// 推荐在主入口注册 SW（只注册一次）
if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('/sw.js')
        .then(reg => console.log('SW registered (main):', reg))
        .catch(err => console.warn('SW register failed (main):', err));
}
