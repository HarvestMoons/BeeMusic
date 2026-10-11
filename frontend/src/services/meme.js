import http from './http';

export async function getRandomMeme() {
    return (await http.get('/public/memes/random')).data;
}

export async function syncMemes() {
    return (await http.post('/memes/sync')).data;
}

export async function deleteMeme(id) {
    return (await http.delete(`/memes/${id}`)).data;
}
