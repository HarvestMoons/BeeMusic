import {defineStore} from 'pinia';
import {getUserStatus} from '@/services/auth'; // 假设已定义

export const useAuthStore = defineStore('auth', {
    state: () => ({
        isAuthenticated: false,
        username: null,
        role: null, // 1=USER, 2=ADMIN, 3=STATION_MASTER
        isHiddenPlaylistUnlocked: false,
    }),
    actions: {
        async fetchUserStatus() {
            const response = await getUserStatus();
            this.isAuthenticated = response.success;
            this.username = response.username || null;
            this.role = response.role || null;
            this.isHiddenPlaylistUnlocked = response.isHiddenPlaylistUnlocked || false;
        },
        logout() {
            this.isAuthenticated = false;
            this.username = null;
            this.role = null;
            this.isHiddenPlaylistUnlocked = false;
        },
    },
    getters: {
        getUsername: (state) => state.username || '未登录',
        isStationMaster: (state) => state.role === 3,
        isAdmin: (state) => state.role === 2,
        canManageSongs: (state) => state.role === 3 || state.role === 2, // 站长或管理员
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