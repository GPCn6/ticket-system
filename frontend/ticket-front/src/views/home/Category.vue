<template>
  <div class="category">
    <div class="container">
      <h1 class="category-title">{{ categoryName }}</h1>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="error" class="error">{{ error }}</div>
      <div v-else class="category-content">
        <div v-if="shows.length === 0" class="empty">暂无演出</div>
        <div class="show-list">
          <div class="show-item" v-for="show in shows" :key="show.id">
            <img :src="show.poster" :alt="show.name" class="show-poster" />
            <div class="show-info">
              <h3>{{ show.name }}</h3>
              <p class="show-description">{{ show.description }}</p>
              <div class="show-meta">
                <span class="meta-item">城市: {{ show.city }}</span>
                <span class="meta-item">场馆: {{ show.venue }}</span>
                <span class="meta-item">时间: {{ formatTime(show.startTime) }}</span>
              </div>
              <button type="button" class="detail-btn" @click="goToDetail(show.id)">查看详情</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { showApi } from '../../api/show';
import dayjs from 'dayjs';

export default {
  name: 'Category',
  setup() {
    const route = useRoute();
    const router = useRouter();
    const shows = ref([]);
    const loading = ref(true);
    const error = ref('');
    const categoryName = ref('');

    const loadCategoryShows = async () => {
      try {
        loading.value = true;
        error.value = '';
        const raw = route.params.category;
        const category = typeof raw === 'string' ? decodeURIComponent(raw) : '';
        categoryName.value = category || '分类';
        const data = await showApi.getByCategory(category);
        shows.value = data.code === 200 && data.data ? data.data : [];
      } catch (err) {
        error.value = '加载失败，请重试';
        console.error(err);
      } finally {
        loading.value = false;
      }
    };

    const formatTime = (time) => {
      return dayjs(time).format('YYYY-MM-DD HH:mm');
    };

    const goToDetail = (showId) => {
      router.push(`/show/${showId}`);
    };

    onMounted(loadCategoryShows);
    watch(() => route.params.category, loadCategoryShows);

    return {
      shows,
      loading,
      error,
      categoryName,
      formatTime,
      goToDetail
    };
  }
};
</script>

<style scoped>
.category {
  padding: 20px 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.category-title {
  font-size: 24px;
  margin-bottom: 30px;
  color: #333;
}

.loading,
.error,
.empty {
  text-align: center;
  padding: 100px 0;
  font-size: 18px;
}

.error {
  color: #ff4d4f;
}

.show-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.show-item {
  display: flex;
  background: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
  transition: transform 0.3s;
}

.show-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.show-poster {
  width: 120px;
  height: 160px;
  object-fit: cover;
  border-radius: 8px;
  margin-right: 20px;
}

.show-info {
  flex: 1;
}

.show-info h3 {
  font-size: 18px;
  margin-bottom: 10px;
  color: #333;
}

.show-description {
  font-size: 14px;
  line-height: 1.4;
  margin-bottom: 15px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.show-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
  color: #999;
  margin-bottom: 15px;
}

.detail-btn {
  padding: 8px 16px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.detail-btn:hover {
  background: #40a9ff;
}
</style>
