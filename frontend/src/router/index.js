import { createRouter, createWebHistory } from 'vue-router';
import HomePage from '../views/HomePage.vue';
import PrivacyPage from '../views/PrivacyPage.vue';
import About from '../views/About.vue';
import { useAuthStore } from '../store';
import Login from "../components/feature/auth/Login.vue";
import Register from "../components/feature/auth/Register.vue";

const routes = [
    { path: '/', component: HomePage },
    { path: '/privacy', component: PrivacyPage },
    { path: '/about', component: About },
    { path: '/login', component: Login },
    { path: '/register', component: Register },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

router.beforeEach(async (to, from, next) => {
    const authStore = useAuthStore();
    await authStore.fetchUserStatus(); // 确保状态同步

    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
        next('/login'); // 未登录时重定向到登录页面
    } else if (to.path === '/login' && authStore.isAuthenticated) {
        next('/'); // 已登录时从登录页面重定向到首页
    } else {
        next(); // 允许访问
    }
});

export default router;