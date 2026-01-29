import axios from 'axios';
import { API_BASE } from '@/constants/index.js';

const api = axios.create({
    baseURL: API_BASE,
    withCredentials: true
});

export const getCommentsEnabled = async () => {
    return (await api.get('/public/config/comments-enabled')).data;
};

export const setCommentsEnabled = async (enabled) => {
    return (await api.post('/admin/config/comments-enabled', { enabled })).data;
};
