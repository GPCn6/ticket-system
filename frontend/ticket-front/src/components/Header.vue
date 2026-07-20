<template>
  <header class="site-header">
    <div class="header-inner">
      <router-link to="/" class="brand" aria-label="Ticket+ 首页">
        <span class="brand-mark">T+</span>
        <span>Ticket+</span>
      </router-link>

      <nav class="desktop-nav" aria-label="主导航">
        <router-link to="/">发现</router-link>
        <router-link to="/category/演唱会">演唱会</router-link>
        <router-link to="/category/话剧">话剧</router-link>
        <router-link to="/category/体育">体育</router-link>
        <router-link to="/seckill" class="accent-link">限时抢票</router-link>
      </nav>

      <form class="header-search" role="search" @submit.prevent="handleSearch">
        <input v-model="searchKeyword" type="search" aria-label="搜索演出" placeholder="搜索演出、场馆" />
        <button type="submit" title="搜索" aria-label="搜索"><el-icon><Search /></el-icon></button>
      </form>

      <div class="header-actions">
        <template v-if="userStore.isLoggedIn">
          <router-link to="/order" class="orders-link">订单</router-link>
          <el-dropdown trigger="click">
            <button class="profile-button" type="button">
              <span class="avatar-fallback">{{ avatarInitial }}</span>
              <span class="profile-name">{{ userStore.nickname || userStore.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/order')"><el-icon><Ticket /></el-icon>我的订单</el-dropdown-item>
                <el-dropdown-item @click="router.push('/user')"><el-icon><User /></el-icon>个人中心</el-dropdown-item>
                <el-dropdown-item v-if="userStore.isAdmin" @click="router.push('/admin')"><el-icon><Setting /></el-icon>管理后台</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout"><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <router-link to="/login" class="login-link">登录</router-link>
          <router-link to="/register" class="signup-link">注册</router-link>
        </template>
        <button class="menu-button" type="button" :aria-expanded="menuOpen" :aria-label="menuOpen ? '关闭导航' : '打开导航'" @click="menuOpen = !menuOpen">
          <el-icon><Close v-if="menuOpen" /><Menu v-else /></el-icon>
        </button>
      </div>
    </div>

    <div v-if="menuOpen" class="mobile-menu">
      <form class="mobile-search" role="search" @submit.prevent="handleSearch"><input v-model="searchKeyword" type="search" aria-label="搜索演出" placeholder="搜索演出、场馆" /><button type="submit">搜索</button></form>
      <nav aria-label="移动导航" @click="menuOpen = false">
        <router-link to="/">发现</router-link><router-link to="/category/演唱会">演唱会</router-link><router-link to="/category/话剧">话剧</router-link><router-link to="/category/体育">体育</router-link><router-link to="/seckill">限时抢票</router-link>
      </nav>
      <div class="mobile-auth">
        <template v-if="!userStore.isLoggedIn">
          <router-link to="/login" @click="menuOpen = false">登录</router-link>
          <router-link to="/register" class="mobile-auth__primary" @click="menuOpen = false">注册</router-link>
        </template>
        <template v-else>
          <router-link to="/order" @click="menuOpen = false">我的订单</router-link>
          <router-link to="/user" @click="menuOpen = false">个人中心</router-link>
          <button type="button" @click="handleLogout">退出登录</button>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '../store/user';
import { Search, ArrowDown, Ticket, User, SwitchButton, Setting, Menu, Close } from '@element-plus/icons-vue';

const router = useRouter();
const userStore = useUserStore();
const searchKeyword = ref('');
const menuOpen = ref(false);
const avatarInitial = computed(() => (userStore.nickname || userStore.username || 'T').slice(0, 1).toUpperCase());

const handleSearch = async () => {
  const keyword = searchKeyword.value.trim();
  if (!keyword) return;
  menuOpen.value = false;
  await router.push({ name: 'Search', query: { keyword } });
};

const handleLogout = async () => { menuOpen.value = false; await userStore.logout(); await router.push('/'); };
</script>

<style scoped>
.site-header { position: sticky; top: 0; z-index: 1000; border-bottom: 1px solid var(--line); background: rgba(255,255,255,.96); backdrop-filter: blur(14px); }
.header-inner { width: min(100% - 48px, var(--container)); height: 68px; margin: auto; display: flex; align-items: center; gap: 30px; }
.brand { flex: none; display: flex; align-items: center; gap: 9px; font-size: 18px; font-weight: 800; }
.brand-mark { width: 30px; height: 30px; display: grid; place-items: center; color: #fff; background: var(--ink); border-radius: var(--radius); font-size: 14px; }
.desktop-nav { display: flex; align-items: center; gap: 24px; white-space: nowrap; }
.desktop-nav a, .orders-link, .login-link { color: var(--ink-2); font-weight: 600; }
.desktop-nav a:hover, .desktop-nav a.router-link-exact-active, .desktop-nav .accent-link { color: var(--brand); }
.header-search { flex: 1; max-width: 320px; margin-left: auto; display: flex; height: 40px; border: 1px solid var(--line-strong); border-radius: var(--radius); background: var(--surface-2); }
.header-search:focus-within { border-color: var(--brand); box-shadow: 0 0 0 3px rgba(217,71,50,.12); }
.header-search input { min-width: 0; flex: 1; border: 0; outline: 0; padding: 0 12px; color: var(--ink); background: transparent; }
.header-search button { width: 40px; border: 0; background: transparent; cursor: pointer; }
.header-actions { flex: none; display: flex; align-items: center; gap: 16px; }
.signup-link { height: 38px; display: inline-flex; align-items: center; padding: 0 15px; color: #fff; background: var(--ink); border-radius: var(--radius); font-weight: 700; }
.profile-button { min-height: 40px; display: flex; align-items: center; gap: 8px; padding: 0 8px 0 4px; border: 0; background: transparent; cursor: pointer; }
.avatar-fallback { width: 32px; height: 32px; display: grid; place-items: center; border-radius: 50%; color: #fff; background: var(--brand); font-weight: 800; }
.profile-name { max-width: 90px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.menu-button { display: none; width: 40px; height: 40px; border: 1px solid var(--line); border-radius: var(--radius); background: var(--surface); }
.mobile-menu { display: none; }
@media (max-width: 1080px) { .desktop-nav { gap: 15px; } .header-search { max-width: 230px; } }
@media (max-width: 860px) {
  .header-inner { width: min(100% - 32px, var(--container)); gap: 14px; }
  .desktop-nav, .header-search, .orders-link, .profile-name, .login-link, .signup-link { display: none; }
  .header-actions { margin-left: auto; }
  .menu-button { display: grid; place-items: center; }
  .mobile-menu { display: block; padding: 14px 16px 20px; border-top: 1px solid var(--line); background: var(--surface); }
  .mobile-search { display: flex; height: 42px; margin-bottom: 14px; }
  .mobile-search input { min-width: 0; flex: 1; padding: 0 12px; border: 1px solid var(--line-strong); border-radius: var(--radius) 0 0 var(--radius); }
  .mobile-search button { width: 64px; border: 0; color: #fff; background: var(--ink); border-radius: 0 var(--radius) var(--radius) 0; }
  .mobile-menu nav { display: grid; grid-template-columns: repeat(2, 1fr); gap: 4px; }
  .mobile-menu nav a { padding: 11px 10px; color: var(--ink-2); font-weight: 650; border-bottom: 1px solid var(--line); }
  .mobile-auth { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--line); }
  .mobile-auth a, .mobile-auth button { min-height: 42px; display: grid; place-items: center; padding: 0 12px; border: 1px solid var(--line-strong); border-radius: var(--radius); background: var(--surface); color: var(--ink); font: inherit; font-weight: 700; cursor: pointer; }
  .mobile-auth .mobile-auth__primary { border-color: var(--ink); background: var(--ink); color: #fff; }
  .mobile-auth button { grid-column: 1 / -1; color: var(--danger); }
}
</style>
