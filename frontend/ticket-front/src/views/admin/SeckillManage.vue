<template>
  <div class="seckill-manage">
    <div class="page-header">
      <h2>秒杀场次管理</h2>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        添加秒杀场次
      </el-button>
    </div>

    <!-- 数据表格 -->
    <el-card>
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="场次名称" min-width="220">
          <template #default="{ row }">
            <div>
              <div style="font-weight: 600;">{{ row.show?.name || '-' }}</div>
              <div style="font-size: 12px; color: #909399; margin-top: 2px;">
                {{ row.ticket ? `${row.ticket.name} (¥${row.ticket.price})` : '-' }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="seckillPrice" label="秒杀价" width="100">
          <template #default="{ row }">
            <span class="price">¥{{ row.seckillPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="100" />
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
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row)">
              {{ getStatusText(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" link size="small" @click="handleWarmup(row)">预热</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑对话框 -->
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
              :label="`${ticket.name} (¥${ticket.price})`"
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
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { seckillApi } from '../../api/seckill';
import { showApi } from '../../api/show';
import { ticketApi } from '../../api/ticket';

const loading = ref(false);
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

// 监听演出变化，加载对应的票档
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
  return new Date(date).format('yyyy-MM-dd hh:mm');
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
  // 加载票档列表
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
      ElMessage.success('预热成功');
    } else {
      ElMessage.error(res.message || '预热失败');
    }
  } catch (error) {
    ElMessage.error('预热失败');
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
    // 构造提交数据，移除 name 字段（由后端自动获取演出名称）
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

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.price {
  color: #ff4d4f;
  font-weight: 600;
}
</style>
