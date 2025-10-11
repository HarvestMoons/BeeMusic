import { defineStore } from 'pinia';
import { getUserStatus } from '../services/auth'; // 假设已定义

export const useAuthStore = defineStore('auth', {
    state: () => ({
        isAuthenticated: false,
        username: null,
    }),
    actions: {
        async fetchUserStatus() {
            const response = await getUserStatus();
            this.isAuthenticated = response.success;
            this.username = response.username || null;
        },
        logout() {
            this.isAuthenticated = false;
            this.username = null;
        },
    },
    getters: {
        getUsername: (state) => state.username || '未登录',
    },
});