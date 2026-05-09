<template>
  <header class="app-header">
    <div class="header-container">
      <div class="header-left">
        <router-link to="/" class="logo">
          <span>Ticket+</span>
        </router-link>
        <nav class="nav-menu">
          <router-link to="/" class="nav-item">首页</router-link>
          <router-link to="/category/演唱会" class="nav-item">演唱会</router-link>
          <router-link to="/category/话剧" class="nav-item">话剧</router-link>
          <router-link to="/category/体育" class="nav-item">体育</router-link>
          <router-link to="/seckill" class="nav-item seckill">抢购</router-link>
        </nav>
      </div>

      <div class="header-center">
        <div class="search-box">
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索演出、场馆、艺人"
            class="search-input"
            @keyup.enter="handleSearch"
          />
          <button class="search-btn" @click="handleSearch">
            <el-icon><Search /></el-icon>
          </button>
        </div>
      </div>

      <div class="header-right">
        <template v-if="userStore.isLoggedIn">
          <el-dropdown trigger="click">
            <div class="user-info">
              <img :src="userStore.avatar || defaultAvatar" class="avatar" />
              <span class="nickname">{{ userStore.nickname }}</span>
              <el-icon class="arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/order')">
                  <el-icon><Ticket /></el-icon>
                  我的订单
                </el-dropdown-item>
                <el-dropdown-item @click="$router.push('/user')">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item v-if="userStore.isAdmin" @click="$router.push('/admin')">
                  <el-icon><Setting /></el-icon>
                  管理后台
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <router-link to="/login" class="login-btn">登录</router-link>
          <router-link to="/register" class="register-btn">注册</router-link>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '../store/user';
import { Search, ArrowDown, Ticket, User, SwitchButton, Setting } from '@element-plus/icons-vue';

const router = useRouter();
const userStore = useUserStore();
const searchKeyword = ref('');
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png';

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/search', query: { keyword: searchKeyword.value } });
  }
};

const handleLogout = () => {
  userStore.logout();
  router.push('/');
};
</script>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 1000;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

  .header-container {
    display: flex;
    align-items: center;
    justify-content: space-between;
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 24px;
    height: 64px;
  }

  .header-left {
    display: flex;
    align-items: center;

    .logo {
      display: flex;
      align-items: center;
      font-size: 20px;
      font-weight: 700;
      color: var(--primary-color);
      text-decoration: none;
      margin-right: 48px;
    }

    .nav-menu {
      display: flex;
      gap: 24px;

      .nav-item {
        padding: 8px 0;
        color: var(--text-regular);
        text-decoration: none;
        font-size: 15px;
        transition: color 0.3s;

        &:hover,
        &.router-link-active {
          color: var(--primary-color);
        }

        &.seckill {
          color: var(--primary-color);
          position: relative;

          &::after {
            content: '抢购';
            position: absolute;
            top: -2px;
            right: -20px;
            font-size: 10px;
            color: #fff;
            background: var(--primary-color);
            padding: 1px 4px;
            border-radius: 8px;
          }
        }
      }
    }
  }

  .header-center {
    flex: 1;
    max-width: 400px;
    margin: 0 48px;

    .search-box {
      position: relative;
      width: 100%;

      .search-input {
        width: 100%;
        padding: 8px 40px 8px 16px;
        border: 1px solid var(--border-color);
        border-radius: var(--border-radius-full);
        font-size: var(--font-size-base);
        transition: all 0.3s;

        &:focus {
          outline: none;
          border-color: var(--primary-color);
          box-shadow: 0 0 0 2px rgba(255, 77, 79, 0.2);
        }
      }

      .search-btn {
        position: absolute;
        right: 8px;
        top: 50%;
        transform: translateY(-50%);
        background: none;
        border: none;
        cursor: pointer;
        color: var(--text-secondary);

        &:hover {
          color: var(--primary-color);
        }
      }
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;

    .login-btn,
    .register-btn {
      padding: 6px 16px;
      border-radius: var(--border-radius-md);
      font-size: var(--font-size-sm);
      text-decoration: none;
      transition: all 0.3s;

      &:hover {
        transform: translateY(-1px);
      }
    }

    .login-btn {
      color: var(--text-regular);
      background: var(--bg-disabled);
    }

    .register-btn {
      color: #fff;
      background: var(--primary-color);
    }

    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      padding: 4px 12px;
      border-radius: var(--border-radius-md);
      transition: background-color 0.3s;

      &:hover {
        background: var(--bg-hover);
      }

      .avatar {
        width: 32px;
        height: 32px;
        border-radius: 50%;
      }

      .nickname {
        font-size: var(--font-size-sm);
        color: var(--text-regular);
      }

      .arrow {
        font-size: 12px;
        color: var(--text-secondary);
      }
    }
  }
}

@media (max-width: 768px) {
  .app-header {
    .header-container {
      padding: 0 16px;

      .header-left {
        .nav-menu {
          display: none;
        }
      }

      .header-center {
        margin: 0 16px;
        max-width: 200px;
      }
    }
  }
}
</style>