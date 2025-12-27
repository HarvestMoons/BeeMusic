import {defineStore} from 'pinia';
import {getUserStatus} from '@/services/auth'; // 假设已定义

export const useAuthStore = defineStore('auth', {
    state: () => ({
        isAuthenticated: false,
        username: null,
        isHiddenPlaylistUnlocked: false,
    }),
    actions: {
        async fetchUserStatus() {
            const response = await getUserStatus();
            this.isAuthenticated = response.success;
            this.username = response.username || null;
            this.isHiddenPlaylistUnlocked = response.isHiddenPlaylistUnlocked || false;
        },
        logout() {
            this.isAuthenticated = false;
            this.username = null;
            this.isHiddenPlaylistUnlocked = false;
        },
    },
    getters: {
        getUsername: (state) => state.username || '未登录',
    },
});

export const useThemeStore = defineStore('theme', {
    state: () => ({
        isDarkMode: localStorage.getItem('theme') === 'dark',
    }),
    actions: {
        toggleTheme() {
            this.isDarkMode = !this.isDarkMode;
            const theme = this.isDarkMode ? 'dark' : 'light';
            localStorage.setItem('theme', theme);
            document.documentElement.setAttribute('data-theme', theme);
        },
        initTheme() {
            const theme = this.isDarkMode ? 'dark' : 'light';
            document.documentElement.setAttribute('data-theme', theme);
        }
    }
});