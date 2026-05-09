import { defineStore } from 'pinia';
import { userApi } from '../api/user';

function sanitizeUser(u) {
  if (!u) return null;
  const { password, ...rest } = u;
  return rest;
}

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null'),
    token: localStorage.getItem('token') || null
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    username: (state) => state.userInfo?.username || '',
    nickname: (state) => state.userInfo?.nickname || '',
    avatar: (state) => state.userInfo?.avatar || '',
    role: (state) => state.userInfo?.role || 'user',
    isAdmin: (state) => state.userInfo?.role === 'admin'
  },

  actions: {
    async login(username, password) {
      const res = await userApi.login({ username, password });
      if (res.code !== 200) {
        throw new Error(res.message || '登录失败');
      }
      const raw = res.data;
      this.token = raw.token;
      this.userInfo = sanitizeUser(raw);
      localStorage.setItem('token', this.token);
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo));
      return res;
    },

    async register(payload) {
      const res = await userApi.register(payload);
      if (res.code !== 200) {
        throw new Error(res.message || '注册失败');
      }
      const raw = res.data;
      this.token = raw.token;
      this.userInfo = sanitizeUser(raw);
      localStorage.setItem('token', this.token);
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo));
      return res;
    },

    async getUserInfo() {
      if (!this.token) return;
      const res = await userApi.getUserInfo();
      if (res.code !== 200) {
        this.logout();
        throw new Error(res.message || '获取用户信息失败');
      }
      this.userInfo = sanitizeUser(res.data);
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo));
    },

    async logout() {
      if (this.token) {
        try {
          await userApi.logout();
        } catch (e) {
          // clear local session regardless of API result
        }
      }
      this.token = null;
      this.userInfo = null;
      localStorage.removeItem('token');
      localStorage.removeItem('userInfo');
    }
  }
});
