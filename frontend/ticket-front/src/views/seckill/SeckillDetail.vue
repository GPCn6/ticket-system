<template>
  <main class="seckill-detail page-shell">
    <section v-if="loading" class="seckill-skeleton" aria-label="正在加载抢票场次">
      <div class="skeleton seckill-skeleton__title"></div>
      <div class="skeleton seckill-skeleton__line"></div>
      <div class="skeleton seckill-skeleton__panel"></div>
    </section>

    <section v-else-if="error" class="state-block seckill-state">
      <div>
        <h1>抢票场次未能加载</h1>
        <p>{{ error }}</p>
        <button class="ui-button ui-button--primary" type="button" @click="loadSessionDetail">重新加载</button>
      </div>
    </section>

    <section v-else class="seckill-layout">
      <section class="seckill-brief" aria-labelledby="seckill-title">
        <p class="section-kicker">限时抢票</p>
        <h1 id="seckill-title">{{ session.show?.name || '抢票场次' }}</h1>
        <div class="session-facts">
          <div>
            <span>抢票时间</span>
            <strong>{{ formatTime(session.startTime) }} 至 {{ formatTime(session.endTime) }}</strong>
          </div>
          <div>
            <span>当前库存</span>
            <strong :class="stock > 0 ? 'stock-available' : 'stock-sold-out'">{{ stock > 0 ? `剩余 ${stock} 张` : '已售罄' }}</strong>
          </div>
        </div>

        <div class="ticket-summary">
          <p>{{ session.ticket?.name || '指定票档' }}</p>
          <div>
            <strong>¥{{ formatPrice(session.seckillPrice) }}</strong>
            <del v-if="session.ticket?.price">¥{{ formatPrice(session.ticket.price) }}</del>
          </div>
        </div>
      </section>

      <section class="seckill-flow" aria-label="抢票操作">
        <div class="flow-status" :class="`flow-status--${sessionState}`">
          <span>{{ stateLabel }}</span>
          <strong>{{ stateDetail }}</strong>
        </div>

        <div v-if="['upcoming', 'active'].includes(sessionState)" class="countdown" aria-live="polite">
          <span>{{ sessionState === 'upcoming' ? '距开始' : '距结束' }}</span>
          <strong>{{ countdown }}</strong>
        </div>

        <div class="quantity-control">
          <div>
            <span>购买数量</span>
            <small>每人限购 2 张</small>
          </div>
          <div class="stepper" aria-label="选择购买数量">
            <button type="button" aria-label="减少数量" :disabled="quantity <= 1 || isProcessing" @click="decreaseQuantity">−</button>
            <output>{{ quantity }}</output>
            <button type="button" aria-label="增加数量" :disabled="quantity >= 2 || isProcessing" @click="increaseQuantity">+</button>
          </div>
        </div>

        <div v-if="processingMessage" class="processing-message" :class="{ 'processing-message--error': processingStage === 'error' }" aria-live="polite">
          {{ processingMessage }}
        </div>

        <button class="ui-button ui-button--primary seckill-button" type="button" :disabled="isActionDisabled" @click="doSeckill">
          {{ actionLabel }}
        </button>
      </section>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { seckillApi } from '../../api/seckill';
import { orderApi } from '../../api/order';
import { useUserStore } from '../../store/user';
import { createPollingLifecycle, getSeckillSessionState } from '../../utils/ticketing-state';
import dayjs from 'dayjs';
import { ElMessage } from 'element-plus';

const props = defineProps({
  sessionId: { type: String, required: true }
});

const router = useRouter();
const userStore = useUserStore();
const session = ref({});
const loading = ref(true);
const error = ref('');
const quantity = ref(1);
const isProcessing = ref(false);
const processingStage = ref('idle');
const processingMessage = ref('');
const now = ref(Date.now());
let countdownTimer = null;
const pollingLifecycle = createPollingLifecycle();

