<template>
  <main class="show-detail page-shell">
    <section v-if="loading" class="detail-skeleton" aria-label="正在加载演出详情">
      <div class="skeleton detail-skeleton__poster"></div>
      <div class="detail-skeleton__copy">
        <div class="skeleton detail-skeleton__title"></div>
        <div class="skeleton detail-skeleton__line"></div>
        <div class="skeleton detail-skeleton__line detail-skeleton__line--short"></div>
      </div>
      <div class="skeleton detail-skeleton__tickets"></div>
    </section>

    <section v-else-if="error" class="state-block detail-state">
      <div>
        <h1>演出详情未能加载</h1>
        <p>{{ error }}</p>
        <button class="ui-button ui-button--primary" type="button" @click="loadShowDetail">重新加载</button>
      </div>
    </section>

    <section v-else class="show-layout">
      <aside class="show-poster-column">
        <div class="poster-frame">
          <img
            v-if="show.poster"
            :src="show.poster"
            :alt="show.name || '演出海报'"
            class="show-poster"
          />
          <div v-else class="poster-placeholder" aria-label="暂无演出海报">暂无海报</div>
        </div>
      </aside>

      <section class="show-facts" aria-labelledby="show-title">
        <p class="section-kicker">演出详情</p>
        <h1 id="show-title">{{ show.name || '未命名演出' }}</h1>
        <p v-if="show.description" class="show-description">{{ show.description }}</p>
        <p v-else class="show-description show-description--empty">主办方暂未提供演出介绍。</p>

        <dl class="fact-list">
          <div class="fact-row">
            <dt>城市</dt>
            <dd>{{ show.city || '待公布' }}</dd>
          </div>
          <div class="fact-row">
            <dt>场馆</dt>
            <dd>{{ show.venue || '待公布' }}</dd>
          </div>
          <div class="fact-row">
            <dt>演出时间</dt>
            <dd>{{ formatTime(show.startTime) }}<span v-if="show.endTime"> 至 {{ formatTime(show.endTime) }}</span></dd>
          </div>
        </dl>

        <section class="notice-block" aria-labelledby="notice-title">
          <h2 id="notice-title">购票提示</h2>
          <p>订单创建后请尽快完成支付。具体入场与退改规则以主办方公布信息为准。</p>
        </section>
      </section>

      <aside class="purchase-column" aria-label="票档与购票">
        <div class="purchase-panel">
          <div class="purchase-panel__heading">
            <p class="section-kicker">选择票档</p>
            <p v-if="tickets.length" class="ticket-count">共 {{ tickets.length }} 个票档</p>
          </div>

          <div v-if="ticketError" class="ticket-feedback ticket-feedback--error">
            <p>{{ ticketError }}</p>
            <button class="text-button" type="button" @click="loadShowDetail">重新加载</button>
          </div>
          <div v-else-if="!tickets.length" class="ticket-feedback">
            暂无可售票档，请留意后续开售安排。
          </div>

          <div v-else class="ticket-list" role="radiogroup" aria-label="票档选择">
            <button
              v-for="ticket in tickets"
              :key="ticket.id"
              class="ticket-option"
              :class="{ 'ticket-option--active': selectedTicket?.id === ticket.id, 'ticket-option--sold-out': !hasStock(ticket) }"
              type="button"
              role="radio"
              :aria-checked="selectedTicket?.id === ticket.id"
              :disabled="!hasStock(ticket)"
              @click="selectTicket(ticket)"
            >
              <span class="ticket-option__main">
                <strong>{{ ticket.name || '未命名票档' }}</strong>
                <small>{{ ticket.seatRange || '座位信息以实际出票为准' }}</small>
              </span>
              <span class="ticket-option__meta">
                <b>¥{{ formatPrice(ticket.price) }}</b>
                <small :class="hasStock(ticket) ? 'stock-available' : 'stock-sold-out'">
                  {{ hasStock(ticket) ? `剩余 ${ticket.availableStock} 张` : '已售罄' }}
                </small>
              </span>
            </button>
          </div>

          <div class="purchase-summary">
            <button
              ref="ticketTrigger"
              class="mobile-ticket-trigger"
              type="button"
              aria-haspopup="dialog"
              :aria-expanded="ticketSelectorOpen"
              aria-controls="ticket-selector-sheet"
              @click="openTicketSelector"
            >
              <span>
                <small>当前票档</small>
                <strong>{{ selectedTicket?.name || '选择票档' }}</strong>
              </span>
              <span class="mobile-ticket-trigger__action">
                {{ selectedTicket ? `¥${formatPrice(selectedTicket.price)}` : '查看全部' }}
                <el-icon><ArrowUp /></el-icon>
              </span>
            </button>
            <div class="purchase-summary__selection">
              <span>当前选择</span>
              <strong>{{ selectedTicket?.name || '请选择票档' }}</strong>
            </div>
            <b v-if="selectedTicket" class="purchase-summary__price">¥{{ formatPrice(selectedTicket.price) }}</b>
          </div>
          <button
            class="ui-button ui-button--primary purchase-button"
            type="button"
            :disabled="!selectedTicket || !hasStock(selectedTicket) || isCreatingOrder"
            @click="buyTicket(selectedTicket)"
          >
            {{ isCreatingOrder ? '正在创建订单' : selectedTicket && hasStock(selectedTicket) ? '立即购票' : '请选择可售票档' }}
          </button>
        </div>
      </aside>
    </section>
  </main>

  <Teleport to="body">
    <div v-if="ticketSelectorOpen" class="ticket-sheet-layer" @keydown.esc="closeTicketSelector">
      <button class="ticket-sheet-backdrop" type="button" aria-label="关闭票档选择" @click="closeTicketSelector"></button>
      <section
        id="ticket-selector-sheet"
        class="ticket-sheet"
        role="dialog"
        aria-modal="true"
        aria-labelledby="ticket-sheet-title"
      >
        <header class="ticket-sheet__header">
          <div>
            <p class="section-kicker">共 {{ tickets.length }} 个票档</p>
            <h2 id="ticket-sheet-title">选择票档</h2>
          </div>
          <button ref="ticketSheetClose" class="ticket-sheet__close" type="button" aria-label="关闭票档选择" @click="closeTicketSelector">
            <el-icon><Close /></el-icon>
          </button>
        </header>

        <div v-if="ticketError" class="ticket-feedback ticket-feedback--error">
          <p>{{ ticketError }}</p>
          <button class="text-button" type="button" @click="loadShowDetail">重新加载</button>
        </div>
        <div v-else-if="!tickets.length" class="ticket-feedback">暂无可售票档，请留意后续开售安排。</div>
        <div v-else class="ticket-list ticket-list--sheet" role="radiogroup" aria-label="移动端票档选择">
          <button
            v-for="ticket in tickets"
            :key="ticket.id"
            class="ticket-option"
            :class="{ 'ticket-option--active': selectedTicket?.id === ticket.id, 'ticket-option--sold-out': !hasStock(ticket) }"
            type="button"
            role="radio"
            :aria-checked="selectedTicket?.id === ticket.id"
            :disabled="!hasStock(ticket)"
            @click="selectTicket(ticket)"
          >
            <span class="ticket-option__main">
              <strong>{{ ticket.name || '未命名票档' }}</strong>
              <small>{{ ticket.seatRange || '座位信息以实际出票为准' }}</small>
            </span>
            <span class="ticket-option__meta">
              <b>¥{{ formatPrice(ticket.price) }}</b>
              <small :class="hasStock(ticket) ? 'stock-available' : 'stock-sold-out'">
                {{ hasStock(ticket) ? `剩余 ${ticket.availableStock} 张` : '已售罄' }}
              </small>
            </span>
          </button>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { showApi } from '../../api/show';
