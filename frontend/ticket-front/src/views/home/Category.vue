<template>
  <div class="page-shell category-page">
    <header class="page-heading"><div><span class="section-index">演出分类</span><h1>{{ categoryName }}</h1></div><p>{{ shows.length }} 场演出正在展示</p></header>
    <div v-if="loading" class="category-grid"><div v-for="n in 6" :key="n" class="category-skeleton skeleton" /></div>
    <div v-else-if="error" class="state-block"><div><h2>暂时无法加载</h2><p>{{ error }}</p><button class="ui-button" type="button" @click="loadCategoryShows">重新加载</button></div></div>
    <div v-else-if="!shows.length" class="state-block"><div><h2>此分类暂无演出</h2><p>可以先浏览首页的近期推荐。</p><router-link class="ui-button ui-button--primary" to="/">浏览演出</router-link></div></div>
    <div v-else class="category-grid">
      <router-link v-for="show in shows" :key="show.id" class="show-card" :to="`/show/${show.id}`" :aria-label="`查看 ${show.name}`">
        <article>
          <img :src="show.poster" :alt="show.name" />
          <div class="show-copy"><span class="show-date">{{ formatTime(show.startTime) }}</span><h2>{{ show.name }}</h2><p>{{ show.city }} · {{ show.venue }}</p><span class="show-link">查看票档 <el-icon><ArrowRight /></el-icon></span></div>
        </article>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ArrowRight } from '@element-plus/icons-vue';
import dayjs from 'dayjs';
import { showApi } from '../../api/show';
const route = useRoute(); const shows = ref([]); const loading = ref(true); const error = ref(''); const categoryName = ref('演出');
const loadCategoryShows = async () => { try { loading.value = true; error.value = ''; const raw = route.params.category; const category = typeof raw === 'string' ? decodeURIComponent(raw) : ''; categoryName.value = category || '演出'; const res = await showApi.getByCategory(category); if (res.code !== 200) throw new Error(res.message); shows.value = res.data || []; } catch (err) { error.value = err.message || '请稍后再试'; } finally { loading.value = false; } };
const formatTime = (time) => dayjs(time).format('MM月DD日 HH:mm');
onMounted(loadCategoryShows); watch(() => route.params.category, loadCategoryShows);
</script>

<style scoped>
.section-index { display:block; margin-bottom:8px; color:var(--brand); font-size:12px; font-weight:800; }.page-heading p { color:var(--ink-3); }
.category-grid { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:36px 22px; }.category-skeleton { aspect-ratio:4/5; border-radius:var(--radius); }
.show-card { min-width:0; display:block; cursor:pointer; }.show-card img { width:100%; aspect-ratio:4/3; object-fit:cover; border-radius:var(--radius); background:var(--surface-3); transition:filter 180ms ease,transform 180ms ease; }.show-card:hover img { filter:brightness(.92); transform:translateY(-2px); }
.show-copy { padding-top:14px; }.show-date { color:var(--brand); font-size:12px; font-weight:800; }.show-copy h2 { margin:6px 0 8px; font-size:19px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }.show-copy p { color:var(--ink-3); overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }.show-link { display:inline-flex; align-items:center; gap:5px; margin-top:12px; font-weight:700; }
.state-block h2 { margin-bottom:8px; }.state-block p { margin-bottom:18px; color:var(--ink-3); }
@media(max-width:900px){.category-grid{grid-template-columns:repeat(2,minmax(0,1fr));}}@media(max-width:560px){.category-grid{grid-template-columns:1fr;gap:28px}.show-card img{aspect-ratio:16/10;}}
</style>
