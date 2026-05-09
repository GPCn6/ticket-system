<template>
  <div class="order-detail">
    <div class="container">
      <h1 class="order-title">订单详情</h1>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="error" class="error">{{ error }}</div>
      <div v-else class="order-content">
        <div class="order-info">
          <div class="info-item">
            <span class="label">订单号:</span>
            <span class="value">{{ order.orderNo }}</span>
          </div>
          <div class="info-item">
            <span class="label">订单状态:</span>
            <span class="value" :class="getStatusClass(order.status)">{{ order.statusName }}</span>
          </div>
          <div class="info-item">
            <span class="label">订单类型:</span>
            <span class="value">
              <span :class="order.isSeckill === 1 ? 'tag-seckill' : 'tag-normal'">
                {{ order.isSeckill === 1 ? '秒杀抢购' : '普通订单' }}
              </span>
            </span>
          </div>
          <div class="info-item">
            <span class="label">创建时间:</span>
            <span class="value">{{ formatTime(order.createTime) }}</span>
          </div>
          <div class="info-item" v-if="order.payTime">
            <span class="label">支付时间:</span>
            <span class="value">{{ formatTime(order.payTime) }}</span>
          </div>
        </div>
        <div class="show-info" v-if="order.show">
          <h2>演出信息</h2>
          <div class="show-content">
            <img :src="order.show.poster" :alt="order.show.name" class="show-poster" />
            <div class="show-details">
              <h3>{{ order.show.name }}</h3>
              <p class="show-time">{{ formatTime(order.show.startTime) }}</p>
              <p class="show-venue">{{ order.show.venue }}</p>
              <p class="show-city">{{ order.show.city }}</p>
            </div>
          </div>
        </div>
        <div class="ticket-info" v-if="order.ticket">
          <h2>票档信息</h2>
          <div class="ticket-content">
            <div class="ticket-item">
              <span class="ticket-name">{{ order.ticket.name }}</span>
              <template v-if="order.isSeckill === 1 && order.seckillPrice">
                <span class="ticket-price ticket-price-seckill">¥{{ order.seckillPrice }}</span>
                <span class="ticket-price-original">¥{{ order.ticket.price }}</span>
              </template>
              <template v-else>
                <span class="ticket-price">¥{{ order.ticket.price }}</span>
              </template>
              <span class="ticket-quantity">× {{ order.quantity }}</span>
            </div>
            <div class="ticket-total">
              <span class="total-label">总计:</span>
              <span class="total-amount">¥{{ order.totalAmount }}</span>
            </div>
          </div>
        </div>
        <div class="order-actions" v-if="order.status === 0">
          <button type="button" class="action-btn pay-btn" @click="payOrder">立即支付</button>
          <button type="button" class="action-btn cancel-btn" @click="cancelOrder">取消订单</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import { orderApi } from '../../api/order';
import dayjs from 'dayjs';
import { ElMessage } from 'element-plus';

export default {
  name: 'OrderDetail',
  props: {
    orderNo: {
      type: String,
      required: true
    }
  },
  setup(props) {
    const order = ref({});
    const loading = ref(true);
    const error = ref('');

    const loadOrderDetail = async () => {
      try {
        loading.value = true;
        error.value = '';
        const data = await orderApi.getByOrderNo(props.orderNo);
        if (data.code !== 200) {
          throw new Error(data.message);
        }
        order.value = data.data || {};
      } catch (err) {
        error.value = err.message || '加载失败，请重试';
        console.error(err);
      } finally {
        loading.value = false;
      }
    };

    const formatTime = (time) => dayjs(time).format('YYYY-MM-DD HH:mm:ss');

    const getStatusClass = (status) => {
      switch (status) {
        case 0:
          return 'status-pending';
        case 1:
          return 'status-paid';
        case 2:
          return 'status-cancelled';
        default:
          return '';
      }
    };

    const payOrder = async () => {
      try {
        const res = await orderApi.payOrder(order.value.orderNo);
        if (res.code === 200) {
          ElMessage.success('支付成功');
          await loadOrderDetail();
        } else {
          ElMessage.error(res.message || '支付失败');
        }
      } catch (e) {
        ElMessage.error(e.message || '支付失败');
      }
    };

    const cancelOrder = async () => {
      if (!confirm('确定要取消订单吗？')) return;
      try {
        const result = await orderApi.cancelOrder(order.value.id);
        if (result.code === 200) {
          ElMessage.success('订单已取消');
          await loadOrderDetail();
        } else {
          ElMessage.error(result.message);
        }
      } catch (err) {
        ElMessage.error('取消失败，请重试');
        console.error(err);
      }
    };

    onMounted(() => {
      loadOrderDetail();
    });

    return {
      order,
      loading,
      error,
      formatTime,
      getStatusClass,
      payOrder,
      cancelOrder
    };
  }
};
</script>

