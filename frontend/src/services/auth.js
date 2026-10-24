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

export async function getProfile() {
    return (await http.get('/users/me')).data;
}

export async function changePassword(payload) {
    return (await http.post('/users/me/password', payload)).data;
}

export async function changeUsername(payload) {
    return (await http.post('/users/me/username', payload)).data;
}

export async function deactivateAccount(payload) {
    return (await http.post('/users/me/deactivate', payload)).data;
}

export async function unlockHiddenPlaylist() {
    return http.post('/users/unlock-hidden-playlist');
}