const stock = computed(() => Number(session.value.stock || 0));
const startAt = computed(() => dayjs(session.value.startTime));
const endAt = computed(() => dayjs(session.value.endTime));
const sessionState = computed(() => getSeckillSessionState(session.value, now.value));
const stateLabel = computed(() => ({ upcoming: '未开始', active: '抢票进行中', soldout: '已售罄', ended: '已结束', closed: '场次已关闭', unavailable: '场次异常' })[sessionState.value]);
const stateDetail = computed(() => ({
  upcoming: '场次开始后可提交抢票请求',
  active: '库存实时变化，以订单创建结果为准',
  soldout: '该场次当前已无可售库存',
  ended: '本场抢票已结束',
  closed: '运营方已关闭本场抢票，请选择其他场次',
  unavailable: '请稍后重新加载场次信息'
})[sessionState.value]);
const countdown = computed(() => {
  const target = sessionState.value === 'upcoming' ? startAt.value : endAt.value;
  const remaining = Math.max(0, target.diff(dayjs(now.value), 'second'));
  const hours = String(Math.floor(remaining / 3600)).padStart(2, '0');
  const minutes = String(Math.floor((remaining % 3600) / 60)).padStart(2, '0');
  const seconds = String(remaining % 60).padStart(2, '0');
  return `${hours}:${minutes}:${seconds}`;
});
const isActionDisabled = computed(() => isProcessing.value || ['upcoming', 'ended', 'soldout', 'closed', 'unavailable'].includes(sessionState.value));
const actionLabel = computed(() => {
  if (isProcessing.value) return processingStage.value === 'waiting' ? '正在创建订单' : '正在提交请求';
  return ({ upcoming: '等待开售', ended: '抢票已结束', soldout: '该票档已售罄', closed: '场次已关闭', unavailable: '场次暂不可用', active: '立即抢票' })[sessionState.value];
});

const formatTime = (time) => (time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '待公布');
const formatPrice = (value) => Number(value || 0).toFixed(2);
const decreaseQuantity = () => { if (quantity.value > 1 && !isProcessing.value) quantity.value--; };
const increaseQuantity = () => { if (quantity.value < 2 && !isProcessing.value) quantity.value++; };

const loadSessionDetail = async () => {
  try {
    loading.value = true;
    error.value = '';
    const data = await seckillApi.getSeckillDetail(props.sessionId);
    if (!pollingLifecycle.active) return;
    if (data.code !== 200 || !data.data) throw new Error(data.message || '抢票场次加载失败');
    session.value = data.data;
  } catch (err) {
    if (!pollingLifecycle.active) return;
    error.value = err.message || '加载失败，请重试';
    console.error(err);
  } finally {
    if (pollingLifecycle.active) loading.value = false;
  }
};

const goToOrderList = async () => {
  if (!pollingLifecycle.active) return;
  processingStage.value = 'idle';
  isProcessing.value = false;
  await router.push({ name: 'Order' });
};

const waitForOrderAndPay = async (requestId) => {
  if (!pollingLifecycle.active) return;
  processingStage.value = 'waiting';
  processingMessage.value = '请求已接收，正在创建订单，请勿重复提交。';
  let retries = 0;
  const maxRetries = 30;
  const pollInterval = 2000;

  const poll = async () => {
    if (!pollingLifecycle.active) return;
    try {
      const res = await orderApi.getByOrderNo(requestId);
      if (!pollingLifecycle.active) return;
      if (res.code === 200 && res.data?.orderNo && res.data.isSeckill === 1) {
        processingMessage.value = '订单已创建，正在跳转支付。';
        await router.push({ name: 'OrderDetail', params: { orderNo: res.data.orderNo } });
        return;
      }
    } catch (pollError) {
      if (!pollingLifecycle.active) return;
      console.error(pollError);
    }

    if (!pollingLifecycle.active) return;
    retries += 1;
    if (retries < maxRetries) {
      pollingLifecycle.schedule(poll, pollInterval);
    } else {
      processingMessage.value = '订单仍在处理中，请稍后在订单列表查看。';
      ElMessage.warning(processingMessage.value);
      await goToOrderList();
    }
  };

  pollingLifecycle.schedule(poll, pollInterval);
};

const doSeckill = async () => {
  if (!userStore.isLoggedIn) {
    await router.push({ name: 'Login', query: { redirect: `/seckill/${props.sessionId}` } });
    return;
  }
  if (isActionDisabled.value) return;

  try {
    isProcessing.value = true;
    processingStage.value = 'submitting';
    processingMessage.value = '正在提交抢票请求，请勿刷新页面。';
    const result = await seckillApi.executeSeckill({
      sessionId: Number(props.sessionId),
      quantity: quantity.value
    });
    if (!pollingLifecycle.active) return;
    if (result.code !== 200) {
      processingStage.value = 'error';
      processingMessage.value = result.message || '抢票失败，请稍后重试。';
      ElMessage.error(processingMessage.value);
      isProcessing.value = false;
      await loadSessionDetail();
      return;
    }
    const requestId = result.data?.requestId || result.data?.orderNo;
    if (!requestId) {
      processingStage.value = 'error';
      processingMessage.value = '请求已提交，但未获取到订单编号，请在订单列表查看。';
      ElMessage.warning(processingMessage.value);
      isProcessing.value = false;
      return;
    }
    await waitForOrderAndPay(requestId);
  } catch (err) {
    if (!pollingLifecycle.active) return;
    processingStage.value = 'error';
    processingMessage.value = err.message || '抢票失败，请稍后重试。';
    ElMessage.error(processingMessage.value);
    console.error(err);
    isProcessing.value = false;
  }
};

