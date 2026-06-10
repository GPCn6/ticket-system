import { createRouter, createWebHistory } from 'vue-router';
import { useUserStore } from '../store/user';

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/home/Home.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/show/:id',
    name: 'ShowDetail',
    component: () => import('../views/show/ShowDetail.vue'),
    props: true,
    meta: { title: '演出详情' }
  },
  {
    path: '/seckill',
    name: 'Seckill',
    component: () => import('../views/seckill/Seckill.vue'),
    meta: { title: '限时抢购' }
  },
  {
    path: '/seckill/:sessionId',
    name: 'SeckillDetail',
    component: () => import('../views/seckill/SeckillDetail.vue'),
    props: true,
    meta: { title: '抢购详情' }
  },
  {
    path: '/order',
    name: 'Order',
    component: () => import('../views/order/Order.vue'),
    meta: { title: '我的订单', requiresAuth: true }
  },
  {
    path: '/order/:orderNo',
    name: 'OrderDetail',
    component: () => import('../views/order/OrderDetail.vue'),
    props: true,
    meta: { title: '订单详情', requiresAuth: true }
  },
  {
    path: '/user',
    name: 'UserCenter',
    component: () => import('../views/user/UserCenter.vue'),
    meta: { title: '个人中心', requiresAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/user/Login.vue'),
    meta: { title: '登录', guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/user/Register.vue'),
    meta: { title: '注册', guest: true }
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('../views/home/Search.vue'),
    meta: { title: '搜索结果' }
  },
  {
    path: '/category/:category',
    name: 'Category',
    component: () => import('../views/home/Category.vue'),
    meta: { title: '分类列表' }
  },
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('../views/admin/AdminDashboard.vue'),
    meta: { title: '管理后台', requiresAuth: true, isAdmin: true }
  },
  {
    path: '/admin/shows',
    name: 'ShowManage',
    component: () => import('../views/admin/ShowManage.vue'),
    meta: { title: '演出管理', requiresAuth: true, isAdmin: true }
  },
  {
    path: '/admin/tickets',
    name: 'TicketManage',
    component: () => import('../views/admin/TicketManage.vue'),
    meta: { title: '票档管理', requiresAuth: true, isAdmin: true }
  },
  {
    path: '/admin/seckill',
    name: 'SeckillManage',
    component: () => import('../views/admin/SeckillManage.vue'),
    meta: { title: '秒杀管理', requiresAuth: true, isAdmin: true }
  },
  {
    path: '/admin/orders',
    name: 'OrderManage',
    component: () => import('../views/admin/OrderManage.vue'),
    meta: { title: '订单管理', requiresAuth: true, isAdmin: true }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// getUserInfo Promise 缓存，防止路由切换时多次并发调用
let userInfoPromise = null;

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? to.meta.title + ' - Ticket+' : 'Ticket+';

  const userStore = useUserStore();

  // 未登录但需要认证 → 跳转登录
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } });
    return;
  }

  // 已登录但没有 userInfo（刚刷新）→ 异步获取
  if (to.meta.requiresAuth && !userStore.userInfo) {
    if (!userInfoPromise) {
      userInfoPromise = userStore.getUserInfo().finally(() => {
        userInfoPromise = null;
      });
    }
    userInfoPromise.then(() => {
      if (to.meta.isAdmin && !userStore.isAdmin) {
        next({ name: 'Home' });
        return;
      }
      next();
    }).catch(() => {
      next({ name: 'Login', query: { redirect: to.fullPath } });
    });
    return;
  }

  // 需要管理员角色但不是 → 跳首页
  if (to.meta.isAdmin && !userStore.isAdmin) {
    next({ name: 'Home' });
    return;
  }

  // 已登录用户访问登录/注册页 → 跳首页
  if (to.meta.guest && userStore.isLoggedIn) {
    next({ name: 'Home' });
    return;
  }

  next();
});

export default router;