<style scoped>
.order-detail {
  padding: 20px 0;
}

.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 20px;
}

.order-title {
  font-size: 24px;
  margin-bottom: 30px;
  color: #333;
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

.order-content {
  background: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
}

.order-info {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e8e8e8;
}

.info-item {
  display: flex;
  margin-bottom: 10px;
}

.label {
  width: 100px;
  font-size: 14px;
  color: #666;
}

.value {
  font-size: 14px;
  color: #333;
}

.status-pending {
  color: #faad14;
  font-weight: bold;
}

.status-paid {
  color: #52c41a;
  font-weight: bold;
}

.status-cancelled {
  color: #999;
  font-weight: bold;
}

.tag-seckill {
  display: inline-block;
  background: #fff1f0;
  color: #ff4d4f;
  border: 1px solid #ffa39e;
  border-radius: 3px;
  padding: 1px 8px;
  font-size: 12px;
}

.tag-normal {
  display: inline-block;
  background: #f5f5f5;
  color: #999;
  border: 1px solid #d9d9d9;
  border-radius: 3px;
  padding: 1px 8px;
  font-size: 12px;
}

.show-info,
.ticket-info {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e8e8e8;
}

.show-info h2,
.ticket-info h2 {
  font-size: 18px;
  margin-bottom: 20px;
  color: #333;
}

.show-content {
  display: flex;
}

.show-poster {
  width: 150px;
  height: 200px;
  object-fit: cover;
  border-radius: 8px;
  margin-right: 20px;
}

.show-details h3 {
  font-size: 16px;
  margin-bottom: 10px;
  color: #333;
}

.show-time,
.show-venue,
.show-city {
  font-size: 14px;
  margin-bottom: 5px;
  color: #666;
}

.ticket-content {
  background: white;
  padding: 20px;
  border-radius: 8px;
}

.ticket-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
}

.ticket-name {
  font-size: 14px;
  color: #333;
}

.ticket-price {
  font-size: 14px;
  color: #ff4d4f;
  font-weight: bold;
}

.ticket-price-seckill {
  color: #ff4d4f;
}

.ticket-price-original {
  font-size: 12px;
  color: #999;
  text-decoration: line-through;
  margin-left: 6px;
}

.ticket-quantity {
  font-size: 14px;
  color: #666;
}

.ticket-total {
  display: flex;
  justify-content: space-between;
  padding-top: 15px;
  border-top: 1px solid #e8e8e8;
}

.total-label {
  font-size: 16px;
  color: #333;
  font-weight: bold;
}

.total-amount {
  font-size: 18px;
  color: #ff4d4f;
  font-weight: bold;
}

.order-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
}

.action-btn {
  padding: 10px 20px;
  border: 1px solid #d9d9d9;
  background: white;
  cursor: pointer;
  font-size: 14px;
  border-radius: 4px;
  transition: all 0.3s;
}

.pay-btn {
  background: #ff4d4f;
  color: white;
  border-color: #ff4d4f;
}

.pay-btn:hover {
  background: #ff7875;
}

.cancel-btn:hover {
  border-color: #ff4d4f;
  color: #ff4d4f;
}
</style>
