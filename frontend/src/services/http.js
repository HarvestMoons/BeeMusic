import axios from 'axios';
import { API_BASE } from '@/constants/index.js';

// 全站共用的 axios 实例：统一 baseURL 与 cookie 携带，避免各 service 各建一个
const http = axios.create({
    baseURL: API_BASE,
    withCredentials: true,
});

export default http;
