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
  // 管理后台路由
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

// 路由守卫
router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - Ticket+` : 'Ticket+';

  const userStore = useUserStore();

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } });
  } else if (to.meta.requiresAuth && !userStore.userInfo) {
    userStore.getUserInfo().then(() => {
      if (to.meta.isAdmin && !userStore.isAdmin) {
        next({ name: 'Home' });
        return;
      }
      next();
    }).catch(() => {
      next({ name: 'Login', query: { redirect: to.fullPath } });
    });
  } else if (to.meta.isAdmin && !userStore.isAdmin) {
    next({ name: 'Home' });
  } else if (to.meta.guest && userStore.isLoggedIn) {
    next({ name: 'Home' });
  } else {
    next();
  }
});

export default router;