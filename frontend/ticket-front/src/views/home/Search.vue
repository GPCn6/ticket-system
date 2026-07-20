<template>
  <div class="page-shell search-page">
    <header class="search-heading"><span>搜索演出</span><h1>{{ currentKeyword ? `“${currentKeyword}”` : '找到你的下一场现场' }}</h1><form @submit.prevent="search"><input v-model="keyword" aria-label="搜索关键词" placeholder="输入演出、艺人或场馆" /><button type="submit" aria-label="搜索"><el-icon><Search /></el-icon></button></form></header>
    <div class="result-meta"><strong>{{ loading ? '正在查找' : `${shows.length} 个结果` }}</strong><router-link to="/">返回发现</router-link></div>
    <div v-if="loading" class="result-list"><div v-for="n in 4" :key="n" class="result-skeleton"><span class="skeleton"/><div><i class="skeleton"/><i class="skeleton short"/></div></div></div>
    <div v-else-if="error" class="state-block"><div><h2>搜索暂时不可用</h2><p>{{ error }}</p><button class="ui-button" type="button" @click="loadSearchResults">重新搜索</button></div></div>
    <div v-else-if="!shows.length" class="state-block"><div><h2>没有找到匹配演出</h2><p>尝试缩短关键词，或按演出类型浏览。</p><router-link class="ui-button ui-button--primary" to="/">浏览近期演出</router-link></div></div>
    <div v-else class="result-list">
      <article v-for="show in shows" :key="show.id" class="result-row" @click="goToDetail(show.id)">
        <img :src="show.poster" :alt="show.name" /><div class="result-date"><strong>{{ day(show.startTime) }}</strong><span>{{ month(show.startTime) }}</span></div>
        <div class="result-copy"><span>{{ show.category || '现场演出' }}</span><h2>{{ show.name }}</h2><p>{{ show.city }} · {{ show.venue }}</p><small>{{ show.description }}</small></div>
        <button class="ui-icon-button" type="button" title="查看详情"><el-icon><ArrowRight /></el-icon></button>
      </article>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'; import { useRoute,useRouter } from 'vue-router'; import { ArrowRight,Search } from '@element-plus/icons-vue'; import dayjs from 'dayjs'; import { showApi } from '../../api/show';
const router=useRouter(),route=useRoute(); const keyword=ref(''),shows=ref([]),loading=ref(true),error=ref(''); const currentKeyword=computed(()=>typeof route.query.keyword==='string'?route.query.keyword:'');
const loadSearchResults=async()=>{try{loading.value=true;error.value='';keyword.value=currentKeyword.value;if(!currentKeyword.value){shows.value=[];return;}const res=await showApi.searchShows(currentKeyword.value);if(res.code!==200)throw new Error(res.message);shows.value=res.data||[];}catch(err){error.value=err.message||'请稍后再试';}finally{loading.value=false;}};
const search=()=>{const value=keyword.value.trim();if(value)router.push({name:'Search',query:{keyword:value}});}; const day=(time)=>dayjs(time).format('DD'); const month=(time)=>dayjs(time).format('MM月'); const goToDetail=(id)=>router.push({name:'ShowDetail',params:{id}});
onMounted(loadSearchResults);watch(()=>route.query.keyword,loadSearchResults);
</script>

<style scoped>
.search-heading>span{color:var(--brand);font-size:12px;font-weight:800}.search-heading h1{margin:8px 0 24px;font-size:clamp(30px,5vw,48px)}.search-heading form{max-width:720px;height:52px;display:flex;border:1px solid var(--line-strong);border-radius:var(--radius);background:var(--surface)}.search-heading input{min-width:0;flex:1;padding:0 16px;border:0;outline:0;background:transparent;font-size:16px}.search-heading button{width:52px;border:0;color:#fff;background:var(--ink);cursor:pointer}.result-meta{display:flex;justify-content:space-between;margin:38px 0 12px;padding-bottom:12px;border-bottom:1px solid var(--line)}.result-meta a{color:var(--brand);font-weight:700}.result-row{min-height:172px;display:grid;grid-template-columns:128px 58px 1fr 40px;align-items:center;gap:22px;padding:18px 0;border-bottom:1px solid var(--line);cursor:pointer}.result-row>img{width:128px;height:136px;object-fit:cover;border-radius:var(--radius)}.result-date strong,.result-date span{display:block}.result-date strong{font-size:28px}.result-date span,.result-copy>span{color:var(--brand);font-size:12px;font-weight:800}.result-copy h2{margin:5px 0 8px;font-size:20px}.result-copy p{color:var(--ink-2)}.result-copy small{display:-webkit-box;max-width:70ch;margin-top:8px;color:var(--ink-3);overflow:hidden;-webkit-line-clamp:2;-webkit-box-orient:vertical}.result-skeleton{display:grid;grid-template-columns:128px 1fr;gap:22px;padding:18px 0;border-bottom:1px solid var(--line)}.result-skeleton>span{height:136px;border-radius:var(--radius)}.result-skeleton div{padding-top:24px}.result-skeleton i{display:block;width:60%;height:18px;margin-bottom:14px}.result-skeleton .short{width:35%}
@media(max-width:650px){.result-row{grid-template-columns:88px 1fr 40px;gap:14px}.result-row>img{width:88px;height:118px}.result-date{display:none}.result-copy small{display:none}.result-copy h2{font-size:17px}.result-skeleton{grid-template-columns:88px 1fr}.result-skeleton>span{height:118px}}
</style>
