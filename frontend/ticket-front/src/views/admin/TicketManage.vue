<template>
  <div class="ticket-manage">
    <div class="page-header">
      <div class="header-left">
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h2>票档管理 - {{ showName || '选择演出' }}</h2>
      </div>
      <el-select v-model="selectedShowId" placeholder="请选择演出" @change="onShowChange" filterable>
        <el-option
          v-for="show in showList"
          :key="show.id"
          :label="show.name"
          :value="show.id"
        />
      </el-select>
    </div>

    <!-- 票档列表 -->
    <el-card v-if="selectedShowId">
      <template #header>
        <div class="card-header">
          <span>票档列表</span>
          <el-button type="primary" size="small" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            添加票档
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="name" label="票档名称" min-width="120" />
        <el-table-column prop="price" label="价格" width="120">
          <template #default="{ row }">
            <span class="price">¥{{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalStock" label="总库存" width="100" />
        <el-table-column prop="availableStock" label="可用库存" width="100" />
        <el-table-column prop="seatRange" label="座位范围" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-empty v-else description="请先选择演出" />

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="票档名称" prop="name">
          <el-input v-model="form.name" placeholder="如：VIP票、普通票" />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :controls="false" />
        </el-form-item>
        <el-form-item label="总库存" prop="totalStock">
          <el-input-number v-model="form.totalStock" :min="0" :controls="false" />
        </el-form-item>
        <el-form-item label="座位范围" prop="seatRange">
          <el-input v-model="form.seatRange" placeholder="如：内场VIP区" />
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
import { ref, reactive, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, ArrowLeft } from '@element-plus/icons-vue';
import { showApi } from '../../api/show';
import { ticketApi } from '../../api/ticket';

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const tableData = ref([]);
const showList = ref([]);
const selectedShowId = ref(null);
const showName = ref('');
const dialogVisible = ref(false);
const dialogTitle = ref('添加票档');
const submitting = ref(false);
const formRef = ref(null);

const form = reactive({
  id: null,
  showId: null,
  name: '',
  price: 0,
  totalStock: 0,
  availableStock: 0,
  seatRange: ''
});

const rules = {
  name: [{ required: true, message: '请输入票档名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  totalStock: [{ required: true, message: '请输入总库存', trigger: 'blur' }]
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
  if (!selectedShowId.value) return;
  loading.value = true;
  try {
    const res = await ticketApi.getByShowId(selectedShowId.value);
    if (res.code === 200) {
      tableData.value = res.data || [];
    }
  } catch (error) {
    ElMessage.error('加载数据失败');
  } finally {
    loading.value = false;
  }
};

const onShowChange = (showId) => {
  const show = showList.value.find(s => s.id === showId);
  showName.value = show ? show.name : '';
  loadData();
};

const goBack = () => {
  router.push('/admin/shows');
};

const handleCreate = () => {
  dialogTitle.value = '添加票档';
  Object.keys(form).forEach(key => {
    if (key === 'showId') form[key] = selectedShowId.value;
    else if (key === 'price' || key === 'totalStock') form[key] = 0;
    else if (key !== 'availableStock') form[key] = '';
  });
  form.id = null;
  form.availableStock = 0;
  dialogVisible.value = true;
};

const handleEdit = (row) => {
  dialogTitle.value = '编辑票档';
  Object.assign(form, row);
  dialogVisible.value = true;
};

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除票档"${row.name}"吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      const res = await ticketApi.deleteTicket(row.id);
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
    form.showId = selectedShowId.value;
    // 编辑时同步更新可用库存（总库存变化时，可用库存也相应变化）
    if (form.id) {
      const oldRow = tableData.value.find(t => t.id === form.id);
      if (oldRow) {
        const diff = form.totalStock - oldRow.totalStock;
        form.availableStock = Math.max(0, oldRow.availableStock + diff);
      }
    }
    const res = form.id ? await ticketApi.updateTicket(form) : await ticketApi.createTicket(form);
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
  // 如果有 showId 参数
  if (route.query.showId) {
    selectedShowId.value = parseInt(route.query.showId);
    showName.value = route.query.showName || '';
    loadData();
  }
});
</script>

<style scoped>
.ticket-manage {
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

.header-left h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  color: #ff4d4f;
  font-weight: 600;
}
</style>
