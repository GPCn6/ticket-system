<template>
  <div class="admin-page page-shell">
    <header class="admin-heading">
      <div><span class="context-label">运营控制台</span><h1>今日总览</h1><p>查看演出、票档与订单的实时运行情况。</p></div>
      <button class="ui-button" type="button" :disabled="loading" @click="refreshAll"><el-icon><Refresh /></el-icon>刷新数据</button>
    </header>

    <section class="metric-strip" aria-label="业务指标">
      <button v-for="item in metrics" :key="item.label" type="button" @click="$router.push(item.to)">
        <span>{{ item.label }}</span><strong>{{ item.value }}</strong><small>{{ item.note }}</small>
      </button>
    </section>

    <div class="admin-grid">
      <section class="admin-section actions-section">
        <header><div><span class="section-kicker">常用入口</span><h2>快速操作</h2></div></header>
        <nav>
          <router-link v-for="action in actions" :key="action.to" :to="action.to">
            <el-icon><component :is="action.icon" /></el-icon>
            <span><strong>{{ action.title }}</strong><small>{{ action.description }}</small></span>
            <el-icon><ArrowRight /></el-icon>
          </router-link>
        </nav>
      </section>

      <section class="admin-section status-section">
        <header><div><span class="section-kicker">系统状态</span><h2>运行检查</h2></div></header>
        <dl><div><dt>API 网关</dt><dd :class="apiState.className">{{ apiState.label }}</dd></div><div><dt>在售演出</dt><dd>{{ onSaleCount }} 场</dd></div><div><dt>秒杀场次</dt><dd>{{ stats.seckillCount }} 场</dd></div><div><dt>更新时间</dt><dd>{{ updatedAt }}</dd></div></dl>
        <p v-if="refreshError" class="refresh-error" role="alert">{{ refreshError }}</p>
      </section>
    </div>

    <section class="admin-section table-section">
      <header><div><span class="section-kicker">内容表现</span><h2>近期热门演出</h2></div><router-link to="/admin/shows">管理全部演出 <el-icon><ArrowRight /></el-icon></router-link></header>
      <el-table :data="hotShows" v-loading="loading" class="admin-table" empty-text="暂无演出数据">
        <el-table-column prop="name" label="演出" min-width="220" />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="venue" label="场馆" min-width="180" />
        <el-table-column label="状态" width="100"><template #default="{ row }"><span :class="['table-status', row.status === 1 ? 'is-on' : 'is-off']">{{ row.status === 1 ? '在售' : '下架' }}</span></template></el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { computed, markRaw, onMounted, reactive, ref } from 'vue';
import { ArrowRight, Clock, Document, Refresh, Ticket, Tickets } from '@element-plus/icons-vue';
import http from '../../api/http';

const loading = ref(false);
const refreshError = ref('');
const hotShows = ref([]);
const updatedAt = ref('--:--');
const stats = reactive({ showCount: 0, ticketCount: 0, seckillCount: 0, orderCount: 0 });
const onSaleCount = computed(() => hotShows.value.filter((show) => show.status === 1).length);
const apiState = computed(() => {
  if (loading.value) return { label: '检查中', className: 'status-pending' };
  if (refreshError.value) return { label: '不可用', className: 'status-error' };
  return { label: '正常', className: 'status-ok' };
});
const metrics = computed(() => [
  { label: '演出总数', value: stats.showCount, note: '内容库存', to: '/admin/shows' },
  { label: '票档总数', value: stats.ticketCount, note: '价格与库存', to: '/admin/tickets' },
  { label: '秒杀场次', value: stats.seckillCount, note: '活动配置', to: '/admin/seckill' },
  { label: '订单总数', value: stats.orderCount, note: '交易记录', to: '/admin/orders' }
]);
const actions = [
  { title: '演出管理', description: '上下架、场馆和演出信息', to: '/admin/shows', icon: markRaw(Tickets) },
  { title: '票档管理', description: '价格、座区和可售库存', to: '/admin/tickets', icon: markRaw(Ticket) },
  { title: '秒杀管理', description: '活动场次与库存预热', to: '/admin/seckill', icon: markRaw(Clock) },
  { title: '订单管理', description: '订单状态与交易查询', to: '/admin/orders', icon: markRaw(Document) }
];