import { ticketApi } from '../../api/ticket';
import { orderApi } from '../../api/order';
import { useUserStore } from '../../store/user';
import { formatDateTime } from '../../utils/format';
import { ElMessage } from 'element-plus';
import { ArrowUp, Close } from '@element-plus/icons-vue';

const props = defineProps({
  id: { type: String, required: true }
});

const router = useRouter();
const userStore = useUserStore();
const show = ref({});
const tickets = ref([]);
const selectedTicket = ref(null);
const loading = ref(true);
const error = ref('');
const ticketError = ref('');
const isCreatingOrder = ref(false);
const ticketSelectorOpen = ref(false);
const ticketTrigger = ref(null);
const ticketSheetClose = ref(null);

const formatTime = (value) => (value ? formatDateTime(value) : '待公布');
const formatPrice = (value) => Number(value || 0).toFixed(2);
const hasStock = (ticket) => Number(ticket?.availableStock || 0) > 0;

const openTicketSelector = async () => {
  ticketSelectorOpen.value = true;
  await nextTick();
  ticketSheetClose.value?.focus();
};

const closeTicketSelector = async () => {
  ticketSelectorOpen.value = false;
  await nextTick();
  ticketTrigger.value?.focus();
};

const selectTicket = async (ticket) => {
  if (!hasStock(ticket)) return;
  selectedTicket.value = ticket;
  if (ticketSelectorOpen.value) await closeTicketSelector();
};

