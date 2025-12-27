// src/main.js
import {createApp} from 'vue'
import App from '@/App.vue'
import router from "@/router/index.js";
// 引入全局 CSS
import '@/assets/css/global.css'
import {createPinia} from "pinia";

const app = createApp(App)
app.use(createPinia()); // 确保这一行存在
app.use(router).mount('#app')
