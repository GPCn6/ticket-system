<template>
  <main class="account-page page-shell">
    <header class="page-heading account-heading">
      <div>
        <p class="eyebrow">账户</p>
        <h1>个人中心</h1>
        <p>管理你的公开资料和账户安全设置。</p>
      </div>
    </header>

    <section v-if="loading" class="account-loading" aria-label="个人资料加载中" aria-busy="true">
      <div class="skeleton account-loading__avatar"></div>
      <div><div class="skeleton account-loading__title"></div><div class="skeleton account-loading__text"></div></div>
    </section>

    <section v-else-if="error" class="state-block account-state" role="alert">
      <div><h2>资料暂时无法加载</h2><p>{{ error }}</p><button type="button" class="ui-button ui-button--primary" @click="loadUserInfo">重新加载</button></div>
    </section>

    <template v-else>
      <section class="profile-overview" aria-label="个人资料">
        <div class="profile-avatar">
          <img v-if="user.avatar && !avatarBroken" :src="user.avatar" :alt="displayName" @error="avatarBroken = true" />
          <span v-else>{{ avatarInitial }}</span>
        </div>
        <div class="profile-copy">
          <h2>{{ displayName }}</h2>
          <p>@{{ user.username || '未设置用户名' }}</p>
          <div class="profile-contact">
            <span>{{ user.email || '未绑定邮箱' }}</span>
            <span>{{ user.phone || '未绑定手机号' }}</span>
          </div>
        </div>
        <button type="button" class="ui-button profile-edit-button" @click="togglePanel('profile')">
          {{ showEdit ? '收起编辑' : '编辑资料' }}
        </button>
      </section>

      <section v-if="showEdit" class="account-section" aria-labelledby="profile-form-title">
        <div class="section-heading"><div><p class="section-label">公开资料</p><h2 id="profile-form-title">编辑个人资料</h2></div></div>
        <form class="account-form" @submit.prevent="submitProfile">
          <div class="field-grid">
            <div class="form-group"><label for="nickname">昵称</label><input id="nickname" v-model.trim="editForm.nickname" class="ui-input" type="text" placeholder="请输入昵称" :disabled="submitting" /></div>
            <div class="form-group"><label for="phone">手机号</label><input id="phone" v-model.trim="editForm.phone" class="ui-input" type="tel" autocomplete="tel" placeholder="请输入手机号" :disabled="submitting" /></div>
            <div class="form-group"><label for="email">邮箱</label><input id="email" v-model.trim="editForm.email" class="ui-input" type="email" autocomplete="email" placeholder="name@example.com" :disabled="submitting" /></div>
            <div class="form-group"><label for="avatar">头像地址</label><input id="avatar" v-model.trim="editForm.avatar" class="ui-input" type="url" placeholder="https://example.com/avatar.jpg" :disabled="submitting" /></div>
          </div>
          <div class="form-actions"><button type="submit" class="ui-button ui-button--primary" :disabled="submitting">{{ submitting ? '正在保存' : '保存资料' }}</button><button type="button" class="ui-button" :disabled="submitting" @click="showEdit = false">取消</button></div>
        </form>
      </section>

      <section class="account-section security-section" aria-labelledby="security-title">
        <div class="section-heading">
          <div><p class="section-label">账户安全</p><h2 id="security-title">密码与会话</h2></div>
          <button type="button" class="ui-button" @click="togglePanel('password')">{{ showPwd ? '收起' : '重置密码' }}</button>
        </div>
        <form v-if="showPwd" class="password-form" @submit.prevent="submitPassword">
          <div class="form-group"><label for="newPassword">新密码</label><input id="newPassword" v-model="passwordForm.newPassword" class="ui-input" type="password" autocomplete="new-password" placeholder="请输入新密码" :disabled="submitting" required /></div>
          <div class="form-actions"><button type="submit" class="ui-button ui-button--primary" :disabled="submitting">{{ submitting ? '正在提交' : '确认重置' }}</button></div>
        </form>
        <p v-else class="section-description">重置密码后，需要使用新密码重新登录。</p>
      </section>

      <section class="account-section logout-section" aria-labelledby="logout-title">
        <div><p class="section-label">会话</p><h2 id="logout-title">退出当前账户</h2><p class="section-description">此操作会清除当前设备上的登录状态。</p></div>
        <button type="button" class="ui-button danger-button" :disabled="submitting" @click="logout">退出登录</button>
      </section>
    </template>
  </main>
</template>

<script>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { userApi } from '../../api/user';
import { useUserStore } from '../../store/user';

