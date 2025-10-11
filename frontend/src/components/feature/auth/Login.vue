<template>
  <div class="login-container">
    <div class="login-card">
      <h2 class="text-2xl font-bold mb-4">登录</h2>
      <form @submit.prevent="handleLogin">
        <div class="mb-4">
          <label for="username" class="block text-gray-700">用户名</label>
          <input
              v-model="username"
              type="text"
              id="username"
              class="w-full p-2 border rounded"
              required
          />
        </div>
        <div class="mb-4">
          <label for="password" class="block text-gray-700">密码</label>
          <input
              v-model="password"
              type="password"
              id="password"
              class="w-full p-2 border rounded"
              required
          />
        </div>
        <button
            type="submit"
            class="w-full bg-blue-500 text-white p-2 rounded hover:bg-blue-600"
        >
          登录
        </button>
      </form>
      <p class="mt-4 text-center">
        还没有账号？<router-link to="/register" class="text-blue-500">注册</router-link>
      </p>
      <p v-if="error" class="mt-2 text-red-500 text-center">{{ error }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import {useAuthStore} from "../../../store/index.js"
import {login} from "../../../services/auth.js";


const username = ref('');
const password = ref('');
const error = ref('');
const router = useRouter();
const authStore = useAuthStore();

async function handleLogin() {
  try {
    await login({ username: username.value, password: password.value });
    await authStore.fetchUserStatus(); // 更新认证状态
    await router.push('/'); // 登录成功后重定向到首页
  } catch (err) {
    error.value = '登录失败，请检查用户名和密码';
    console.error('Login error:', err);
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f5f5;
}

.login-card {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
}
</style>