<template>
  <main class="order-detail-page page-shell">
    <button type="button" class="back-link" @click="goBack">
      <el-icon><ArrowLeft /></el-icon>
      返回订单
    </button>

    <section v-if="loading" class="detail-skeleton" aria-label="订单详情加载中" aria-busy="true">
      <div class="skeleton detail-skeleton__heading"></div>
      <div class="skeleton detail-skeleton__body"></div>
      <div class="skeleton detail-skeleton__body"></div>
    </section>

    <section v-else-if="error" class="state-block detail-state" role="alert">
      <div><h1>订单详情暂时无法加载</h1><p>{{ error }}</p><button type="button" class="ui-button ui-button--primary" @click="loadOrderDetail">重新加载</button></div>
    </section>

    <template v-else>
      <header class="detail-hero">
        <div>
          <p class="detail-kicker">订单 {{ order.orderNo }}</p>
          <h1>{{ getStatusName(order) }}</h1>
          <p>创建于 {{ formatTime(order.createTime) }}</p>
        </div>
        <div class="detail-status" :class="getStatusClass(order.status)"><span></span>{{ getStatusName(order) }}</div>
      </header>

      <div class="detail-layout">
        <section class="detail-main">
          <section class="detail-section show-section">
            <p class="section-label">演出信息</p>
            <div v-if="order.show" class="show-fact">
              <img v-if="order.show.poster" :src="order.show.poster" :alt="order.show.name" class="show-poster" />
              <div class="show-fact__copy">
                <h2>{{ order.show.name }}</h2>
                <p>{{ formatTime(order.show.startTime) }}</p>
                <p>{{ order.show.venue || '场馆待确认' }}<span v-if="order.show.city"> / {{ order.show.city }}</span></p>
              </div>
            </div>
            <p v-else class="muted">演出信息待确认。</p>
          </section>

          <section class="detail-section">
            <p class="section-label">票档信息</p>
            <div class="ticket-fact">
              <div><h2>{{ order.ticket?.name || '票档待确认' }}</h2><p>数量 {{ order.quantity || 0 }} 张</p></div>
              <div class="ticket-fact__price"><strong>¥{{ formatAmount(order.totalAmount) }}</strong><del v-if="originalAmount">¥{{ formatAmount(originalAmount) }}</del></div>
            </div>
          </section>
        </section>

        <aside class="detail-summary" aria-label="订单摘要">
          <p class="section-label">订单摘要</p>
          <dl>
            <div><dt>订单编号</dt><dd>{{ order.orderNo }}</dd></div>
            <div><dt>订单类型</dt><dd>{{ order.isSeckill === 1 ? '秒杀订单' : '普通订单' }}</dd></div>
            <div><dt>创建时间</dt><dd>{{ formatTime(order.createTime) }}</dd></div>
            <div v-if="order.payTime"><dt>支付时间</dt><dd>{{ formatTime(order.payTime) }}</dd></div>
            <div><dt>订单状态</dt><dd>{{ getStatusName(order) }}</dd></div>
          </dl>
          <div v-if="order.status === 0" class="summary-actions">
            <button type="button" class="ui-button ui-button--primary" :disabled="processing" @click="payOrder">{{ processing ? '正在处理' : '立即支付' }}</button>
            <button type="button" class="ui-button" :disabled="processing" @click="cancelOrder">取消订单</button>
          </div>
        </aside>
      </div>
    </template>
  </main>
</template>

<script>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowLeft } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import dayjs from 'dayjs';
import { orderApi } from '../../api/order';

const STATUS_NAMES = { 0: '待支付', 1: '已支付', 2: '已取消', 3: '已退款', 4: '已完成' };

export default {
  name: 'OrderDetail',
  components: { ArrowLeft },
  props: { orderNo: { type: String, required: true } },
  setup(props) {
    const order = ref({});
    const loading = ref(true);
    const error = ref('');
    const processing = ref(false);
    const router = useRouter();
    const originalAmount = computed(() => order.value.isSeckill === 1 && order.value.seckillPrice && order.value.ticket?.price
      ? Number(order.value.ticket.price) * Number(order.value.quantity || 0)
      : null);

    const loadOrderDetail = async () => {
      try {
        loading.value = true;
        error.value = '';
        const res = await orderApi.getByOrderNo(props.orderNo);
        if (res.code !== 200) throw new Error(res.message || '订单详情加载失败');
        order.value = res.data || {};
      } catch (err) {
        error.value = err.message || '加载失败，请稍后重试。';
        console.error(err);
      } finally { loading.value = false; }
    };

    const formatTime = (time) => time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '待确认';
    const formatAmount = (amount) => Number(amount || 0).toFixed(2);
    const getStatusName = (value) => value.statusName || STATUS_NAMES[value.status] || '状态待确认';
    const getStatusClass = (status) => ({ 0: 'status-pending', 1: 'status-paid', 2: 'status-cancelled', 3: 'status-refunded', 4: 'status-complete' }[status] || 'status-unknown');

    const payOrder = async () => {
      if (processing.value) return;
      processing.value = true;
      try {
        const res = await orderApi.payOrder(order.value.orderNo);
        if (res.code !== 200) throw new Error(res.message || '支付失败');
        ElMessage.success('支付成功');
        await loadOrderDetail();
      } catch (err) { ElMessage.error(err.message || '支付失败，请重试'); }
      finally { processing.value = false; }
    };

    const cancelOrder = async () => {
      if (processing.value || !confirm('确定要取消这笔订单吗？')) return;
      processing.value = true;
      try {
        const res = await orderApi.cancelOrder(order.value.id);
        if (res.code !== 200) throw new Error(res.message || '取消失败');
        ElMessage.success('订单已取消');
        await loadOrderDetail();
      } catch (err) { ElMessage.error(err.message || '取消失败，请重试'); }
      finally { processing.value = false; }
    };

    const goBack = () => router.push({ name: 'Order' });
    onMounted(loadOrderDetail);
    return { order, loading, error, processing, originalAmount, loadOrderDetail, formatTime, formatAmount, getStatusName, getStatusClass, payOrder, cancelOrder, goBack };
  }
};
</script>

