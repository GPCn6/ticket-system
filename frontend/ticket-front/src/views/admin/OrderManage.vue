<template>
  <div class="order-manage">
    <div class="page-header">
      <div class="header-left">
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h2>订单管理</h2>
      </div>
      <el-button type="danger" @click="handleResetAll" :loading="resetting">重置全部数据</el-button>
    </div>

    <!-- 搜索筛选 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="订单号">
          <el-input v-model="searchForm.orderNo" placeholder="请输入订单号" clearable />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="待支付" :value="0" />
            <el-option label="已支付" :value="1" />
            <el-option label="已取消" :value="2" />
            <el-option label="已完成" :value="3" />
            <el-option label="已退款" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card>
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="orderNo" label="订单号" min-width="180" show-overflow-tooltip />
        <el-table-column label="订单类型" width="110">
          <template #default="{ row }">
            <el-tag :type="row.isSeckill === 1 ? 'danger' : ''" size="small">
              {{ row.isSeckill === 1 ? '秒杀抢购' : '普通订单' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="userName" label="用户名" width="120" />
        <el-table-column prop="showName" label="演出名称" min-width="150" show-overflow-tooltip />
        <el-table-column label="票档" width="180">
          <template #default="{ row }">
            <div class="ticket-info-cell">
              <span class="ticket-name">{{ row.ticketName }}</span>
              <template v-if="row.isSeckill === 1 && row.seckillPrice">
                <span class="ticket-unit-price seckill-price">¥{{ row.seckillPrice }}</span>
                <span class="ticket-original-price" v-if="row.ticket?.price">¥{{ row.ticket.price }}</span>
              </template>
              <template v-else>
                <span class="ticket-unit-price" v-if="row.ticket?.price">¥{{ row.ticket.price }}</span>
              </template>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="totalPrice" label="总价" width="140">
          <template #default="{ row }">
            <template v-if="row.isSeckill === 1 && row.seckillPrice">
              <span class="price price-seckill">¥{{ row.totalPrice }}</span>
              <span class="price-original" v-if="row.ticket?.price">¥{{ row.ticket.price * row.quantity }}</span>
            </template>
            <template v-else>
              <span class="price">¥{{ row.totalPrice }}</span>
            </template>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="payTime" label="支付时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.payTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <el-descriptions :column="2" border v-if="currentOrder">
        <el-descriptions-item label="订单ID">{{ currentOrder.id }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ currentOrder.userId }}</el-descriptions-item>
        <el-descriptions-item label="演出名称">{{ currentOrder.showName }}</el-descriptions-item>
        <el-descriptions-item label="票档">{{ currentOrder.ticketName }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ currentOrder.quantity }}</el-descriptions-item>
        <el-descriptions-item label="总价">
          <span class="price">¥{{ currentOrder.totalPrice }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentOrder.status)">
            {{ getStatusText(currentOrder.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="下单时间">
          {{ formatDateTime(currentOrder.createTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="支付时间">
          {{ formatDateTime(currentOrder.payTime) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowLeft } from '@element-plus/icons-vue';
import { orderApi } from '../../api/order';

const router = useRouter();

const loading = ref(false);
const tableData = ref([]);
const detailVisible = ref(false);
const currentOrder = ref(null);
const resetting = ref(false);

const searchForm = reactive({
  orderNo: '',
  status: null
});

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
});

const statusMap = {
  0: { text: '待支付', type: 'warning' },
  1: { text: '已支付', type: 'success' },
  2: { text: '已取消', type: 'info' },
  3: { text: '已完成', type: 'success' },
  4: { text: '已退款', type: 'danger' }
};

const getStatusType = (status) => {
  return statusMap[status]?.type || 'info';
};

const getStatusText = (status) => {
  return statusMap[status]?.text || '未知';
};

const formatDateTime = (date) => {
  if (!date) return '-';
  return new Date(date).format('yyyy-MM-dd hh:mm:ss');
};

Date.prototype.format = function(fmt) {
  var o = {
    'y+': this.getFullYear(),
    'M+': this.getMonth() + 1,
    'd+': this.getDate(),
    'h+': this.getHours(),
    'm+': this.getMinutes(),
    's+': this.getSeconds()
  };
  for (var k in o) {
    if (new RegExp('(' + k + ')').test(fmt)) {
      fmt = fmt.replace(RegExp.$1, o[k].toString().padStart(RegExp.$1.length, '0'));
    }
  }
  return fmt;
};

const loadData = async () => {
  loading.value = true;
  try {
    if (searchForm.orderNo) {
      const detailRes = await orderApi.getByOrderNo(searchForm.orderNo);
      if (detailRes.code === 200 && detailRes.data) {
        tableData.value = [detailRes.data];
        pagination.total = 1;
        return;
      }
    }
    const res = await orderApi.getAdminOrderList({
      page: pagination.page,
      size: pagination.size,
      status: searchForm.status
    });
    if (res.code === 200) {
      tableData.value = res.data.records || res.data.list || [];
      pagination.total = res.data.total || 0;
    }
  } catch (error) {
    ElMessage.error('加载数据失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  pagination.page = 1;
  loadData();
};

const handleReset = () => {
  searchForm.orderNo = '';
  searchForm.status = null;
  handleSearch();
};

const handleDetail = async (row) => {
  try {
    const res = await orderApi.getOrderDetail(row.id);
    if (res.code === 200) {
      currentOrder.value = res.data;
      detailVisible.value = true;
    }
  } catch (error) {
    ElMessage.error('加载详情失败');
  }
};

const handleResetAll = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要重置所有数据吗？此操作将：\n1. 删除所有用户的全部订单\n2. 恢复演出票档库存到初始值\n3. 恢复秒杀抢购库存\n4. 清除Redis缓存\n\n此操作不可撤销！',
      '警告',
      { confirmButtonText: '确定重置', cancelButtonText: '取消', type: 'warning' }
    );
  } catch {
    return;
  }
  resetting.value = true;
  try {
    const res = await orderApi.resetAllData();
    if (res.code === 200) {
      ElMessage.success(res.data || '重置成功');
      await loadData();
    } else {
      ElMessage.error(res.message || '重置失败');
    }
  } catch (error) {
    ElMessage.error('重置请求失败');
  } finally {
    resetting.value = false;
  }
};

const goBack = () => {
  router.push('/admin');
};

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.order-manage {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.filter-card {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.price {
  color: #ff4d4f;
  font-weight: 600;
}

.price-seckill {
  font-size: 14px;
}

.price-original {
  display: block;
  font-size: 11px;
  color: #999;
  text-decoration: line-through;
  font-weight: 400;
}

.ticket-info-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ticket-name {
  font-size: 13px;
  color: #333;
}

.ticket-unit-price {
  font-size: 12px;
  color: #666;
}

.seckill-price {
  color: #ff4d4f;
  font-weight: bold;
}

.ticket-original-price {
  font-size: 11px;
  color: #999;
  text-decoration: line-through;
  margin-left: 4px;
}
</style>
