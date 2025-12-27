// services/auth.js
import axios from 'axios';
import {API_BASE} from "@/constants/index.js";

const api = axios.create({
    baseURL: API_BASE,
    withCredentials: true, // 携带 cookie
});

export async function login(credentials) {
    return api.post('/auth/login', credentials);
}

export async function getUserStatus() {
    return (await api.get('/auth/status')).data;
}

export async function logout() {
    return api.post('/auth/logout');
}

export async function register(credentials) {
    return api.post('/auth/register', credentials);
}

export async function unlockHiddenPlaylist() {
    return api.post('/users/unlock-hidden-playlist');
}

export default api;