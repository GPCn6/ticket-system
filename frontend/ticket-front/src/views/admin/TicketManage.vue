<template>
  <main class="page-shell admin-page ticket-manage">
    <header class="admin-page-heading">
      <div class="admin-title-group">
        <el-tooltip content="返回运营总览" placement="bottom">
          <el-button class="back-button" :icon="ArrowLeft" circle aria-label="返回运营总览" @click="goBack" />
        </el-tooltip>
        <div>
          <p class="admin-eyebrow">INVENTORY CONTROL</p>
          <h1>票档管理</h1>
          <p>{{ showName ? `正在维护：${showName}` : '选择演出后，查看并维护可售票档。' }}</p>
        </div>
      </div>
      <el-select class="show-switcher" v-model="selectedShowId" placeholder="选择演出" @change="onShowChange" filterable>
        <el-option
          v-for="show in showList"
          :key="show.id"
          :label="show.name"
          :value="show.id"
        />
      </el-select>
    </header>

    <section v-if="selectedShowId" class="table-frame">
      <div class="table-frame-heading">
        <div>
          <h2>票档列表</h2>
          <span>{{ tableData.length }} 个票档</span>
        </div>
        <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            添加票档
        </el-button>
      </div>

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
    </section>

    <section v-else class="empty-frame">
      <el-empty description="选择一场演出后管理票档" />
    </section>

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
  </main>
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
  router.push('/admin');
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
.admin-page { padding-block: 32px 56px; }
.admin-page-heading, .admin-title-group, .table-frame-heading { display: flex; align-items: center; }
.admin-page-heading { justify-content: space-between; gap: 20px; margin-bottom: 28px; }
.admin-title-group { gap: 12px; min-width: 0; }
.admin-eyebrow { margin: 0 0 4px; color: var(--ink-3); font-size: 11px; font-weight: 750; letter-spacing: .08em; }
.admin-title-group h1 { margin: 0; font-size: 26px; font-weight: 760; }
.admin-title-group p:not(.admin-eyebrow) { margin: 5px 0 0; color: var(--ink-2); font-size: 13px; }
.back-button { flex: 0 0 auto; }
.show-switcher { width: min(300px, 100%); flex: 0 1 300px; }
.table-frame, .empty-frame { overflow: hidden; border: 1px solid var(--line); border-radius: var(--radius); background: var(--surface); }
.table-frame-heading { justify-content: space-between; min-height: 58px; gap: 16px; padding: 0 18px; border-bottom: 1px solid var(--line); }
.table-frame-heading h2 { margin: 0; font-size: 15px; font-weight: 720; }
.table-frame-heading span { margin-left: 10px; color: var(--ink-3); font-size: 12px; }
.table-frame :deep(.el-table) { --el-table-border-color: var(--line); }
.empty-frame { min-height: 280px; display: grid; place-items: center; }
.price { color: var(--brand); font-weight: 720; font-variant-numeric: tabular-nums; }
@media (max-width: 768px) {
  .admin-page-heading { align-items: stretch; flex-direction: column; }
  .show-switcher { width: 100%; max-width: none; }
  .table-frame-heading { align-items: flex-start; flex-direction: column; padding: 14px; }
  .table-frame-heading > .el-button { width: 100%; }
}
</style>
