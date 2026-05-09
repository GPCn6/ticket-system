<template>
  <div class="order">
    <div class="container">
      <h1 class="order-title">我的订单</h1>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="error" class="error">{{ error }}</div>
      <div v-else class="order-content">
        <div class="order-tabs">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            type="button"
            :class="['tab-btn', { active: activeTab === tab.value }]"
            @click="activeTab = tab.value"
          >
            {{ tab.label }}
          </button>
        </div>
        <div class="order-list">
          <div v-if="orders.length === 0" class="empty">暂无订单</div>
          <div v-else class="order-item" v-for="order in orders" :key="order.id">
            <div class="order-header">
              <span class="order-no">订单号: {{ order.orderNo }}</span>
              <div class="order-header-right">
                <span v-if="order.isSeckill === 1" class="order-type seckill-tag">秒杀抢购</span>
                <span v-else class="order-type normal-tag">普通订单</span>
                <span class="order-status" :class="getStatusClass(order.status)">{{ order.statusName }}</span>
              </div>
            </div>
            <div class="order-body">
              <div class="order-show">
                <h3>{{ order.show?.name || '演出信息加载中' }}</h3>
                <p class="show-time">{{ order.show?.startTime ? formatTime(order.show.startTime) : '' }}</p>
              </div>
              <div class="order-ticket">
                <p>{{ order.ticket?.name || '票档' }}</p>
                <template v-if="order.isSeckill === 1 && order.seckillPrice">
                  <p class="ticket-price">
                    ¥{{ order.seckillPrice }}
                    <span class="original-price">¥{{ order.ticket?.price }}</span>
                    × {{ order.quantity }}
                  </p>
                </template>
                <template v-else>
                  <p class="ticket-price">¥{{ order.ticket?.price }} × {{ order.quantity }}</p>
                </template>
              </div>
            </div>
            <div class="order-footer">
              <span class="total-amount">
                <template v-if="order.isSeckill === 1 && order.seckillPrice">
                  总计: ¥{{ order.totalAmount }}
                  <span class="total-original">¥{{ order.ticket?.price * order.quantity }}</span>
                </template>
                <template v-else>
                  总计: ¥{{ order.totalAmount }}
                </template>
              </span>
              <div class="order-actions">
                <button v-if="order.status === 0" type="button" class="action-btn pay-btn" @click="payOrder(order.orderNo)">
                  立即支付
                </button>
                <button v-if="order.status === 0" type="button" class="action-btn cancel-btn" @click="cancelOrder(order.id)">
                  取消订单
                </button>
                <button v-if="order.status === 1" type="button" class="action-btn detail-btn" @click="goToOrderDetail(order.orderNo)">
                  查看详情
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { orderApi } from '../../api/order';
import { useUserStore } from '../../store/user';
import dayjs from 'dayjs';
import { ElMessage } from 'element-plus';

export default {
  name: 'Order',
  setup() {
    const orders = ref([]);
    const loading = ref(true);
    const error = ref('');
    const activeTab = ref('all');
    const userStore = useUserStore();
    const router = useRouter();

    const tabs = [
      { label: '全部', value: 'all' },
      { label: '待支付', value: 'pending' },
      { label: '已支付', value: 'paid' },
      { label: '已取消', value: 'cancelled' }
    ];

    const loadOrders = async () => {
      if (!userStore.isLoggedIn) {
        error.value = '请先登录';
        loading.value = false;
        return;
      }

      try {
        loading.value = true;
        error.value = '';
        const statusMap = {
          all: undefined,
          pending: 0,
          paid: 1,
          cancelled: 2
        };
        const status = statusMap[activeTab.value];
        const res = await orderApi.getOrderList({ page: 1, size: 50, status });
        if (res.code !== 200) {
          throw new Error(res.message);
        }
        orders.value = res.data?.records || [];
      } catch (err) {
        error.value = err.message || '加载失败，请重试';
        console.error(err);
      } finally {
        loading.value = false;
      }
    };

    const formatTime = (time) => dayjs(time).format('YYYY-MM-DD HH:mm');

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

    const payOrder = async (orderNo) => {
      try {
        const res = await orderApi.payOrder(orderNo);
        if (res.code === 200) {
          ElMessage.success('支付成功');
          await loadOrders();
        } else {
          ElMessage.error(res.message || '支付失败');
        }
      } catch (e) {
        ElMessage.error(e.message || '支付失败');
      }
    };

    const cancelOrder = async (orderId) => {
      if (!confirm('确定要取消订单吗？')) return;
      try {
        const result = await orderApi.cancelOrder(orderId);
        if (result.code === 200) {
          ElMessage.success('订单已取消');
          await loadOrders();
        } else {
          ElMessage.error(result.message);
        }
      } catch (err) {
        ElMessage.error('取消失败，请重试');
        console.error(err);
      }
    };

    const goToOrderDetail = (orderNo) => {
      router.push({ name: 'OrderDetail', params: { orderNo } });
    };

    watch(activeTab, () => {
      loadOrders();
    });

    onMounted(() => {
      loadOrders();
    });

    return {
      orders,
      loading,
      error,
      activeTab,
      tabs,
      formatTime,
      getStatusClass,
      payOrder,
      cancelOrder,
      goToOrderDetail
    };
  }
};
</script>

<style scoped>
.order {
  padding: 20px 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.order-title {
  font-size: 24px;
  margin-bottom: 30px;
  color: #333;
}

.loading,
.error,
.empty {
  text-align: center;
  padding: 100px 0;
  font-size: 18px;
}

.error {
  color: #ff4d4f;
}

.order-tabs {
  display: flex;
  margin-bottom: 20px;
  border-bottom: 1px solid #e8e8e8;
}

.tab-btn {
  padding: 10px 20px;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 16px;
  color: #666;
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
}

.tab-btn:hover {
  color: #ff4d4f;
}

.tab-btn.active {
  color: #ff4d4f;
  border-bottom-color: #ff4d4f;
}

.order-item {
  background: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  transition: transform 0.3s;
}

.order-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e8e8e8;
}

.order-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.order-type {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 3px;
  font-weight: normal;
}

.seckill-tag {
  background: #fff1f0;
  color: #ff4d4f;
  border: 1px solid #ffa39e;
}

.normal-tag {
  background: #f5f5f5;
  color: #999;
  border: 1px solid #d9d9d9;
}

.order-no {
  font-size: 14px;
  color: #666;
}

.order-status {
  font-size: 14px;
  font-weight: bold;
}

.status-pending {
  color: #faad14;
}

.status-paid {
  color: #52c41a;
}

.status-cancelled {
  color: #999;
}

.order-body {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e8e8e8;
}

.order-show h3 {
  font-size: 16px;
  margin-bottom: 5px;
  color: #333;
}

.show-time {
  font-size: 14px;
  color: #666;
}

.order-ticket p {
  font-size: 14px;
  color: #666;
  text-align: right;
}

.ticket-price {
  font-weight: bold;
  color: #ff4d4f;
  margin-top: 5px;
}

.original-price {
  font-size: 12px;
  color: #999;
  text-decoration: line-through;
  font-weight: 400;
  margin: 0 4px;
}

.total-original {
  font-size: 12px;
  color: #999;
  text-decoration: line-through;
  font-weight: 400;
  margin-left: 6px;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.total-amount {
  font-size: 16px;
  font-weight: bold;
  color: #ff4d4f;
}

.order-actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  padding: 8px 16px;
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

.detail-btn {
  border-color: #1890ff;
  color: #1890ff;
}

.detail-btn:hover {
  background: #e6f7ff;
}
</style>
