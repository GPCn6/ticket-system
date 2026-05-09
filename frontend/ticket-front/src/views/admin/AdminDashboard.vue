<template>
  <div class="admin-dashboard">
    <div class="page-header">
      <h2>管理后台</h2>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #409eff;">
              <el-icon><Tickets /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.showCount }}</div>
              <div class="stat-label">演出数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #67c23a;">
              <el-icon><Ticket /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.ticketCount }}</div>
              <div class="stat-label">票档数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #e6a23c;">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.seckillCount }}</div>
              <div class="stat-label">秒杀场次</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #f56c6c;">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.orderCount }}</div>
              <div class="stat-label">订单数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷菜单 -->
    <el-card class="menu-card">
      <template #header>
        <span>快捷操作</span>
      </template>
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="menu-item" @click="$router.push('/admin/shows')">
            <el-icon><Tickets /></el-icon>
            <span>演出管理</span>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="menu-item" @click="$router.push('/admin/tickets')">
            <el-icon><Ticket /></el-icon>
            <span>票档管理</span>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="menu-item" @click="$router.push('/admin/seckill')">
            <el-icon><Clock /></el-icon>
            <span>秒杀管理</span>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="menu-item" @click="$router.push('/admin/orders')">
            <el-icon><Document /></el-icon>
            <span>订单管理</span>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 热门演出 -->
    <el-card class="hot-card">
      <template #header>
        <div class="card-header">
          <span>热门演出</span>
          <el-button type="primary" link size="small" @click="$router.push('/admin/shows')">
            查看更多
          </el-button>
        </div>
      </template>
      <el-table :data="hotShows" v-loading="loading" stripe>
        <el-table-column prop="name" label="演出名称" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="venue" label="场馆" width="150" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { Tickets, Ticket, Clock, Document } from '@element-plus/icons-vue';
import http from '../../api/http';

const loading = ref(false);
const hotShows = ref([]);

const stats = reactive({
  showCount: 0,
  ticketCount: 0,
  seckillCount: 0,
  orderCount: 0
});

const loadStats = async () => {
  try {
    const res = await http.get('/show/stats');
    if (res.code === 200) {
      stats.showCount = res.data.showCount || 0;
      stats.ticketCount = res.data.ticketCount || 0;
      stats.seckillCount = res.data.seckillCount || 0;
      stats.orderCount = res.data.orderCount || 0;
    }
  } catch (error) {
    console.error('加载统计失败', error);
  }
};

const loadHotShows = async () => {
  loading.value = true;
  try {
    const res = await http.get('/show/hot', { params: { limit: 5 } });
    if (res.code === 200) {
      hotShows.value = res.data || [];
    }
  } catch (error) {
    console.error('加载热门演出失败', error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadStats();
  loadHotShows();
});
</script>

<style scoped>
.admin-dashboard {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #fff;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.menu-card {
  margin-bottom: 20px;
}

.menu-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 30px 0;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.menu-item:hover {
  background: #ecf5ff;
  color: #409eff;
}

.menu-item .el-icon {
  font-size: 32px;
}

.menu-item span {
  font-size: 14px;
  font-weight: 500;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