const loadShowDetail = async () => {
  try {
    loading.value = true;
    error.value = '';
    ticketError.value = '';

    const showRes = await showApi.getShowDetail(props.id);
    if (showRes.code !== 200 || !showRes.data) {
      throw new Error(showRes.message || '演出详情加载失败');
    }
    show.value = showRes.data;

    try {
      const ticketRes = await ticketApi.getByShowId(props.id);
      if (ticketRes.code !== 200) {
        throw new Error(ticketRes.message || '票档加载失败');
      }
      tickets.value = ticketRes.data || [];
      selectedTicket.value = tickets.value.find(hasStock) || null;
    } catch (ticketErr) {
      tickets.value = [];
      selectedTicket.value = null;
      ticketError.value = ticketErr.message || '票档加载失败，请稍后重试';
      console.error(ticketErr);
    }
  } catch (err) {
    error.value = err.message || '加载失败，请重试';
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const buyTicket = async (ticket) => {
  if (!ticket || isCreatingOrder.value) return;
  if (!userStore.isLoggedIn) {
    await router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } });
    return;
  }

  try {
    isCreatingOrder.value = true;
    const detailRes = await ticketApi.getTicketDetail(ticket.id);
    if (detailRes.code !== 200 || !detailRes.data) {
      ElMessage.error(detailRes.message || '票档信息异常');
      return;
    }
    if (!hasStock(detailRes.data)) {
      ElMessage.warning('该票档已售罄');
      await loadShowDetail();
      return;
    }
    const res = await orderApi.createOrder({
      showId: Number(props.id),
      ticketId: ticket.id,
      quantity: 1,
      seatId: null,
    });
    if (res.code !== 200) {
      ElMessage.error(res.message || '下单失败');
      return;
    }
    ElMessage.success('订单已创建');
    if (res.data?.orderNo) {
      await router.push({ name: 'OrderDetail', params: { orderNo: res.data.orderNo } });
    } else {
      await router.push({ name: 'Order' });
    }
  } catch (e) {
    ElMessage.error(e.message || '下单失败，请稍后重试');
  } finally {
    isCreatingOrder.value = false;
  }
};

onMounted(loadShowDetail);
</script>

<style scoped>
.show-detail { min-height: 640px; }

.show-layout {
  display: grid;
  grid-template-columns: minmax(220px, 286px) minmax(0, 1fr) minmax(290px, 344px);
  align-items: start;
  gap: clamp(24px, 3vw, 48px);
}

