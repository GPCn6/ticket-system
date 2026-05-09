<template>
  <div class="seckill">
    <div class="container">
      <h1 class="seckill-title">限时抢购</h1>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="error" class="error">{{ error }}</div>
      <div v-else class="seckill-content">
        <div class="active-sessions">
          <h2>正在抢购</h2>
          <div v-if="!activeSessions.length" class="hint">暂无进行中的场次</div>
          <div class="session-list">
            <div class="session-item" v-for="session in activeSessions" :key="session.id">
              <div class="session-info">
                <h3>{{ session.show?.name || '演出' }}</h3>
                <p class="session-time">时间: {{ formatTime(session.startTime) }} - {{ formatTime(session.endTime) }}</p>
                <p class="session-stock">剩余: {{ session.stock }}张</p>
              </div>
              <div class="session-price">
                <span class="price" v-if="session.seckillPrice != null">¥{{ session.seckillPrice }}</span>
                <button type="button" class="seckill-btn" @click="goToSeckillDetail(session.id)">立即抢购</button>
              </div>
            </div>
          </div>
        </div>
        <div class="upcoming-sessions">
          <h2>即将开始</h2>
          <div v-if="!upcomingSessions.length" class="hint">暂无即将开始的场次</div>
          <div class="session-list">
            <div class="session-item" v-for="session in upcomingSessions" :key="session.id">
              <div class="session-info">
                <h3>{{ session.show?.name || '演出' }}</h3>
                <p class="session-time">开始时间: {{ formatTime(session.startTime) }}</p>
                <p class="session-countdown">倒计时: {{ countdown(session.startTime) }}</p>
              </div>
              <div class="session-price">
                <span class="price" v-if="session.seckillPrice != null">¥{{ session.seckillPrice }}</span>
                <button type="button" class="disabled-btn">即将开始</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { seckillApi } from '../../api/seckill';
import dayjs from 'dayjs';

export default {
  name: 'Seckill',
  setup() {
    const router = useRouter();
    const activeSessions = ref([]);
    const upcomingSessions = ref([]);
    const loading = ref(true);
    const error = ref('');
    let timer = null;

    const loadSessions = async () => {
      try {
        loading.value = true;
        error.value = '';
        const activeData = await seckillApi.getActiveSessions();
        const upcomingData = await seckillApi.getUpcomingSessions();
        activeSessions.value = activeData.code === 200 && activeData.data ? activeData.data : [];
        upcomingSessions.value = upcomingData.code === 200 && upcomingData.data ? upcomingData.data : [];
      } catch (err) {
        error.value = '加载失败，请重试';
        console.error(err);
      } finally {
        loading.value = false;
      }
    };

    const formatTime = (time) => dayjs(time).format('YYYY-MM-DD HH:mm');

    const countdown = (startTime) => {
      const now = dayjs();
      const start = dayjs(startTime);
      const diff = start.diff(now, 'second');
      if (diff <= 0) return '已开始';
      const hours = Math.floor(diff / 3600);
      const minutes = Math.floor((diff % 3600) / 60);
      const seconds = diff % 60;
      return `${hours}小时${minutes}分钟${seconds}秒`;
    };

    const goToSeckillDetail = (sessionId) => {
      router.push(`/seckill/${sessionId}`);
    };

    onMounted(() => {
      loadSessions();
      timer = setInterval(() => {
        upcomingSessions.value = [...upcomingSessions.value];
      }, 1000);
    });

    onUnmounted(() => {
      if (timer) clearInterval(timer);
    });

    return {
      activeSessions,
      upcomingSessions,
      loading,
      error,
      formatTime,
      countdown,
      goToSeckillDetail
    };
  }
};
</script>

<style scoped>
.seckill {
  padding: 20px 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.seckill-title {
  font-size: 32px;
  text-align: center;
  margin-bottom: 40px;
  color: #333;
}

.loading,
.error,
.hint {
  text-align: center;
  padding: 24px 0;
  font-size: 16px;
}

.hint {
  color: #999;
}

.error {
  color: #ff4d4f;
}

.active-sessions,
.upcoming-sessions {
  margin-bottom: 40px;
}

.active-sessions h2,
.upcoming-sessions h2 {
  font-size: 20px;
  margin-bottom: 20px;
  color: #333;
  border-left: 4px solid #ff4d4f;
  padding-left: 10px;
}

.session-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.session-item {
  background: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
  transition: transform 0.3s;
}

.session-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.session-info h3 {
  font-size: 18px;
  margin-bottom: 10px;
  color: #333;
}

.session-time,
.session-stock,
.session-countdown {
  font-size: 14px;
  margin-bottom: 5px;
  color: #666;
}

.session-stock {
  color: #ff4d4f;
  font-weight: bold;
}

.session-price {
  margin-top: 20px;
  text-align: right;
}

.price {
  font-size: 24px;
  font-weight: bold;
  color: #ff4d4f;
  display: block;
  margin-bottom: 10px;
}

.seckill-btn,
.disabled-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  transition: background 0.3s;
}

.seckill-btn {
  background: #ff4d4f;
  color: white;
}

.seckill-btn:hover {
  background: #ff7875;
}

.disabled-btn {
  background: #d9d9d9;
  color: #999;
  cursor: not-allowed;
}
</style>
