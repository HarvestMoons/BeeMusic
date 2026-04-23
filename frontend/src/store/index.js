import { defineStore } from 'pinia';
import { getUserStatus } from '@/services/auth';
import { getCommentsEnabled, setCommentsEnabled } from '@/services/siteConfig';

let authStatusRequest = null;

export const useAuthStore = defineStore('auth', {
    state: () => ({
        isAuthenticated: false,
        username: null,
        role: null, // 1=USER, 2=ADMIN, 3=STATION_MASTER
        isHiddenPlaylistUnlocked: false,
        initialized: false,
    }),
    actions: {
        applyUserStatus(response) {
            this.isAuthenticated = response.success;
            this.username = response.username || null;
            this.role = response.role || null;
            this.isHiddenPlaylistUnlocked = response.isHiddenPlaylistUnlocked || false;
        },
        clearUserStatus() {
            this.isAuthenticated = false;
            this.username = null;
            this.role = null;
            this.isHiddenPlaylistUnlocked = false;
        },
        async refreshUserStatus() {
            if (authStatusRequest) {
                return authStatusRequest;
            }

            authStatusRequest = (async () => {
                try {
                    const response = await getUserStatus();
                    this.applyUserStatus(response);
                    this.initialized = true;
                    return response;
                } catch (error) {
                    this.clearUserStatus();
                    this.initialized = true;
                    throw error;
                } finally {
                    authStatusRequest = null;
                }
            })();

            return authStatusRequest;
        },
        async initializeAuth() {
            if (this.initialized) {
                return;
            }

            try {
                await this.refreshUserStatus();
            } catch (error) {
                console.error('Failed to initialize auth state', error);
            }
        },
        logout() {
            this.clearUserStatus();
            this.initialized = true;
        },
    },
    getters: {
        getUsername: (state) => state.username || '未登录',
        isStationMaster: (state) => state.role === 3,
        isAdmin: (state) => state.role === 2,
        canManageSongs: (state) => state.role === 3 || state.role === 2, // 站长或管理员
    },
});

export const useSiteConfigStore = defineStore('siteConfig', {
    state: () => ({
        commentsEnabled: false
    }),
    actions: {
        async fetchConfig() {
            try {
                const res = await getCommentsEnabled();
                this.commentsEnabled = res.enabled;
            } catch (e) {
                console.error("Failed to load site config", e);
            }
        },
        async updateCommentsEnabled(enabled) {
            try {
                await setCommentsEnabled(enabled);
                this.commentsEnabled = enabled;
            } catch (e) {
                console.error("Failed to update site config", e);
                throw e;
            }
        }
    }
});

export const useThemeStore = defineStore('theme', {
    state: () => ({
        isDarkMode: localStorage.getItem('theme') === 'dark',
        showParticles: localStorage.getItem('show_particles') !== 'false'
    }),
    actions: {
        toggleTheme() {
            this.isDarkMode = !this.isDarkMode;
            const theme = this.isDarkMode ? 'dark' : 'light';
            localStorage.setItem('theme', theme);
            document.documentElement.setAttribute('data-theme', theme);
        },
        toggleParticles() {
            this.showParticles = !this.showParticles;
            localStorage.setItem('show_particles', String(this.showParticles));
        },
        setParticles(value) {
            this.showParticles = value;
            localStorage.setItem('show_particles', String(value));
        },
        initTheme() {
            const theme = this.isDarkMode ? 'dark' : 'light';
            document.documentElement.setAttribute('data-theme', theme);
        }
    }
});