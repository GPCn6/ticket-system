<template>
  <div class="seckill-manage">
    <div class="page-header">
      <div class="header-left">
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h2>秒杀场次管理</h2>
      </div>
      <div class="header-actions">
        <el-button type="warning" @click="handleBatchWarmup" :loading="warmingUp">
          <el-icon><Lightning /></el-icon>
          批量预热
        </el-button>
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          添加秒杀场次
        </el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="场次名称" min-width="220">
          <template #default="{ row }">
            <div>
              <div style="font-weight: 600;">{{ row.show?.name || '-' }}</div>
              <div style="font-size: 12px; color: #909399; margin-top: 2px;">
                {{ row.ticket ? `${row.ticket.name} (￥${row.ticket.price})` : '-' }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="seckillPrice" label="秒杀价" width="100">
          <template #default="{ row }">
            <span class="price">￥{{ row.seckillPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="实时库存" width="100">
          <template #default="{ row }">
            <span :class="{ 'stock-warn': row.stock != null && row.stock <= 10 }">
              {{ row.stock ?? '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="预热状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.warmedUp ? 'success' : 'info'" size="small">
              {{ row.warmedUp ? '已预热' : '未预热' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row)" size="small">
              {{ getStatusText(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              type="warning" link size="small"
              @click="handleWarmup(row)"
              :disabled="row.warmedUp"
            >
              {{ row.warmedUp ? '已预热' : '预热' }}
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="关联演出" prop="showId">
          <el-select v-model="form.showId" placeholder="请选择演出" filterable>
            <el-option
              v-for="show in showList"
              :key="show.id"
              :label="show.name"
              :value="show.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="票档" prop="ticketId">
          <el-select v-model="form.ticketId" placeholder="请先选择演出" filterable>
            <el-option
              v-for="ticket in ticketList"
              :key="ticket.id"
              :label="`${ticket.name} (￥${ticket.price})`"
              :value="ticket.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="秒杀价格" prop="seckillPrice">
          <el-input-number v-model="form.seckillPrice" :min="0" :precision="2" :controls="false" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" :controls="false" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="选择开始时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="选择结束时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">开启</el-radio>
            <el-radio :label="0">关闭</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue';
import dayjs from 'dayjs';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, ArrowLeft, Lightning } from '@element-plus/icons-vue';
import { seckillApi } from '../../api/seckill';
import { showApi } from '../../api/show';
import { ticketApi } from '../../api/ticket';

const router = useRouter();

const loading = ref(false);
const warmingUp = ref(false);
const tableData = ref([]);
const showList = ref([]);
const ticketList = ref([]);
const dialogVisible = ref(false);
const dialogTitle = ref('添加秒杀场次');
const submitting = ref(false);
const formRef = ref(null);

const form = reactive({
  id: null,
  name: '',
  showId: null,
  ticketId: null,
  seckillPrice: 0,
  stock: 0,
  startTime: '',
  endTime: '',
  status: 1
});

const rules = {
  showId: [{ required: true, message: '请选择关联演出', trigger: 'change' }],
  ticketId: [{ required: true, message: '请选择票档', trigger: 'change' }],
  seckillPrice: [{ required: true, message: '请输入秒杀价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
};

watch(() => form.showId, async (newShowId) => {
  if (newShowId) {
    try {
      const res = await ticketApi.getByShowId(newShowId);
      if (res.code === 200) {
        ticketList.value = res.data || [];
      }
    } catch (error) {
      console.error('加载票档列表失败', error);
    }
  } else {
    ticketList.value = [];
    form.ticketId = null;
  }
});

const formatDateTime = (date) => {
  if (!date) return '-';
  return dayjs(date).format('YYYY-MM-DD HH:mm');
};


const getStatusType = (row) => {
  if (row.status === 0) return 'info';
  const now = new Date();
  const start = new Date(row.startTime);
  const end = new Date(row.endTime);
  if (now < start) return 'warning';
  if (now >= start && now <= end) return 'success';
  return 'info';
};

const getStatusText = (row) => {
  if (row.status === 0) return '已关闭';
  const now = new Date();
  const start = new Date(row.startTime);
  const end = new Date(row.endTime);
  if (now < start) return '未开始';
  if (now >= start && now <= end) return '进行中';
  return '已结束';
};

const loadShowList = async () => {
  try {
    const res = await showApi.getShowList({ page: 1, size: 1000 });
    if (res.code === 200) {
      showList.value = res.data.records || res.data.list || [];
    }
  } catch (error) {
    console.error('加载演出列表失败', error);
  }
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await seckillApi.getAllSessions();
    if (res.code === 200) {
      tableData.value = (res.data || []).sort((a, b) => a.id - b.id);
    }
  } catch (error) {
    ElMessage.error('加载数据失败');
  } finally {
    loading.value = false;
  }
};

const handleCreate = () => {
  dialogTitle.value = '添加秒杀场次';
  Object.keys(form).forEach(key => {
    if (key === 'status') form[key] = 1;
    else if (key === 'seckillPrice' || key === 'stock') form[key] = 0;
    else form[key] = '';
  });
  form.id = null;
  ticketList.value = [];
  dialogVisible.value = true;
};

const handleEdit = async (row) => {
  dialogTitle.value = '编辑秒杀场次';
  Object.assign(form, {
    id: row.id,
    name: row.name || '',
    showId: row.showId,
    ticketId: row.ticketId,
    seckillPrice: row.seckillPrice || 0,
    stock: row.stock || 0,
    startTime: row.startTime,
    endTime: row.endTime,
    status: row.status
  });
  if (row.showId) {
    try {
      const res = await ticketApi.getByShowId(row.showId);
      if (res.code === 200) {
        ticketList.value = res.data || [];
      }
    } catch (error) {
      console.error('加载票档列表失败', error);
    }
  }
  dialogVisible.value = true;
};

const handleWarmup = async (row) => {
  try {
    const res = await seckillApi.warmUpStock(row.id);
    if (res.code === 200) {
      const warmedStock = res.data?.stock ?? '?';
      ElMessage.success(`预热成功，库存: ${warmedStock}`);
      loadData(); // 刷新表格显示预热状态
    } else {
      ElMessage.error(res.message || '预热失败');
    }
  } catch (error) {
    ElMessage.error('预热失败');
  }
};

const handleBatchWarmup = async () => {
  warmingUp.value = true;
  try {
    const res = await seckillApi.batchWarmUp();
    if (res.code === 200) {
      const count = res.data?.warmedCount ?? 0;
      if (count > 0) {
        ElMessage.success(`批量预热完成，共成功 ${count} 场`);
      } else {
        ElMessage.info('没有即将开始的场次需要预热');
      }
      loadData();
    } else {
      ElMessage.error(res.message || '批量预热失败');
    }
  } catch (error) {
    ElMessage.error('批量预热失败');
  } finally {
    warmingUp.value = false;
  }
};

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除秒杀场次"${row.name}"吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      const res = await seckillApi.deleteSeckill(row.id);
      if (res.code === 200) {
        ElMessage.success('删除成功');
        loadData();
      } else {
        ElMessage.error(res.message || '删除失败');
      }
    } catch (error) {
      ElMessage.error('删除失败');
    }
  }).catch(() => {});
};

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    const submitData = {
      id: form.id,
      showId: form.showId,
      ticketId: form.ticketId,
      seckillPrice: form.seckillPrice,
      stock: form.stock,
      startTime: form.startTime,
      endTime: form.endTime,
      status: form.status
    };
    const res = form.id ? await seckillApi.updateSeckill(submitData) : await seckillApi.createSeckill(submitData);
    if (res.code === 200) {
      ElMessage.success(form.id ? '修改成功' : '添加成功');
      dialogVisible.value = false;
      loadData();
    } else {
      ElMessage.error(res.message || '操作失败');
    }
  } catch (error) {
    ElMessage.error('操作失败');
  } finally {
    submitting.value = false;
  }
};

const goBack = () => {
  router.push('/admin');
};

onMounted(() => {
  loadShowList();
  loadData();
});
</script>

<style scoped>
.seckill-manage {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.price {
  color: #ff4d4f;
  font-weight: 600;
}

.stock-warn {
  color: #e6a23c;
  font-weight: 600;
}
</style>



