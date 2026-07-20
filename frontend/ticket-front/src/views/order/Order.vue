<template>
  <main class="orders-page page-shell">
    <header class="page-heading orders-heading">
      <div>
        <p class="eyebrow">订单中心</p>
        <h1>我的订单</h1>
        <p>查看购票状态、完成支付或管理已取消的订单。</p>
      </div>
      <button type="button" class="ui-button refresh-button" :disabled="loading" @click="loadOrders">
        <el-icon><RefreshRight /></el-icon>
        刷新列表
      </button>
    </header>

    <nav class="order-tabs" aria-label="订单状态">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        type="button"
        :class="['tab-btn', { active: activeTab === tab.value }]"
        :aria-pressed="activeTab === tab.value"
        @click="activeTab = tab.value"
      >
        {{ tab.label }}
      </button>
    </nav>

    <section v-if="loading" class="order-list" aria-label="订单加载中" aria-busy="true">
      <div v-for="item in 4" :key="item" class="order-row order-row--skeleton">
        <div class="skeleton skeleton-line skeleton-line--short"></div>
        <div class="skeleton skeleton-line skeleton-line--title"></div>
        <div class="skeleton skeleton-line skeleton-line--medium"></div>
      </div>
    </section>

    <section v-else-if="error" class="state-block order-state" role="alert">
      <div>
        <h2>订单暂时无法加载</h2>
        <p>{{ error }}</p>
        <button type="button" class="ui-button ui-button--primary" @click="loadOrders">重新加载</button>
      </div>
    </section>

    <section v-else-if="orders.length === 0" class="state-block order-state">
      <div>
        <h2>{{ emptyTitle }}</h2>
        <p>{{ emptyDescription }}</p>
        <RouterLink class="ui-button ui-button--primary" to="/">浏览演出</RouterLink>
      </div>
    </section>

    <section v-else class="order-list" aria-label="订单列表">
      <article v-for="order in orders" :key="order.id" class="order-row">
        <div class="order-row__meta">
          <span class="order-number">订单 {{ order.orderNo }}</span>
          <span class="order-kind">{{ order.isSeckill === 1 ? '秒杀订单' : '普通订单' }}</span>
        </div>

        <div class="order-row__show">
          <h2>{{ order.show?.name || '演出信息待确认' }}</h2>
          <p>
            {{ order.show?.startTime ? formatTime(order.show.startTime) : '场次时间待确认' }}
            <span v-if="order.show?.venue">{{ order.show.venue }}</span>
          </p>
        </div>

        <div class="order-row__ticket">
          <span>{{ order.ticket?.name || '票档待确认' }}</span>
          <span>共 {{ order.quantity }} 张</span>
        </div>

        <div class="order-row__amount">
          <span class="order-row__label">实付金额</span>
          <strong>¥{{ formatAmount(order.totalAmount) }}</strong>
          <del v-if="originalAmount(order)">¥{{ formatAmount(originalAmount(order)) }}</del>
        </div>

        <div class="order-row__status">
          <span class="status-marker" :class="getStatusClass(order.status)"></span>
          <span>{{ getStatusName(order) }}</span>
        </div>

        <div class="order-row__actions">
          <button
            v-if="order.status === 0"
            type="button"
            class="ui-button ui-button--primary"
            :disabled="isProcessing(order.orderNo)"
            @click="payOrder(order.orderNo)"
          >
            {{ isProcessing(order.orderNo) ? '正在处理' : '立即支付' }}
          </button>
          <button
            v-if="order.status === 0"
            type="button"
            class="ui-button"
            :disabled="isProcessing(order.orderNo)"
            @click="cancelOrder(order)"
          >
            取消
          </button>
          <button type="button" class="detail-link" @click="goToOrderDetail(order.orderNo)">
            详情
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
      </article>
    </section>
  </main>
</template>

<script>
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowRight, RefreshRight } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import dayjs from 'dayjs';
import { orderApi } from '../../api/order';
import { useUserStore } from '../../store/user';

