<template>
  <main class="auth-page">
    <section class="auth-panel" aria-labelledby="login-title">
      <header class="auth-header">
        <p class="auth-kicker">Ticket+</p>
        <h1 id="login-title">登录账户</h1>
        <p>登录后继续管理你的演出、订单与购票记录。</p>
      </header>

      <form class="auth-form" @submit.prevent="login">
        <div class="form-group">
          <label for="username">用户名</label>
          <input id="username" v-model.trim="form.username" class="ui-input" type="text" autocomplete="username" placeholder="请输入用户名" :disabled="isLoading" required />
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input id="password" v-model="form.password" class="ui-input" type="password" autocomplete="current-password" placeholder="请输入密码" :disabled="isLoading" required />
        </div>

        <p v-if="error" class="form-error" role="alert">{{ error }}</p>

        <button type="submit" class="ui-button ui-button--primary auth-submit" :disabled="isLoading">
          {{ isLoading ? '正在登录' : '登录' }}
        </button>
      </form>

      <footer class="auth-footer">
        <span>还没有账号？</span>
        <RouterLink to="/register">注册新账号</RouterLink>
      </footer>
    </section>
  </main>
</template>

<script>
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '../../store/user';

export default {
  name: 'Login',
  setup() {
    const form = reactive({ username: '', password: '' });
    const isLoading = ref(false);
    const error = ref('');
    const userStore = useUserStore();
    const router = useRouter();
    const route = useRoute();

    const login = async () => {
      if (!form.username || !form.password) {
        error.value = '请输入用户名和密码。';
        return;
      }
      try {
        isLoading.value = true;
        error.value = '';
        await userStore.login(form.username, form.password);
        const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/';
        await router.replace(redirect || '/');
      } catch (err) {
        error.value = err.message || '登录失败，请重试。';
        console.error(err);
      } finally { isLoading.value = false; }
    };

    return { form, isLoading, error, login };
  }
};
</script>

<style scoped>
.auth-page { display: grid; min-height: calc(100dvh - 80px); place-items: center; padding: 48px 24px; background: var(--surface-2); }.auth-panel { width: min(100%, 430px); padding: 0; }.auth-header { padding: 0 0 28px; border-bottom: 2px solid var(--ink); }.auth-kicker { margin-bottom: 12px; color: var(--brand); font-size: 14px; font-weight: 800; }.auth-header h1 { font-size: 32px; }.auth-header p:last-child { margin-top: 10px; color: var(--ink-2); }.auth-form { display: grid; gap: 18px; padding: 28px 0; }.form-group { display: grid; gap: 8px; }.form-group label { color: var(--ink); font-size: 13px; font-weight: 700; }.ui-input { box-sizing: border-box; }.form-error { padding: 10px 12px; border-left: 3px solid #bd3f30; background: #fff4f2; color: #a63125; font-size: 13px; }.auth-submit { width: 100%; min-height: 44px; margin-top: 4px; }.auth-footer { display: flex; gap: 8px; padding-top: 18px; border-top: 1px solid var(--line); color: var(--ink-2); font-size: 13px; }.auth-footer a { color: var(--brand); font-weight: 700; }.auth-footer a:hover { color: var(--brand-hover); }
@media (max-width: 480px) { .auth-page { align-items: start; padding: 32px 16px; }.auth-header h1 { font-size: 28px; } }
</style>
