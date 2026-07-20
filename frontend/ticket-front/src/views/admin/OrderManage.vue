<template>
  <main class="page-shell admin-page order-manage">
    <header class="admin-page-heading">
      <div class="admin-title-group">
        <el-tooltip content="返回运营总览" placement="bottom">
          <el-button class="back-button" :icon="ArrowLeft" circle aria-label="返回运营总览" @click="goBack" />
        </el-tooltip>
        <div>
          <p class="admin-eyebrow">ORDER OPERATIONS</p>
          <h1>订单管理</h1>
          <p>检索订单进度、支付状态与票务明细。</p>
        </div>
      </div>
      <el-button type="danger" @click="handleResetAll" :loading="resetting">重置全部数据</el-button>
    </header>

    <section class="admin-filter" aria-label="订单筛选">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="订单号">
          <el-input v-model="searchForm.orderNo" placeholder="请输入订单号" clearable />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="待支付" :value="0" />
            <el-option label="已支付" :value="1" />
            <el-option label="已取消" :value="2" />
            <el-option label="已退款" :value="3" />
            <el-option label="已完成" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="table-frame">
      <div class="table-frame-heading">
        <div>
          <h2>订单列表</h2>
          <span>共 {{ pagination.total }} 笔订单</span>
        </div>
      </div>
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
    </section>

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
  </main>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import dayjs from 'dayjs';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowLeft } from '@element-plus/icons-vue';
import { orderApi } from '../../api/order';
import { getOrderStatus } from '../../utils/ticketing-state';

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

const getStatusType = (status) => getOrderStatus(status).type;
const getStatusText = (status) => getOrderStatus(status).text;

const formatDateTime = (date) => {
  if (!date) return '-';
  return dayjs(date).format('YYYY-MM-DD HH:mm');
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

const goBack = () => {
  router.push('/admin');
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

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.admin-page { padding-block: 32px 56px; }
.admin-page-heading, .admin-title-group, .table-frame-heading, .admin-filter :deep(.el-form) { display: flex; align-items: center; }
.admin-page-heading { justify-content: space-between; gap: 20px; margin-bottom: 28px; }
.admin-title-group { gap: 12px; min-width: 0; }
.admin-eyebrow { margin: 0 0 4px; color: var(--ink-3); font-size: 11px; font-weight: 750; letter-spacing: .08em; }
.admin-title-group h1 { margin: 0; font-size: 26px; font-weight: 760; }
.admin-title-group p:not(.admin-eyebrow) { margin: 5px 0 0; color: var(--ink-2); font-size: 13px; }
.back-button { flex: 0 0 auto; }
.admin-filter { padding: 16px 0; margin-bottom: 18px; border-block: 1px solid var(--line); }
.admin-filter :deep(.el-form) { flex-wrap: wrap; gap: 8px 12px; }
.admin-filter :deep(.el-form-item) { margin: 0; }
.admin-filter :deep(.el-input), .admin-filter :deep(.el-select) { width: 220px; }
.table-frame { overflow: hidden; border: 1px solid var(--line); border-radius: var(--radius); background: var(--surface); }
.table-frame-heading { justify-content: space-between; min-height: 58px; padding: 0 18px; border-bottom: 1px solid var(--line); }
.table-frame-heading h2 { margin: 0; font-size: 15px; font-weight: 720; }
.table-frame-heading span { margin-left: 10px; color: var(--ink-3); font-size: 12px; }
.table-frame :deep(.el-table) { --el-table-border-color: var(--line); }
.pagination { display: flex; justify-content: flex-end; padding: 16px 18px; margin: 0; border-top: 1px solid var(--line); }
.price { color: var(--brand); font-weight: 720; font-variant-numeric: tabular-nums; }
.price-seckill { font-size: 14px; }
.price-original, .ticket-original-price { color: var(--ink-3); font-size: 11px; font-weight: 400; text-decoration: line-through; }
.price-original { display: block; }
.ticket-info-cell { display: grid; gap: 3px; }
.ticket-name { color: var(--ink); font-size: 13px; font-weight: 650; }
.ticket-unit-price { color: var(--ink-2); font-size: 12px; }
.seckill-price { color: var(--brand); font-weight: 720; }
.ticket-original-price { margin-left: 4px; }
@media (max-width: 768px) {
  .admin-page-heading { align-items: stretch; flex-direction: column; }
  .admin-page-heading > .el-button { width: 100%; }
  .admin-filter :deep(.el-form) { display: grid; grid-template-columns: 1fr; }
  .admin-filter :deep(.el-form-item), .admin-filter :deep(.el-input), .admin-filter :deep(.el-select) { width: 100%; }
  .table-frame-heading { padding-inline: 14px; }
  .pagination { justify-content: flex-start; overflow-x: auto; padding-inline: 14px; }
}
</style>
