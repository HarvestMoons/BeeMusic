import http from './http';

export const getCommentsEnabled = async () => {
    return (await http.get('/public/config/comments-enabled')).data;
};

export const setCommentsEnabled = async (enabled) => {
    return (await http.post('/admin/config/comments-enabled', {enabled})).data;
};

export const syncDatabase = async () => {
    return (await http.post('/admin/songs/sync')).data;
};
