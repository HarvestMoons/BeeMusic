<template>
  <div class="register-container">
    <div class="register-card">
      <h2 class="text-2xl font-bold mb-4">注册</h2>
      <form @submit.prevent="handleRegister">
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
            class="w-full bg-green-500 text-white p-2 rounded hover:bg-green-600"
        >
          注册
        </button>
      </form>
      <p class="mt-4 text-center">
        已有账号？<router-link to="/login" class="text-blue-500">登录</router-link>
      </p>
      <p v-if="error" class="mt-2 text-red-500 text-center">{{ error }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../../../store';
import { register } from '../../../services/auth';

const username = ref('');
const password = ref('');
const error = ref('');
const router = useRouter();
const authStore = useAuthStore();

async function handleRegister() {
  try {
    await register({ username: username.value, password: password.value });
    await authStore.fetchUserStatus();
    await router.push('/');
  } catch (err) {
    error.value = `注册失败: ${err.response?.data?.message || err.message}`;
    console.error('Register error:', err.response?.status, err.response?.data, err.message);
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f5f5;
}

.register-card {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
}
</style>