export default {
  name: 'UserCenter',
  setup() {
    const user = ref({});
    const loading = ref(true);
    const error = ref('');
    const showEdit = ref(false);
    const showPwd = ref(false);
    const submitting = ref(false);
    const avatarBroken = ref(false);
    const userStore = useUserStore();
    const router = useRouter();
    const editForm = reactive({ nickname: '', phone: '', email: '', avatar: '' });
    const passwordForm = reactive({ newPassword: '' });
    const displayName = computed(() => user.value?.nickname || user.value?.username || '未命名用户');
    const avatarInitial = computed(() => displayName.value.slice(0, 1).toUpperCase());

    const applyUserToForm = () => {
      editForm.nickname = user.value?.nickname || '';
      editForm.phone = user.value?.phone || '';
      editForm.email = user.value?.email || '';
      editForm.avatar = user.value?.avatar || '';
      avatarBroken.value = false;
    };

    const loadUserInfo = async () => {
      try {
        loading.value = true;
        error.value = '';
        await userStore.getUserInfo();
        user.value = userStore.userInfo || {};
        applyUserToForm();
      } catch (err) {
        error.value = err.message || '加载失败，请稍后重试。';
        console.error('加载用户信息失败:', err);
      } finally { loading.value = false; }
    };

    const togglePanel = (panel) => {
      if (panel === 'profile') { showEdit.value = !showEdit.value; showPwd.value = false; }
      if (panel === 'password') { showPwd.value = !showPwd.value; showEdit.value = false; }
    };

    const submitProfile = async () => {
      submitting.value = true;
      try {
        const res = await userApi.updateUser(editForm);
        if (res.code !== 200) throw new Error(res.message || '更新失败');
        ElMessage.success('资料已更新');
        showEdit.value = false;
        await loadUserInfo();
      } catch (err) { ElMessage.error(err.message || '更新失败，请重试'); }
      finally { submitting.value = false; }
    };

    const submitPassword = async () => {
      if (!passwordForm.newPassword) { ElMessage.warning('请输入新密码'); return; }
      submitting.value = true;
      try {
        const res = await userApi.resetPassword({ newPassword: passwordForm.newPassword });
        if (res.code !== 200) throw new Error(res.message || '重置失败');
        ElMessage.success('密码已重置，请重新登录');
        await logout();
      } catch (err) { ElMessage.error(err.message || '重置失败，请重试'); }
      finally { submitting.value = false; }
    };

    const logout = async () => {
      await userStore.logout();
      await router.replace('/login');
    };

    onMounted(loadUserInfo);
    return { user, loading, error, showEdit, showPwd, submitting, avatarBroken, editForm, passwordForm, displayName, avatarInitial, loadUserInfo, togglePanel, submitProfile, submitPassword, logout };
  }
};
</script>

<style scoped>
.account-page { min-height: 60vh; }.eyebrow { margin-bottom: 8px; color: var(--brand); font-size: 12px; font-weight: 700; }.account-heading { margin-bottom: 28px; }.profile-overview { display: grid; grid-template-columns: 80px minmax(0, 1fr) auto; gap: 18px; align-items: center; padding: 0 0 28px; border-bottom: 1px solid var(--line); }.profile-avatar { width: 80px; height: 80px; display: grid; place-items: center; overflow: hidden; border: 1px solid var(--line-strong); border-radius: 50%; background: var(--surface-3); color: var(--brand); font-size: 28px; font-weight: 760; }.profile-avatar img { width: 100%; height: 100%; object-fit: cover; }.profile-copy h2 { font-size: 24px; }.profile-copy > p { margin-top: 4px; color: var(--ink-2); }.profile-contact { display: flex; flex-wrap: wrap; gap: 5px 18px; margin-top: 10px; color: var(--ink-3); font-size: 13px; }.profile-edit-button { min-width: 104px; }.account-section { padding: 28px 0; border-bottom: 1px solid var(--line); }.section-heading, .logout-section { display: flex; justify-content: space-between; gap: 20px; align-items: start; }.section-label { margin-bottom: 8px; color: var(--ink-3); font-size: 12px; font-weight: 700; }.section-heading h2, .logout-section h2 { font-size: 19px; }.section-description { margin-top: 8px; color: var(--ink-2); }.account-form { margin-top: 20px; }.field-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }.form-group { display: grid; gap: 8px; }.form-group label { font-size: 13px; font-weight: 700; }.ui-input { box-sizing: border-box; }.form-actions { display: flex; gap: 8px; margin-top: 20px; }.password-form { width: min(100%, 400px); margin-top: 20px; }.danger-button { border-color: #bd3f30; color: #a63125; }.danger-button:hover { border-color: #a63125; background: #fff4f2; }.account-loading { display: flex; gap: 18px; align-items: center; min-height: 128px; padding-bottom: 28px; border-bottom: 1px solid var(--line); }.account-loading__avatar { width: 80px; height: 80px; border-radius: 50%; }.account-loading__title { width: 180px; height: 20px; border-radius: 3px; }.account-loading__text { width: 280px; max-width: 60vw; height: 12px; margin-top: 12px; border-radius: 3px; }.account-state h2 { margin-bottom: 8px; font-size: 20px; }.account-state p { margin-bottom: 18px; }
@media (max-width: 620px) { .profile-overview { grid-template-columns: 64px minmax(0, 1fr); }.profile-avatar { width: 64px; height: 64px; font-size: 23px; }.profile-edit-button { grid-column: 1 / -1; width: 100%; }.field-grid { grid-template-columns: 1fr; }.logout-section { align-items: stretch; flex-direction: column; }.logout-section .ui-button { width: 100%; }.form-actions .ui-button { flex: 1; } }
</style>