const STATUS_NAMES = { 0: '待支付', 1: '已支付', 2: '已取消', 3: '已退款', 4: '已完成' };

export default {
  name: 'Order',
  components: { ArrowRight, RefreshRight },
  setup() {
    const orders = ref([]);
    const loading = ref(true);
    const error = ref('');
    const activeTab = ref('all');
    const processingOrderNo = ref('');
    const userStore = useUserStore();
    const router = useRouter();

    const tabs = [
      { label: '全部', value: 'all' },
      { label: '待支付', value: 'pending' },
      { label: '已支付', value: 'paid' },
      { label: '已取消', value: 'cancelled' }
    ];

    const emptyTitle = computed(() => activeTab.value === 'all' ? '还没有订单' : `没有${tabs.find((tab) => tab.value === activeTab.value)?.label || ''}订单`);
    const emptyDescription = computed(() => activeTab.value === 'all' ? '浏览演出后，订单会显示在这里。' : '切换其他状态，或浏览新的演出。');

    const loadOrders = async () => {
      if (!userStore.isLoggedIn) {
        error.value = '请先登录后查看订单。';
        loading.value = false;
        return;
      }

      try {
        loading.value = true;
        error.value = '';
        const status = { all: undefined, pending: 0, paid: 1, cancelled: 2 }[activeTab.value];
        const res = await orderApi.getOrderList({ page: 1, size: 50, status });
        if (res.code !== 200) throw new Error(res.message || '订单加载失败');
        orders.value = res.data?.records || [];
      } catch (err) {
        error.value = err.message || '加载失败，请稍后重试。';
        console.error(err);
      } finally {
        loading.value = false;
      }
    };

    const formatTime = (time) => dayjs(time).format('YYYY-MM-DD HH:mm');
    const formatAmount = (amount) => Number(amount || 0).toFixed(2);
    const originalAmount = (order) => order.isSeckill === 1 && order.seckillPrice && order.ticket?.price
      ? Number(order.ticket.price) * Number(order.quantity || 0)
      : null;
    const getStatusName = (order) => order.statusName || STATUS_NAMES[order.status] || '状态待确认';
    const getStatusClass = (status) => ({ 0: 'status-pending', 1: 'status-paid', 2: 'status-cancelled', 3: 'status-refunded', 4: 'status-complete' }[status] || 'status-unknown');
    const isProcessing = (orderNo) => processingOrderNo.value === orderNo;

    const payOrder = async (orderNo) => {
      if (processingOrderNo.value) return;
      processingOrderNo.value = orderNo;
      try {
        const res = await orderApi.payOrder(orderNo);
        if (res.code !== 200) throw new Error(res.message || '支付失败');
        ElMessage.success('支付成功');
        await loadOrders();
      } catch (err) {
        ElMessage.error(err.message || '支付失败，请重试');
      } finally {
        processingOrderNo.value = '';
      }
    };

    const cancelOrder = async (order) => {
      if (processingOrderNo.value || !confirm('确定要取消这笔订单吗？')) return;
      processingOrderNo.value = order.orderNo;
      try {
        const res = await orderApi.cancelOrder(order.id);
        if (res.code !== 200) throw new Error(res.message || '取消失败');
        ElMessage.success('订单已取消');
        await loadOrders();
      } catch (err) {
        ElMessage.error(err.message || '取消失败，请重试');
      } finally {
        processingOrderNo.value = '';
      }
    };

    const goToOrderDetail = (orderNo) => router.push({ name: 'OrderDetail', params: { orderNo } });

    watch(activeTab, loadOrders);
    onMounted(loadOrders);

    return { orders, loading, error, activeTab, tabs, emptyTitle, emptyDescription, loadOrders, formatTime, formatAmount, originalAmount, getStatusName, getStatusClass, isProcessing, payOrder, cancelOrder, goToOrderDetail };
  }
};
</script>

