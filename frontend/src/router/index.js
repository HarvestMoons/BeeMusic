import { createRouter, createWebHistory } from 'vue-router';

const About = () => import('@/views/About.vue');
const MemePage = () => import('@/views/MemePage.vue');
const Profile = () => import('@/views/Profile.vue');

// 根路径不配 component：HomePage 由 App.vue 直接用 v-show 渲染（保持常驻不销毁），
// 这里的 component 永远不会被 router-view 渲染，保留路径匹配即可
const routes = [
    { path: '/' },
    { path: '/about', component: About },
    { path: '/meme', component: MemePage },
    { path: '/profile', component: Profile },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

export default router;