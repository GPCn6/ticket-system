<template>
  <main class="auth-page">
    <section class="auth-panel" aria-labelledby="register-title">
      <header class="auth-header">
        <p class="auth-kicker">Ticket+</p>
        <h1 id="register-title">创建账户</h1>
        <p>创建账号后即可保存订单并参与演出购票。</p>
      </header>

      <form class="auth-form" @submit.prevent="register">
        <div class="form-group">
          <label for="username">用户名</label>
          <input id="username" v-model.trim="form.username" class="ui-input" type="text" autocomplete="username" placeholder="请输入用户名" :disabled="isLoading" required />
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input id="password" v-model="form.password" class="ui-input" type="password" autocomplete="new-password" placeholder="请输入密码" :disabled="isLoading" required />
        </div>
        <div class="form-group">
          <label for="confirmPassword">确认密码</label>
          <input id="confirmPassword" v-model="form.confirmPassword" class="ui-input" type="password" autocomplete="new-password" placeholder="请再次输入密码" :disabled="isLoading" required />
        </div>
        <div class="form-grid">
          <div class="form-group">
            <label for="email">邮箱 <span>选填</span></label>
            <input id="email" v-model.trim="form.email" class="ui-input" type="email" autocomplete="email" placeholder="name@example.com" :disabled="isLoading" />
          </div>
          <div class="form-group">
            <label for="phone">手机号 <span>选填</span></label>
            <input id="phone" v-model.trim="form.phone" class="ui-input" type="tel" autocomplete="tel" placeholder="请输入手机号" :disabled="isLoading" />
          </div>
        </div>

        <p v-if="error" class="form-error" role="alert">{{ error }}</p>
        <button type="submit" class="ui-button ui-button--primary auth-submit" :disabled="isLoading">{{ isLoading ? '正在创建账户' : '创建账户' }}</button>
      </form>

      <footer class="auth-footer"><span>已有账号？</span><RouterLink to="/login">立即登录</RouterLink></footer>
    </section>
  </main>
</template>

<script>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '../../store/user';

export default {
  name: 'Register',
  setup() {
    const router = useRouter();
    const userStore = useUserStore();
    const form = reactive({ username: '', password: '', confirmPassword: '', email: '', phone: '' });
    const isLoading = ref(false);
    const error = ref('');

    const register = async () => {
      if (!form.username || !form.password || !form.confirmPassword) {
        error.value = '请填写用户名、密码和确认密码。';
        return;
      }
      if (form.password !== form.confirmPassword) {
        error.value = '两次输入的密码不一致。';
        return;
      }
      try {
        isLoading.value = true;
        error.value = '';
        await userStore.register({ username: form.username, password: form.password, email: form.email, phone: form.phone });
        await router.replace('/');
      } catch (err) {
        error.value = err.message || '注册失败，请重试。';
        console.error(err);
      } finally { isLoading.value = false; }
    };

    return { form, isLoading, error, register };
  }
};
</script>

<style scoped>
.auth-page { display: grid; min-height: calc(100dvh - 80px); place-items: center; padding: 48px 24px; background: var(--surface-2); }.auth-panel { width: min(100%, 620px); padding: 0; }.auth-header { padding: 0 0 28px; border-bottom: 2px solid var(--ink); }.auth-kicker { margin-bottom: 12px; color: var(--brand); font-size: 14px; font-weight: 800; }.auth-header h1 { font-size: 32px; }.auth-header p:last-child { margin-top: 10px; color: var(--ink-2); }.auth-form { display: grid; gap: 18px; padding: 28px 0; }.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }.form-group { display: grid; gap: 8px; }.form-group label { color: var(--ink); font-size: 13px; font-weight: 700; }.form-group label span { color: var(--ink-3); font-weight: 500; }.ui-input { box-sizing: border-box; }.form-error { padding: 10px 12px; border-left: 3px solid #bd3f30; background: #fff4f2; color: #a63125; font-size: 13px; }.auth-submit { width: 100%; min-height: 44px; margin-top: 4px; }.auth-footer { display: flex; gap: 8px; padding-top: 18px; border-top: 1px solid var(--line); color: var(--ink-2); font-size: 13px; }.auth-footer a { color: var(--brand); font-weight: 700; }.auth-footer a:hover { color: var(--brand-hover); }
@media (max-width: 560px) { .auth-page { align-items: start; padding: 32px 16px; }.auth-header h1 { font-size: 28px; }.form-grid { grid-template-columns: 1fr; gap: 18px; } }
</style>
