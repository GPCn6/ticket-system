<template>
  <div class="search">
    <div class="container">
      <div class="search-header">
        <h1 class="search-title">搜索结果��</h1>
        <div class="search-box">
          <input 
            type="text" 
            v-model="keyword" 
            placeholder="请输入演出名称或关键词"
            @keyup.enter="search"
          >
          <button class="search-btn" @click="search">搜索</button>
        </div>
      </div>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else-if="error" class="error">{{ error }}</div>
      <div v-else class="search-results">
        <div v-if="shows.length === 0" class="empty">暂无搜索结果果��</div>
        <div class="show-list">
          <div class="show-item" v-for="show in shows" :key="show.id" @click="goToDetail(show.id)">
            <img :src="show.poster" :alt="show.name" class="show-poster">
            <div class="show-info">
              <h3>{{ show.name }}</h3>
              <p class="show-description">{{ show.description }}</p>
              <div class="show-meta">
                <span class="meta-item">城市: {{ show.city }}</span>
                <span class="meta-item">场馆: {{ show.venue }}</span>
                <span class="meta-item">时间: {{ formatTime(show.startTime) }}</span>
              </div>
              <button class="detail-btn" @click.stop="goToDetail(show.id)">查看详情</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { showApi } from '../../api/show';
import dayjs from 'dayjs';

const router = useRouter();
const route = useRoute();

const keyword = ref('');
const shows = ref([]);
const loading = ref(true);
const error = ref('');

const loadSearchResults = async () => {
  try {
    loading.value = true;
    error.value = '';
    const searchKeyword = route.query.keyword;
    if (searchKeyword) {
      keyword.value = searchKeyword;
      const res = await showApi.searchShows(searchKeyword);
      shows.value = res.code === 200 && res.data ? res.data : [];
    }
  } catch (err) {
    error.value = '搜索失败，请重试';
    console.error(err);
  } finally {
    loading.value = false;
  }
};

const search = () => {
  if (keyword.value) {
    router.push({ name: 'Search', query: { keyword: keyword.value } });
  }
};

const formatTime = (time) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm');
};

const goToDetail = (showId) => {
  router.push({ name: 'ShowDetail', params: { id: showId } });
};

onMounted(() => {
  loadSearchResults();
});
</script>

<style scoped>
.search {
  padding: 20px 0;
}
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}
.search-header { margin-bottom: 30px; }
.search-title { font-size: 24px; margin-bottom: 20px; color: #333; }
.search-box { display: flex; max-width: 600px; }
.search-box input {
  flex: 1; padding: 10px; border: 1px solid #d9d9d9;
  border-radius: 4px 0 0 4px; font-size: 16px;
  transition: border-color 0.3s;
}
.search-box input:focus { outline: none; border-color: #1890ff; }
.search-btn {
  padding: 0 20px; background: #1890ff; color: white; border: none;
  border-radius: 0 4px 4px 0; cursor: pointer; font-size: 16px;
  transition: background 0.3s;
}
.search-btn:hover { background: #40a9ff; }
.loading, .error, .empty { text-align: center; padding: 100px 0; font-size: 18px; }
.error { color: #ff4d4f; }
.show-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}
.show-item {
  display: flex; background: #f5f5f5; padding: 20px;
  border-radius: 8px; cursor: pointer;
  transition: transform 0.3s;
}
.show-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0,0,0,0.1);
}
.show-poster {
  width: 120px; height: 160px; object-fit: cover;
  border-radius: 8px; margin-right: 20px;
}
.show-info { flex: 1; }
.show-info h3 { font-size: 18px; margin-bottom: 10px; color: #333; }
.show-description {
  font-size: 14px; line-height: 1.4; margin-bottom: 15px; color: #666;
  overflow: hidden; text-overflow: ellipsis;
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical;
}
.show-meta { display: flex; flex-wrap: wrap; gap: 10px; font-size: 12px; color: #999; margin-bottom: 15px; }
.detail-btn { padding: 8px 16px; background: #1890ff; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 14px; }
.detail-btn:hover { background: #40a9ff; }
</style>
