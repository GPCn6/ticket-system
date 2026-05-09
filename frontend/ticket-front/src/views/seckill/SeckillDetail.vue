<template>
  <div class="seckill-detail">
    <div class="container">
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="error" class="error">{{ error }}</div>
      <div v-else class="seckill-content">
        <div class="seckill-info">
          <h1>{{ session.show?.name || '抢购场次' }}</h1>
          <p class="seckill-time">抢购时间: {{ formatTime(session.startTime) }} - {{ formatTime(session.endTime) }}</p>
          <p class="seckill-stock">剩余: {{ session.stock }}张</p>
          <div class="ticket-info" v-if="session.ticket">
            <h2>{{ session.ticket.name }}</h2>
            <p class="ticket-price">抢购价: ¥{{ session.seckillPrice }}</p>
            <p class="original-price">原价: ¥{{ session.ticket.price }}</p>
          </div>
        </div>
        <div class="seckill-action">
          <div class="quantity">
            <span>数量:</span>
            <button type="button" @click="decreaseQuantity" :disabled="quantity <= 1">-</button>
            <span>{{ quantity }}</span>
            <button type="button" @click="increaseQuantity" :disabled="quantity >= 2">+</button>
            <p class="limit">每人限购2张</p>
          </div>
          <button type="button" class="seckill-btn" @click="doSeckill" :disabled="isSeckilling || session.stock <= 0">
            {{ isSeckilling ? '抢购中...' : session.stock <= 0 ? '已售罄' : '立即抢购' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { seckillApi } from '../../api/seckill';
import { orderApi } from '../../api/order';
import { useUserStore } from '../../store/user';
import dayjs from 'dayjs';
import { ElMessage } from 'element-plus';

export default {
  name: 'SeckillDetail',
  props: {
    sessionId: {
      type: String,
      required: true
    }
  },
  setup(props) {
    const router = useRouter();
    const session = ref({});
    const loading = ref(true);
    const error = ref('');
    const quantity = ref(1);
    const isSeckilling = ref(false);
    const userStore = useUserStore();

    const loadSessionDetail = async () => {
      try {
        loading.value = true;
        error.value = '';
        const data = await seckillApi.getSeckillDetail(props.sessionId);
        if (data.code !== 200) {
          throw new Error(data.message);
        }
        session.value = data.data || {};
      } catch (err) {
        error.value = err.message || '加载失败，请重试';
        console.error(err);
      } finally {
        loading.value = false;
      }
    };

    const formatTime = (time) => dayjs(time).format('YYYY-MM-DD HH:mm');

    const decreaseQuantity = () => {
      if (quantity.value > 1) quantity.value--;
    };

    const increaseQuantity = () => {
      if (quantity.value < 2) quantity.value++;
    };

    let pollingTimer = null;

    const waitForOrderAndPay = async (userId) => {
      ElMessage.info('抢购成功，正在创建订单，请稍候...');
      let retries = 0;
      const maxRetries = 30;
      const pollInterval = 2000;

      const poll = async () => {
        try {
          const res = await orderApi.getOrderList({ page: 1, size: 5, status: 0 });
          if (res.code === 200) {
            const pendingOrders = res.data?.records || [];
            const seckillOrder = pendingOrders.find(o => o.isSeckill === 1);
            if (seckillOrder) {
              ElMessage.success('订单创建成功，请尽快支付！');
              await router.push({ name: 'OrderDetail', params: { orderNo: seckillOrder.orderNo } });
              return;
            }
          }
          retries++;
          if (retries < maxRetries) {
            pollingTimer = setTimeout(poll, pollInterval);
          } else {
            ElMessage.warning('订单处理中，请稍后在订单列表中查看');
            await router.push({ name: 'Order' });
          }
        } catch (err) {
          retries++;
          if (retries < maxRetries) {
            pollingTimer = setTimeout(poll, pollInterval);
          } else {
            ElMessage.warning('订单处理中，请稍后在订单列表中查看');
            await router.push({ name: 'Order' });
          }
        }
      };

      pollingTimer = setTimeout(poll, pollInterval);
    };

    const doSeckill = async () => {
      if (!userStore.isLoggedIn) {
        await router.push({ name: 'Login', query: { redirect: `/seckill/${props.sessionId}` } });
        return;
      }
      if (isSeckilling.value || session.value.stock <= 0) return;

      try {
        isSeckilling.value = true;
        const result = await seckillApi.executeSeckill({
          sessionId: Number(props.sessionId),
          quantity: quantity.value
        });
        if (result.code === 200) {
          ElMessage.success('抢购成功！');
          await waitForOrderAndPay(userStore.userInfo?.id);
        } else {
          ElMessage.error(result.message || '抢购失败');
        }
      } catch (err) {
        ElMessage.error(err.message || '抢购失败，请重试');
        console.error(err);
      } finally {
        isSeckilling.value = false;
      }
    };

    onMounted(() => {
      loadSessionDetail();
    });

    onUnmounted(() => {
      if (pollingTimer) {
        clearTimeout(pollingTimer);
        pollingTimer = null;
      }
    });

    return {
      session,
      loading,
      error,
      quantity,
      isSeckilling,
      formatTime,
      decreaseQuantity,
      increaseQuantity,
      doSeckill
    };
  }
};
</script>

<style scoped>
.seckill-detail {
  padding: 20px 0;
}

.container {
  max-width: 800px;
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

.seckill-content {
  background: #f5f5f5;
  padding: 40px;
  border-radius: 8px;
}

.seckill-info h1 {
  font-size: 28px;
  margin-bottom: 20px;
  color: #333;
}

.seckill-time,
.seckill-stock {
  font-size: 16px;
  margin-bottom: 10px;
  color: #666;
}

.seckill-stock {
  color: #ff4d4f;
  font-weight: bold;
}

.ticket-info {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #e8e8e8;
}

.ticket-info h2 {
  font-size: 20px;
  margin-bottom: 10px;
  color: #333;
}

.ticket-price {
  font-size: 24px;
  font-weight: bold;
  color: #ff4d4f;
  margin-bottom: 5px;
}

.original-price {
  font-size: 14px;
  color: #999;
  text-decoration: line-through;
}

.seckill-action {
  margin-top: 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.quantity {
  display: flex;
  align-items: center;
  gap: 10px;
}

.quantity span {
  font-size: 16px;
  color: #333;
}

.quantity button {
  width: 30px;
  height: 30px;
  border: 1px solid #d9d9d9;
  background: white;
  cursor: pointer;
  font-size: 16px;
  border-radius: 4px;
  transition: all 0.3s;
}

.quantity button:hover {
  border-color: #ff4d4f;
  color: #ff4d4f;
}

.quantity button:disabled {
  border-color: #d9d9d9;
  color: #d9d9d9;
  cursor: not-allowed;
}

.limit {
  font-size: 12px;
  color: #999;
  margin-left: 10px;
}

.seckill-btn {
  padding: 15px 40px;
  background: #ff4d4f;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 18px;
  transition: background 0.3s;
}

.seckill-btn:hover {
  background: #ff7875;
}

.seckill-btn:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}
</style>
