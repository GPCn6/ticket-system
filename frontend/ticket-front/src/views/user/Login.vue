<template>
  <div class="login">
    <div class="container">
      <div class="login-form">
        <h1 class="login-title">登录</h1>
        <div class="form-group">
          <label for="username">用户名</label>
          <input 
            type="text" 
            id="username" 
            v-model="form.username" 
            placeholder="请输入用户名"
          >
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input 
            type="password" 
            id="password" 
            v-model="form.password" 
            placeholder="请输入密码"
          >
        </div>
        <div class="form-actions">
          <button class="login-btn" @click="login" :disabled="isLoading">
            {{ isLoading ? '登录中...' : '登录' }}
          </button>
          <a href="/register" class="register-link">注册新账号</a>
        </div>
        <div class="error-message" v-if="error">{{ error }}</div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useUserStore } from '../../store/user';

export default {
  name: 'Login',
  setup() {
    const form = reactive({
      username: '',
      password: ''
    });
    const isLoading = ref(false);
    const error = ref('');
    const userStore = useUserStore();
    const router = useRouter();
    const route = useRoute();

    const login = async () => {
      if (!form.username || !form.password) {
        error.value = '请输入用户名和密码';
        return;
      }

      try {
        isLoading.value = true;
        error.value = '';
        await userStore.login(form.username, form.password);
        const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/';
        await router.replace(redirect || '/');
      } catch (err) {
        error.value = err.message || '登录失败，请重试';
        console.error(err);
      } finally {
        isLoading.value = false;
      }
    };

    return {
      form,
      isLoading,
      error,
      login
    };
  }
};
</script>

<style scoped>
.login {
  padding: 60px 0;
  background: #f5f5f5;
  min-height: 100vh;
}

.container {
  max-width: 400px;
  margin: 0 auto;
  padding: 0 20px;
}

.login-form {
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.login-title {
  font-size: 24px;
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #333;
}

.form-group input {
  width: 100%;
  padding: 10px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 16px;
  transition: border-color 0.3s;
}

.form-group input:focus {
  outline: none;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.form-actions {
  margin-top: 30px;
}

.login-btn {
  width: 100%;
  padding: 12px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background 0.3s;
}

.login-btn:hover {
  background: #40a9ff;
}

.login-btn:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}

.register-link {
  display: block;
  text-align: center;
  margin-top: 15px;
  font-size: 14px;
  color: #1890ff;
  text-decoration: none;
  transition: color 0.3s;
}

.register-link:hover {
  color: #40a9ff;
}

.error-message {
  margin-top: 15px;
  padding: 10px;
  background: #fff1f0;
  border: 1px solid #ffccc7;
  border-radius: 4px;
  color: #ff4d4f;
  font-size: 14px;
}
</style>