.show-poster-column { position: sticky; top: 92px; }
.poster-frame { overflow: hidden; border: 1px solid var(--line); background: var(--surface-3); aspect-ratio: 2 / 3; }
.show-poster { width: 100%; height: 100%; object-fit: cover; }
.poster-placeholder { display: grid; height: 100%; place-items: center; color: var(--ink-3); }

.section-kicker { margin-bottom: 12px; color: var(--ink-3); font-size: 12px; font-weight: 700; }
.show-facts h1 { max-width: 16ch; font-size: clamp(30px, 3.2vw, 48px); font-weight: 760; }
.show-description { max-width: 62ch; margin-top: 24px; color: var(--ink-2); font-size: 16px; line-height: 1.8; white-space: pre-line; }
.show-description--empty { color: var(--ink-3); }

.fact-list { margin: 32px 0; border-top: 1px solid var(--line); }
.fact-row { display: grid; grid-template-columns: 96px minmax(0, 1fr); gap: 16px; padding: 15px 0; border-bottom: 1px solid var(--line); }
.fact-row dt { color: var(--ink-3); }
.fact-row dd { margin: 0; color: var(--ink); font-weight: 600; overflow-wrap: anywhere; }

.notice-block { padding-top: 22px; border-top: 1px solid var(--line); }
.notice-block h2 { font-size: 16px; font-weight: 720; }
.notice-block p { margin-top: 8px; color: var(--ink-2); font-size: 13px; line-height: 1.7; }

.purchase-column { position: sticky; top: 92px; }
.purchase-panel { padding: 22px; border: 1px solid var(--line); border-radius: var(--radius); background: var(--surface); }
.purchase-panel__heading { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; padding-bottom: 16px; border-bottom: 1px solid var(--line); }
.purchase-panel__heading .section-kicker { margin: 0; }
.ticket-count { color: var(--ink-3); font-size: 12px; }
.ticket-list { display: grid; gap: 0; margin-top: 4px; }

.ticket-option { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 14px; width: 100%; padding: 16px 0; border: 0; border-bottom: 1px solid var(--line); background: transparent; color: var(--ink); text-align: left; cursor: pointer; }
.ticket-option:hover:not(:disabled), .ticket-option--active { color: var(--brand); }
.ticket-option--active { box-shadow: inset 3px 0 0 var(--brand); padding-left: 12px; }
.ticket-option:disabled { color: var(--ink-3); cursor: not-allowed; }
.ticket-option__main, .ticket-option__meta { display: grid; gap: 4px; }
.ticket-option__main strong { font-size: 14px; font-weight: 720; }
.ticket-option small { color: var(--ink-3); font-size: 12px; line-height: 1.45; }
.ticket-option__meta { justify-items: end; text-align: right; }
.ticket-option__meta b { color: var(--ink); font-size: 16px; }
.ticket-option--active .ticket-option__meta b { color: var(--brand); }
.stock-available { color: var(--success) !important; }
.stock-sold-out { color: var(--danger) !important; }

.ticket-feedback { padding: 28px 0; color: var(--ink-3); line-height: 1.7; }
.ticket-feedback--error { color: var(--danger); }
.text-button { margin-top: 8px; padding: 0; border: 0; background: transparent; color: var(--brand); font-weight: 700; cursor: pointer; }
.purchase-summary { display: flex; align-items: end; justify-content: space-between; gap: 12px; padding: 20px 0; }
.purchase-summary span, .purchase-summary strong { display: block; }
.purchase-summary__selection > span { color: var(--ink-3); font-size: 12px; }
.purchase-summary__selection strong { margin-top: 3px; font-size: 14px; }
.purchase-summary__price { color: var(--brand); font-size: 22px; line-height: 1; }
.mobile-ticket-trigger { display: none; }
.purchase-button { width: 100%; }

