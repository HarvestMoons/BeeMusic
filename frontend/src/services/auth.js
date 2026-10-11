// services/auth.js
import http from './http';

export async function login(credentials) {
    return http.post('/auth/login', credentials);
}

export async function getUserStatus() {
    return (await http.get('/auth/status')).data;
}

export async function logout() {
    return http.post('/auth/logout');
}

export async function register(credentials) {
    return http.post('/auth/register', credentials);
}

export async function unlockHiddenPlaylist() {
    return http.post('/users/unlock-hidden-playlist');
}