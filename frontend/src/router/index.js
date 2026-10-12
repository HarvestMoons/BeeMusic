import { createRouter, createWebHistory } from 'vue-router';

const About = () => import('@/views/About.vue');
const MemePage = () => import('@/views/MemePage.vue');
const Settings = () => import('@/views/Settings.vue');

// 根路径的 HomePage 由 App.vue 用 v-show 常驻渲染（避免切页时销毁播放器），
// 这里不会渲染，但必须给一个 component：没有任何 component/children 的路由记录
// 会被 vue-router 直接丢弃，导致所有对 '/' 的解析报 "No match found"。
const EmptyView = { render: () => null };

const routes = [
    { path: '/', component: EmptyView },
    { path: '/about', component: About },
    { path: '/meme', component: MemePage },
    { path: '/settings', component: Settings },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

export default router;