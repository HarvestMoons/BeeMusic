import axios from 'axios';
import { API_BASE } from "@/constants/index.js";

const api = axios.create({
    baseURL: API_BASE,
    withCredentials: true,
});

export async function getRandomMeme() {
    return (await api.get('/public/memes/random')).data;
}

export async function syncMemes() {
    return (await api.post('/memes/sync')).data;
}

export async function deleteMeme(id) {
    return (await api.delete(`/memes/${id}`)).data;
}