<style scoped>
.order-detail-page { min-height: 60vh; }.back-link { display: inline-flex; align-items: center; gap: 6px; min-height: 36px; margin-bottom: 20px; padding: 0; border: 0; background: transparent; color: var(--ink-2); cursor: pointer; }.back-link:hover { color: var(--brand); }
.detail-hero { display: flex; justify-content: space-between; align-items: end; gap: 24px; padding: 0 0 28px; border-bottom: 1px solid var(--line); }.detail-kicker, .section-label { color: var(--ink-3); font-size: 12px; font-weight: 700; }.detail-kicker { margin-bottom: 8px; }.detail-hero h1 { font-size: clamp(28px, 4vw, 42px); }.detail-hero p:not(.detail-kicker) { margin-top: 10px; color: var(--ink-2); }
.detail-status { display: inline-flex; align-items: center; gap: 7px; color: var(--ink-2); font-weight: 650; white-space: nowrap; }.detail-status span { width: 8px; height: 8px; border-radius: 50%; background: var(--ink-3); }.detail-status.status-pending span { background: #c6811f; }.detail-status.status-paid span, .detail-status.status-complete span { background: #27865c; }.detail-status.status-cancelled span { background: #8c939a; }.detail-status.status-refunded span { background: #3e78ad; }
.detail-layout { display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 48px; padding-top: 32px; }.detail-main { min-width: 0; }.detail-section { padding: 0 0 28px; margin-bottom: 28px; border-bottom: 1px solid var(--line); }.section-label { margin-bottom: 16px; }.show-fact { display: flex; gap: 20px; align-items: center; }.show-poster { width: 112px; aspect-ratio: 2 / 3; object-fit: cover; border-radius: var(--radius); background: var(--surface-3); }.show-fact__copy h2, .ticket-fact h2 { font-size: 20px; }.show-fact__copy p, .ticket-fact p, .muted { margin-top: 8px; color: var(--ink-2); }
.ticket-fact { display: flex; justify-content: space-between; gap: 16px; align-items: end; }.ticket-fact__price { display: grid; justify-items: end; gap: 4px; }.ticket-fact__price strong { color: var(--brand); font-size: 22px; }.ticket-fact__price del { color: var(--ink-3); font-size: 13px; }
.detail-summary { align-self: start; padding: 20px 0; border-top: 2px solid var(--ink); border-bottom: 1px solid var(--line); }.detail-summary dl { margin: 0; }.detail-summary dl > div { display: grid; grid-template-columns: 88px minmax(0, 1fr); gap: 16px; padding: 12px 0; border-bottom: 1px solid var(--line); }.detail-summary dt { color: var(--ink-2); font-size: 13px; }.detail-summary dd { min-width: 0; margin: 0; color: var(--ink); font-size: 13px; overflow-wrap: anywhere; text-align: right; }.summary-actions { display: grid; gap: 8px; margin-top: 20px; }.summary-actions .ui-button { width: 100%; }.detail-skeleton { display: grid; gap: 18px; }.detail-skeleton__heading { width: min(50%, 360px); height: 48px; border-radius: var(--radius); }.detail-skeleton__body { height: 180px; border-radius: var(--radius); }.detail-state h1 { margin-bottom: 8px; font-size: 20px; }.detail-state p { margin-bottom: 18px; }
@media (max-width: 760px) { .detail-hero { align-items: start; flex-direction: column; gap: 16px; }.detail-layout { grid-template-columns: 1fr; gap: 0; padding-top: 24px; }.detail-summary { margin-bottom: 32px; }.show-fact { align-items: start; }.show-poster { width: 88px; }.ticket-fact { align-items: start; flex-direction: column; }.ticket-fact__price { justify-items: start; } }
</style>
