<template>
  <div class="user-center">
    <div class="container">
      <h1 class="user-title">个人中心</h1>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="error" class="error">{{ error }}</div>
      <div v-else class="user-content">
        <div class="user-info">
          <div class="avatar">
            <img :src="user.avatar || defaultAvatar" :alt="user.nickname || user.username">
          </div>
          <div class="info">
            <h2>{{ user.nickname || user.username }}</h2>
            <p class="username">用户名: {{ user.username }}</p>
            <p class="email" v-if="user.email">邮箱: {{ user.email }}</p>
            <p class="phone" v-if="user.phone">手机: {{ user.phone }}</p>
          </div>
        </div>
        <div class="user-actions">
          <button class="action-btn" @click="showEdit = !showEdit">编辑个人资料</button>
          <button class="action-btn" @click="showPwd = !showPwd">重置密码</button>
          <button class="action-btn logout-btn" @click="logout">退出登录</button>
        </div>
        <div v-if="showEdit" class="form-card">
          <h3>更新资料</h3>
          <input v-model="editForm.nickname" placeholder="昵称" />
          <input v-model="editForm.phone" placeholder="手机号" />
          <input v-model="editForm.email" placeholder="邮箱" />
          <input v-model="editForm.avatar" placeholder="头像URL" />
          <button class="action-btn" @click="submitProfile" :disabled="submitting">保存</button>
        </div>
        <div v-if="showPwd" class="form-card">
          <h3>重置密码</h3>
          <input v-model="passwordForm.newPassword" type="password" placeholder="新密码" />
          <button class="action-btn" @click="submitPassword" :disabled="submitting">提交</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { reactive, ref, onMounted } from 'vue';
import { userApi } from '../../api/user';
import { useUserStore } from '../../store/user';
import { ElMessage } from 'element-plus';

export default {
  name: 'UserCenter',
  setup() {
    const user = ref({});
    const loading = ref(true);
    const error = ref('');
    const showEdit = ref(false);
    const showPwd = ref(false);
    const submitting = ref(false);
    const userStore = useUserStore();
    const defaultAvatar = 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=default%20user%20avatar%20simple%20profile%20picture&image_size=square';
    const editForm = reactive({
      nickname: '',
      phone: '',
      email: '',
      avatar: ''
    });
    const passwordForm = reactive({
      newPassword: ''
    });

    const loadUserInfo = async () => {
      try {
        loading.value = true;
        error.value = '';
        await userStore.getUserInfo();
        user.value = userStore.userInfo;
        
        editForm.nickname = user.value?.nickname || '';
        editForm.phone = user.value?.phone || '';
        editForm.email = user.value?.email || '';
        editForm.avatar = user.value?.avatar || '';
      } catch (err) {
        error.value = '加载失败，请重试';
        console.error('加载用户信息失败:', err);
      } finally {
        loading.value = false;
      }
    };

    const submitProfile = async () => {
      submitting.value = true;
      try {
        const res = await userApi.updateUser(editForm);
        if (res.code !== 200) {
          throw new Error(res.message || '更新失败');
        }
        ElMessage.success('资料更新成功');
        await loadUserInfo();
      } catch (err) {
        ElMessage.error(err.message || '更新失败');
      } finally {
        submitting.value = false;
      }
    };

    const submitPassword = async () => {
      if (!passwordForm.newPassword) {
        ElMessage.warning('请输入新密码');
        return;
      }
      submitting.value = true;
      try {
        const res = await userApi.resetPassword({ newPassword: passwordForm.newPassword });
        if (res.code !== 200) {
          throw new Error(res.message || '重置失败');
        }
        ElMessage.success('密码已重置，请重新登录');
        await logout();
      } catch (err) {
        ElMessage.error(err.message || '重置失败');
      } finally {
        submitting.value = false;
      }
    };

    const logout = async () => {
      await userStore.logout();
      window.location.href = '/login';
    };

    onMounted(() => {
      loadUserInfo();
    });

    return {
      user,
      loading,
      error,
      showEdit,
      showPwd,
      submitting,
      defaultAvatar,
      editForm,
      passwordForm,
      submitProfile,
      submitPassword,
      logout,
      loadUserInfo
    };
  }
};
</script>

<style scoped>
.user-center {
  padding: 20px 0;
}

.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 20px;
}

.user-title {
  font-size: 24px;
  margin-bottom: 30px;
  color: #333;
}

.loading, .error {
  text-align: center;
  padding: 100px 0;
  font-size: 18px;
}

.error {
  color: #ff4d4f;
}

.user-content {
  background: #f5f5f5;
  padding: 40px;
  border-radius: 8px;
}

.user-info {
  display: flex;
  margin-bottom: 40px;
}

.avatar {
  margin-right: 30px;
}

.avatar img {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.info h2 {
  font-size: 24px;
  margin-bottom: 10px;
  color: #333;
}

.username, .email, .phone {
  font-size: 14px;
  margin-bottom: 5px;
  color: #666;
}

.user-actions {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.form-card {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 6px;
  padding: 16px;
  margin-bottom: 12px;
}

.form-card h3 {
  margin-bottom: 10px;
}

.form-card input {
  display: block;
  width: 100%;
  margin-bottom: 10px;
  padding: 8px 10px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
}

.action-btn {
  padding: 10px 20px;
  border: 1px solid #d9d9d9;
  background: white;
  cursor: pointer;
  font-size: 16px;
  border-radius: 4px;
  transition: all 0.3s;
}

.action-btn:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.logout-btn {
  border-color: #ff4d4f;
  color: #ff4d4f;
}

.logout-btn:hover {
  background: #fff1f0;
}
</style>