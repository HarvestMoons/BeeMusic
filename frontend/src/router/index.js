import { createRouter, createWebHistory } from 'vue-router';
import HomePage from '@/views/HomePage.vue';
import PrivacyPage from '@/views/PrivacyPage.vue';
import About from '@/views/About.vue';
import MemePage from '@/views/MemePage.vue';

const routes = [
    { path: '/', component: HomePage },
    { path: '/privacy', component: PrivacyPage },
    { path: '/about', component: About },
    { path: '/meme', component: MemePage },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

export default router;