onMounted(() => {
  loadSessionDetail();
  countdownTimer = setInterval(() => { now.value = Date.now(); }, 1000);
});

onUnmounted(() => {
  pollingLifecycle.cancel();
  if (countdownTimer) clearInterval(countdownTimer);
});
</script>

<style scoped>
.seckill-detail { min-height: 640px; }
.seckill-layout { display: grid; grid-template-columns: minmax(0, 1fr) minmax(320px, 410px); gap: clamp(36px, 7vw, 104px); align-items: center; max-width: 1060px; margin: 0 auto; }
.section-kicker { margin-bottom: 14px; color: var(--ink-3); font-size: 12px; font-weight: 700; }
.seckill-brief h1 { max-width: 13ch; font-size: clamp(32px, 4vw, 52px); font-weight: 760; }
.session-facts { margin-top: 34px; border-top: 1px solid var(--line); }
.session-facts > div { display: grid; grid-template-columns: 94px minmax(0, 1fr); gap: 16px; padding: 15px 0; border-bottom: 1px solid var(--line); }
.session-facts span { color: var(--ink-3); }
.session-facts strong { overflow-wrap: anywhere; }
.stock-available { color: var(--success); }
.stock-sold-out { color: var(--danger); }
.ticket-summary { display: flex; align-items: end; justify-content: space-between; gap: 20px; margin-top: 28px; padding-top: 18px; border-top: 1px solid var(--line); }
.ticket-summary p { color: var(--ink-2); font-weight: 650; }
.ticket-summary div { display: grid; justify-items: end; gap: 4px; }
.ticket-summary strong { color: var(--brand); font-size: 28px; line-height: 1; }
.ticket-summary del { color: var(--ink-3); font-size: 13px; }

.seckill-flow { border: 1px solid var(--line); border-radius: var(--radius); background: var(--surface); }
.flow-status { padding: 20px 22px; border-bottom: 1px solid var(--line); }
.flow-status span, .flow-status strong { display: block; }
.flow-status span { color: var(--ink-3); font-size: 12px; font-weight: 700; }
.flow-status strong { margin-top: 4px; font-size: 15px; }
.flow-status--active { box-shadow: inset 3px 0 0 var(--brand); }
.flow-status--soldout strong, .flow-status--ended strong, .flow-status--unavailable strong { color: var(--ink-2); }
.countdown { display: flex; align-items: baseline; justify-content: space-between; padding: 24px 22px; border-bottom: 1px solid var(--line); }
.countdown span { color: var(--ink-3); }
.countdown strong { color: var(--brand); font-size: 30px; letter-spacing: 1px; font-variant-numeric: tabular-nums; }
.quantity-control { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 22px; border-bottom: 1px solid var(--line); }
.quantity-control span, .quantity-control small { display: block; }
.quantity-control span { color: var(--ink); font-weight: 700; }
.quantity-control small { margin-top: 3px; color: var(--ink-3); font-size: 12px; }
.stepper { display: grid; grid-template-columns: 36px 42px 36px; align-items: center; border: 1px solid var(--line-strong); border-radius: var(--radius); overflow: hidden; }
.stepper button { height: 36px; padding: 0; border: 0; background: var(--surface); cursor: pointer; font-size: 20px; line-height: 1; }
.stepper button:hover:not(:disabled) { color: var(--brand); background: var(--brand-soft); }
.stepper button:disabled { color: var(--ink-3); background: var(--surface-2); cursor: not-allowed; }
.stepper output { display: grid; height: 36px; place-items: center; border-inline: 1px solid var(--line); font-variant-numeric: tabular-nums; }
.processing-message { margin: 18px 22px 0; color: var(--info); font-size: 13px; line-height: 1.6; }
.processing-message--error { color: var(--danger); }
.seckill-button { width: calc(100% - 44px); margin: 22px; }

.seckill-skeleton { max-width: 1060px; margin: 0 auto; }
.seckill-skeleton__title { width: min(440px, 70%); height: 54px; }
.seckill-skeleton__line { width: min(620px, 90%); height: 18px; margin-top: 30px; }
.seckill-skeleton__panel { width: 410px; max-width: 100%; min-height: 360px; margin: 52px 0 0 auto; }
.seckill-state h1 { margin-bottom: 10px; font-size: 22px; }
.seckill-state p { margin-bottom: 20px; }

@media (max-width: 760px) {
  .seckill-detail { padding-bottom: 36px; }
  .seckill-layout { grid-template-columns: 1fr; gap: 32px; }
  .seckill-brief h1 { max-width: none; font-size: 34px; }
  .seckill-flow { position: static; }
  .seckill-skeleton__panel { margin: 36px 0 0; }
}
</style>
