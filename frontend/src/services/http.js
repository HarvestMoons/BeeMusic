import axios from 'axios';
import { API_BASE } from '@/constants/index.js';

// 全站共用的 axios 实例：统一 baseURL 与 cookie 携带，避免各 service 各建一个
const http = axios.create({
    baseURL: API_BASE,
    withCredentials: true,
});

// 使用独立客户端获取令牌，避免触发写请求拦截器；令牌只留在当前页面内存中。
const csrfClient = axios.create({baseURL: API_BASE, withCredentials: true});
let csrfTokenRequest = null;
const safeMethods = new Set(['get', 'head', 'options', 'trace']);

function clearCsrfToken() {
    csrfTokenRequest = null;
}

function getCsrfToken() {
    if (!csrfTokenRequest) {
        const pending = csrfClient.get('/auth/csrf').then(({data}) => {
            if (typeof data?.token !== 'string' || typeof data?.headerName !== 'string') {
                throw new Error('获取安全令牌失败');
            }
            return data;
        }).catch(error => {
            if (csrfTokenRequest === pending) clearCsrfToken();
            throw error;
        });
        csrfTokenRequest = pending;
    }
    return csrfTokenRequest;
}

function needsCsrf(config) {
    const url = new URL(config.url, new URL(config.baseURL || API_BASE, window.location.origin));
    return url.origin === window.location.origin && !safeMethods.has((config.method || 'get').toLowerCase());
}

http.interceptors.request.use(async config => {
    if (needsCsrf(config)) {
        const csrf = await getCsrfToken();
        config.headers.set(csrf.headerName, csrf.token);
    }
    return config;
});

http.interceptors.response.use(response => {
    if (['/auth/login', '/auth/logout', '/users/me/deactivate'].includes(response.config.url)) clearCsrfToken();
    return response;
}, async error => {
    const config = error.config;
    // 仅重试 CSRF 过滤器明确拒绝的请求；此时业务尚未执行，不重试权限或业务错误。
    if (config && needsCsrf(config) && error.response?.status === 403 &&
            error.response?.data?.code === 'CSRF_INVALID' && !config.csrfRetried) {
        clearCsrfToken();
        config.csrfRetried = true;
        return http.request(config);
    }
    return Promise.reject(error);
});

export default http;