const loadStats = async () => { const res = await http.get('/show/stats'); if (res.code !== 200) throw new Error(res.message || '统计数据加载失败'); Object.assign(stats, res.data || {}); };
const loadHotShows = async () => { const res = await http.get('/show/hot', { params: { limit: 6 } }); if (res.code !== 200) throw new Error(res.message || '热门演出加载失败'); hotShows.value = res.data || []; };
const refreshAll = async () => {
  loading.value = true;
  refreshError.value = '';
  try {
    const results = await Promise.allSettled([loadStats(), loadHotShows()]);
    if (results.some((result) => result.status === 'rejected')) {
      refreshError.value = '部分数据加载失败，请检查网关与后端服务。';
    }
    updatedAt.value = new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit' }).format(new Date());
  } finally {
    loading.value = false;
  }
};
onMounted(refreshAll);
</script>

<style scoped>
.admin-page { max-width: 1280px; }
.admin-heading { display: flex; align-items: end; justify-content: space-between; gap: 24px; margin-bottom: 34px; }
.context-label, .section-kicker { display: block; margin-bottom: 8px; color: var(--brand); font-size: 12px; font-weight: 800; }
.admin-heading h1 { margin-bottom: 8px; font-size: 38px; }.admin-heading p { color: var(--ink-2); }
.metric-strip { display: grid; grid-template-columns: repeat(4, 1fr); border-block: 1px solid var(--line); }
.metric-strip button { min-height: 142px; padding: 22px 24px; border: 0; border-right: 1px solid var(--line); text-align: left; background: transparent; cursor: pointer; }.metric-strip button:last-child { border-right: 0; }.metric-strip button:hover { background: var(--surface); }
.metric-strip span, .metric-strip small { display: block; color: var(--ink-3); }.metric-strip strong { display: block; margin: 9px 0 5px; font-size: 34px; line-height: 1; }
.admin-grid { display: grid; grid-template-columns: 2fr 1fr; gap: 42px; margin-top: 46px; }
.admin-section > header { display: flex; align-items: end; justify-content: space-between; gap: 20px; padding-bottom: 16px; border-bottom: 1px solid var(--line); }.admin-section h2 { font-size: 21px; }.admin-section header a { display: inline-flex; align-items: center; gap: 6px; color: var(--brand); font-weight: 700; }
.actions-section nav { display: grid; grid-template-columns: repeat(2, 1fr); }
.actions-section a { min-height: 92px; display: grid; grid-template-columns: 34px 1fr 18px; align-items: center; gap: 12px; padding: 18px 16px; border-bottom: 1px solid var(--line); }.actions-section a:nth-child(odd) { border-right: 1px solid var(--line); }.actions-section a:hover { background: var(--surface); }.actions-section a > .el-icon:first-child { color: var(--brand); font-size: 22px; }.actions-section strong, .actions-section small { display: block; }.actions-section small { margin-top: 3px; color: var(--ink-3); }
.status-section dl { margin: 0; }.status-section dl div { min-height: 54px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--line); }.status-section dt { color: var(--ink-2); }.status-section dd { margin: 0; font-weight: 700; }.status-ok { color: var(--success); }.status-pending { color: var(--warning); }.status-error { color: var(--danger); }.refresh-error { margin-top: 14px; color: var(--danger); font-size: 13px; }
.table-section { margin-top: 48px; }.admin-table { margin-top: 12px; }.table-status { font-weight: 700; }.table-status.is-on { color: var(--success); }.table-status.is-off { color: var(--ink-3); }
@media (max-width: 900px) { .metric-strip { grid-template-columns: repeat(2, 1fr); }.metric-strip button:nth-child(2) { border-right: 0; }.admin-grid { grid-template-columns: 1fr; } }
@media (max-width: 600px) { .admin-heading { align-items: start; flex-direction: column; }.admin-heading h1 { font-size: 30px; }.metric-strip button { min-height: 116px; padding: 16px; }.metric-strip strong { font-size: 28px; }.actions-section nav { grid-template-columns: 1fr; }.actions-section a:nth-child(odd) { border-right: 0; } }
</style>
