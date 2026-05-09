<template>
  <div class="register">
    <div class="container">
      <div class="register-form">
        <h1 class="register-title">注册</h1>
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
        <div class="form-group">
          <label for="confirmPassword">确认密码</label>
          <input 
            type="password" 
            id="confirmPassword" 
            v-model="form.confirmPassword" 
            placeholder="请确认密码"
          >
        </div>
        <div class="form-group">
          <label for="email">邮箱</label>
          <input 
            type="email" 
            id="email" 
            v-model="form.email" 
            placeholder="请输入邮箱"
          >
        </div>
        <div class="form-group">
          <label for="phone">手机</label>
          <input 
            type="tel" 
            id="phone" 
            v-model="form.phone" 
            placeholder="请输入手机号"
          >
        </div>
        <div class="form-actions">
          <button class="register-btn" @click="register" :disabled="isLoading">
            {{ isLoading ? '注册中...' : '注册' }}
          </button>
          <a href="/login" class="login-link">已有账号？立即登录</a>
        </div>
        <div class="error-message" v-if="error">{{ error }}</div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '../../store/user';

export default {
  name: 'Register',
  setup() {
    const router = useRouter();
    const userStore = useUserStore();
    const form = reactive({
      username: '',
      password: '',
      confirmPassword: '',
      email: '',
      phone: ''
    });
    const isLoading = ref(false);
    const error = ref('');

    const register = async () => {
      if (!form.username || !form.password || !form.confirmPassword) {
        error.value = '请填写必填字段';
        return;
      }

      if (form.password !== form.confirmPassword) {
        error.value = '两次输入的密码不一致';
        return;
      }

      try {
        isLoading.value = true;
        error.value = '';
        await userStore.register({
          username: form.username,
          password: form.password,
          email: form.email,
          phone: form.phone
        });
        await router.replace('/');
      } catch (err) {
        error.value = err.message || '注册失败，请重试';
        console.error(err);
      } finally {
        isLoading.value = false;
      }
    };

    return {
      form,
      isLoading,
      error,
      register
    };
  }
};
</script>

<style scoped>
.register {
  padding: 60px 0;
  background: #f5f5f5;
  min-height: 100vh;
}

.container {
  max-width: 400px;
  margin: 0 auto;
  padding: 0 20px;
}

.register-form {
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.register-title {
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

.register-btn {
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

.register-btn:hover {
  background: #40a9ff;
}

.register-btn:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}

.login-link {
  display: block;
  text-align: center;
  margin-top: 15px;
  font-size: 14px;
  color: #1890ff;
  text-decoration: none;
  transition: color 0.3s;
}

.login-link:hover {
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