.ticket-sheet-layer { position: fixed; z-index: 2000; inset: 0; display: grid; align-items: end; }
.ticket-sheet-backdrop { position: absolute; inset: 0; width: 100%; height: 100%; border: 0; background: rgba(20, 22, 25, .56); cursor: default; }
.ticket-sheet { position: relative; max-height: min(78dvh, 620px); display: grid; grid-template-rows: auto minmax(0, 1fr); overflow: hidden; border-top: 1px solid var(--line-strong); border-radius: var(--radius) var(--radius) 0 0; background: var(--surface); box-shadow: var(--shadow-float); }
.ticket-sheet__header { min-height: 76px; display: flex; align-items: center; justify-content: space-between; gap: 20px; padding: 16px; border-bottom: 1px solid var(--line); }
.ticket-sheet__header .section-kicker { margin-bottom: 4px; }
.ticket-sheet__header h2 { font-size: 20px; }
.ticket-sheet__close { width: 40px; height: 40px; display: grid; flex: none; place-items: center; border: 1px solid var(--line); border-radius: var(--radius); background: var(--surface); color: var(--ink); cursor: pointer; }
.ticket-list--sheet { grid-template-columns: 1fr; margin: 0; padding: 0 16px max(18px, env(safe-area-inset-bottom)); overflow-y: auto; overscroll-behavior: contain; }
.ticket-sheet > .ticket-feedback { padding: 28px 16px max(28px, env(safe-area-inset-bottom)); }

.detail-skeleton { display: grid; grid-template-columns: 286px minmax(0, 1fr) 344px; gap: 40px; }
.detail-skeleton__poster { min-height: 430px; }
.detail-skeleton__copy { padding-top: 16px; }
.detail-skeleton__title { width: 70%; height: 48px; }
.detail-skeleton__line { width: 95%; height: 18px; margin-top: 22px; }
.detail-skeleton__line--short { width: 64%; }
.detail-skeleton__tickets { min-height: 420px; }
.detail-state h1 { margin-bottom: 10px; font-size: 22px; }
.detail-state p { margin-bottom: 20px; }

@media (max-width: 1040px) {
  .show-layout { grid-template-columns: minmax(200px, 250px) minmax(0, 1fr); }
  .purchase-column { grid-column: 1 / -1; position: static; }
  .purchase-panel { max-width: none; }
  .ticket-list { grid-template-columns: repeat(2, minmax(0, 1fr)); column-gap: 20px; }
  .detail-skeleton { grid-template-columns: 250px minmax(0, 1fr); }
  .detail-skeleton__tickets { grid-column: 1 / -1; }
}

@media (max-width: 680px) {
  .show-detail { padding-bottom: 142px; }
  .show-layout, .detail-skeleton { display: block; }
  .show-poster-column { position: static; max-width: 210px; }
  .show-facts { margin-top: 28px; }
  .show-facts h1 { max-width: none; font-size: 32px; }
  .fact-row { grid-template-columns: 82px minmax(0, 1fr); }
  .purchase-column { position: fixed; z-index: 10; right: 0; bottom: 0; left: 0; background: var(--surface); box-shadow: var(--shadow-float); }
  .purchase-panel { padding: 12px 16px max(12px, env(safe-area-inset-bottom)); border: 0; border-radius: 0; }
  .purchase-panel__heading, .purchase-panel > .ticket-list, .purchase-panel > .ticket-feedback { display: none; }
  .purchase-summary { padding: 0 0 10px; }
  .purchase-summary__selection, .purchase-summary__price { display: none; }
  .mobile-ticket-trigger { width: 100%; min-height: 46px; display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 0; border: 0; background: transparent; color: var(--ink); text-align: left; cursor: pointer; }
  .mobile-ticket-trigger small { color: var(--ink-3); font-size: 11px; }
  .mobile-ticket-trigger strong { max-width: 58vw; margin-top: 2px; overflow: hidden; font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }
  .mobile-ticket-trigger__action { display: inline-flex !important; align-items: center; gap: 6px; color: var(--brand); font-size: 14px; font-weight: 750; white-space: nowrap; }
  .detail-skeleton__poster { min-height: 315px; max-width: 210px; }
  .detail-skeleton__copy, .detail-skeleton__tickets { margin-top: 28px; }
}
</style>
