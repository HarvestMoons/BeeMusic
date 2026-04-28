import { createRouter, createWebHistory } from 'vue-router';
import HomePage from '@/views/HomePage.vue';

const About = () => import('@/views/About.vue');
const MemePage = () => import('@/views/MemePage.vue');

const routes = [
    { path: '/', component: HomePage },
    { path: '/about', component: About },
    { path: '/meme', component: MemePage },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

export default router;