<style scoped>
.orders-page { min-height: 60vh; }
.eyebrow { margin-bottom: 8px; color: var(--brand); font-size: 12px; font-weight: 700; }
.orders-heading { margin-bottom: 20px; }
.refresh-button { flex: 0 0 auto; }
.order-tabs { display: flex; gap: 4px; overflow-x: auto; margin-bottom: 18px; border-bottom: 1px solid var(--line); }
.tab-btn { min-height: 42px; padding: 0 14px; border: 0; border-bottom: 2px solid transparent; background: transparent; color: var(--ink-2); font-weight: 650; white-space: nowrap; cursor: pointer; }
.tab-btn:hover, .tab-btn.active { color: var(--ink); }
.tab-btn.active { border-bottom-color: var(--brand); }
.order-list { border-top: 1px solid var(--line); }
.order-row { display: grid; grid-template-columns: minmax(150px, 1.25fr) minmax(220px, 2.1fr) minmax(120px, 1fr) minmax(105px, .8fr) minmax(88px, .72fr) minmax(188px, auto); gap: 18px; align-items: center; padding: 21px 4px; border-bottom: 1px solid var(--line); background: var(--surface-2); }
.order-row:hover { background: var(--surface); }
.order-row__meta, .order-row__ticket, .order-row__amount, .order-row__status { display: grid; gap: 4px; }
.order-number { overflow: hidden; color: var(--ink-2); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.order-kind, .order-row__label { color: var(--ink-3); font-size: 12px; }
.order-row__show h2 { overflow: hidden; font-size: 16px; font-weight: 720; text-overflow: ellipsis; white-space: nowrap; }
.order-row__show p, .order-row__ticket { color: var(--ink-2); font-size: 13px; }
.order-row__show p span::before { content: ' / '; color: var(--line-strong); }
.order-row__amount strong { color: var(--brand); font-size: 16px; }
.order-row__amount del { color: var(--ink-3); font-size: 12px; }
.order-row__status { grid-template-columns: 7px auto; align-items: center; color: var(--ink-2); font-size: 13px; }
.status-marker { width: 7px; height: 7px; border-radius: 50%; background: var(--ink-3); }
.status-pending { background: #c6811f; }.status-paid, .status-complete { background: #27865c; }.status-cancelled { background: #8c939a; }.status-refunded { background: #3e78ad; }
.order-row__actions { display: flex; justify-content: end; gap: 8px; align-items: center; }
.detail-link { display: inline-flex; align-items: center; gap: 3px; min-height: 40px; padding: 0 4px; border: 0; background: transparent; color: var(--ink-2); cursor: pointer; }
.detail-link:hover { color: var(--brand); }
.order-state h2 { margin-bottom: 8px; font-size: 18px; }.order-state p { margin-bottom: 18px; }
.order-row--skeleton { grid-template-columns: 1fr 2fr 1fr; min-height: 86px; }.skeleton-line { height: 12px; border-radius: 3px; }.skeleton-line--short { width: 70%; }.skeleton-line--title { width: 90%; }.skeleton-line--medium { width: 60%; }
@media (max-width: 1040px) { .order-row { grid-template-columns: minmax(180px, 2fr) minmax(140px, 1fr) minmax(100px, .8fr) auto; }.order-row__meta { display: none; }.order-row__ticket { display: none; } }
@media (max-width: 680px) { .orders-heading { margin-bottom: 14px; }.refresh-button { width: 40px; padding: 0; font-size: 0; }.refresh-button .el-icon { margin: 0; font-size: 16px; }.order-row { grid-template-columns: minmax(0, 1fr) auto; gap: 12px; padding: 18px 0; }.order-row__show { grid-column: 1 / -1; }.order-row__amount { grid-column: 1; }.order-row__status { grid-column: 2; }.order-row__actions { grid-column: 1 / -1; justify-content: start; }.order-row__actions .ui-button { flex: 1; }.detail-link { padding-right: 12px; }.order-row--skeleton { grid-template-columns: 1fr; }.order-row--skeleton .skeleton-line { width: 100%; } }
</style>
