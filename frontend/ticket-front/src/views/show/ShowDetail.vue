<template>
  <div class="show-detail">
    <div class="container">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="error" class="error">{{ error }}</div>
      <div v-else class="show-content">
        <div class="show-header">
          <img :src="show.poster" :alt="show.name" class="show-poster" />
          <div class="show-info">
            <h1>{{ show.name }}</h1>
            <p class="show-description">{{ show.description }}</p>
            <div class="show-meta">
              <span class="meta-item">城市: {{ show.city }}</span>
              <span class="meta-item">场馆: {{ show.venue }}</span>
              <span class="meta-item">时间: {{ formatTime(show.startTime) }} - {{ formatTime(show.endTime) }}</span>
            </div>
          </div>
        </div>
        <div class="ticket-list">
          <h2>票档信息</h2>
          <div v-if="ticketError" class="empty-ticket">{{ ticketError }}</div>
          <div v-else-if="!tickets.length" class="empty-ticket">暂无票档</div>
          <div class="ticket-item" v-for="ticket in tickets" :key="ticket.id">
            <div class="ticket-info">
              <h3>{{ ticket.name }}</h3>
              <p class="seat-range">{{ ticket.seatRange }}</p>
              <p class="stock">剩余: {{ ticket.availableStock }}张</p>
            </div>
            <div class="ticket-price">
              <span class="price">¥{{ ticket.price }}</span>
              <button class="buy-btn" type="button" @click="buyTicket(ticket)" :disabled="ticket.availableStock <= 0">
                立即购买
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { showApi } from '../../api/show';
import { ticketApi } from '../../api/ticket';
import { orderApi } from '../../api/order';
import { useUserStore } from '../../store/user';
import { formatDateTime } from '../../utils/format';
import { ElMessage } from 'element-plus';

const props = defineProps({
  id: { type: String, required: true }
});

const router = useRouter();
const userStore = useUserStore();
const show = ref({});
const tickets = ref([]);
const loading = ref(true);
const error = ref('');
const ticketError = ref('');

const formatTime = formatDateTime;

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
    } catch (ticketErr) {
      tickets.value = [];
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
  if (!userStore.isLoggedIn) {
    await router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } });
    return;
  }
  try {
    const detailRes = await ticketApi.getTicketDetail(ticket.id);
    if (detailRes.code !== 200 || !detailRes.data) {
      ElMessage.error(detailRes.message || '票档信息异常');
      return;
    }
    if ((detailRes.data.availableStock || 0) <= 0) {
      ElMessage.warning('该票档已售罄');
      return;
    }
    const res = await orderApi.createOrder({
      showId: Number(props.id),
      ticketId: ticket.id,
      quantity: 1,
      seatId: null,
      userId: userStore.userInfo?.id
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
    ElMessage.error(e.message || '下单失败');
  }
};

onMounted(loadShowDetail);
</script>

<style scoped>
.show-detail {
  padding: 20px 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.loading,
.error {
  text-align: center;
  padding: 100px 0;
  font-size: 18px;
}

.error {
  color: #ff4d4f;
}

.show-header {
  display: flex;
  margin-bottom: 40px;
  background: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
}

.show-poster {
  width: 300px;
  height: 400px;
  object-fit: cover;
  border-radius: 8px;
  margin-right: 30px;
}

.show-info {
  flex: 1;
}

.show-info h1 {
  font-size: 28px;
  margin-bottom: 20px;
  color: #333;
}

.show-description {
  font-size: 16px;
  line-height: 1.6;
  margin-bottom: 20px;
  color: #666;
}

.show-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  font-size: 14px;
  color: #999;
}

.ticket-list {
  margin-top: 40px;
}

.ticket-list h2 {
  font-size: 20px;
  margin-bottom: 20px;
  color: #333;
}

.empty-ticket {
  color: #999;
  padding: 16px 0;
}

.ticket-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.ticket-info h3 {
  font-size: 18px;
  margin-bottom: 10px;
  color: #333;
}

.seat-range {
  font-size: 14px;
  color: #666;
  margin-bottom: 5px;
}

.stock {
  font-size: 14px;
  color: #ff4d4f;
}

.ticket-price {
  text-align: right;
}

.price {
  font-size: 24px;
  font-weight: bold;
  color: #ff4d4f;
  display: block;
  margin-bottom: 10px;
}

.buy-btn {
  padding: 10px 20px;
  background: #ff4d4f;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  transition: background 0.3s;
}

.buy-btn:hover {
  background: #ff7875;
}

.buy-btn:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}
</style>
