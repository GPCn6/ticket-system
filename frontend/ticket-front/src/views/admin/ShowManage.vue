<template>
  <div class="show-manage">
    <div class="page-header">
      <div class="header-left">
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h2>演出管理</h2>
      </div>
      <el-button type="primary" @click="handleCreate">
        <el-icon><Plus /></el-icon>
        添加演出
      </el-button>
    </div>

    <!-- 搜索筛选 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="演出名称">
          <el-input v-model="searchForm.name" placeholder="请输入演出名称" clearable />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.category" placeholder="请选择分类" clearable>
            <el-option label="演唱会" value="演唱会" />
            <el-option label="话剧" value="话剧" />
            <el-option label="体育" value="体育" />
            <el-option label="儿童剧" value="儿童剧" />
            <el-option label="音乐会" value="音乐会" />
            <el-option label="舞蹈" value="舞蹈" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="演出名称" min-width="180" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="venue" label="场馆" width="150" />
        <el-table-column prop="city" label="城市" width="100" />
        <el-table-column prop="startTime" label="演出时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" link size="small" @click="handleManageTickets(row)">票档</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="演出名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入演出名称" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类">
            <el-option label="演唱会" value="演唱会" />
            <el-option label="话剧" value="话剧" />
            <el-option label="体育" value="体育" />
            <el-option label="儿童剧" value="儿童剧" />
            <el-option label="音乐会" value="音乐会" />
            <el-option label="舞蹈" value="舞蹈" />
          </el-select>
        </el-form-item>
        <el-form-item label="场馆" prop="venue">
          <el-input v-model="form.venue" placeholder="请输入场馆名称" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入详细地址" />
        </el-form-item>
        <el-form-item label="演出时间" prop="showTime">
          <el-date-picker
            v-model="form.showTime"
            type="datetime"
            placeholder="选择演出时间"
            format="YYYY-MM-DD HH:mm"
          />
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model="form.city" placeholder="请输入城市" />
        </el-form-item>
        <el-form-item label="时长" prop="duration">
          <el-input v-model="form.duration" placeholder="如：120分钟" />
        </el-form-item>
        <el-form-item label="简介" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入演出简介" />
        </el-form-item>
        <el-form-item label="图片" prop="poster">
          <el-input v-model="form.poster" placeholder="请输入图片URL" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">上架</el-radio>
            <el-radio :label="0">下架</el-radio>
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
import { ref, reactive, onMounted } from 'vue';
import dayjs from 'dayjs';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, ArrowLeft } from '@element-plus/icons-vue';
import { showApi } from '../../api/show';

const router = useRouter();

const loading = ref(false);
const tableData = ref([]);
const dialogVisible = ref(false);
const dialogTitle = ref('添加演出');
const submitting = ref(false);
const formRef = ref(null);

const searchForm = reactive({
  name: '',
  category: '',
  status: null
});

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
});

const form = reactive({
  id: null,
  name: '',
  category: '',
  venue: '',
  address: '',
  showTime: '',
  city: '',
  duration: '',
  description: '',
  poster: '',
  status: 1
});

const rules = {
  name: [{ required: true, message: '请输入演出名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  venue: [{ required: true, message: '请输入场馆名称', trigger: 'blur' }],
  showTime: [{ required: true, message: '请选择演出时间', trigger: 'change' }]
};

const formatDateTime = (date) => {
  if (!date) return '-';
  return dayjs(date).format('YYYY-MM-DD HH:mm');
};



const loadData = async () => {
  loading.value = true;
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      ...searchForm
    };
    const res = await showApi.getShowList(params);
    if (res.code === 200) {
      tableData.value = res.data.records || res.data.list || [];
      pagination.total = res.data.total || 0;
      // 兼容 pageSize 和 size 两种字段名
      if (res.data.size) pagination.size = res.data.size;
      if (res.data.pageSize) pagination.size = res.data.pageSize;
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
  searchForm.name = '';
  searchForm.category = '';
  searchForm.status = null;
  handleSearch();
};

const handleCreate = () => {
  dialogTitle.value = '添加演出';
  Object.keys(form).forEach(key => {
    if (key === 'status') form[key] = 1;
    else form[key] = '';
  });
  form.id = null;
  dialogVisible.value = true;
};

const handleEdit = (row) => {
  dialogTitle.value = '编辑演出';
  Object.assign(form, row);
  dialogVisible.value = true;
};

const handleManageTickets = (row) => {
  // 跳转到票档管理页面
  window.location.href = `/admin/tickets?showId=${row.id}&showName=${encodeURIComponent(row.name)}`;
};

const goBack = () => {
  router.push('/admin');
};

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除演出"${row.name}"吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      const res = await showApi.deleteShow(row.id);
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
    const res = form.id ? await showApi.updateShow(form) : await showApi.createShow(form);
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
  loadData();
});
</script>

<style scoped>
.show-manage {
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

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.filter-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
