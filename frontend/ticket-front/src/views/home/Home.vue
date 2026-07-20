<template>
  <main class="home-page">
    <section class="featured-stage" :style="featuredStageStyle">
      <div class="featured-stage__shade"></div>
      <div class="container featured-stage__content">
        <p class="featured-stage__eyebrow">本周主推</p>
        <h1>{{ featuredShow?.name || '把现场留给值得等待的夜晚' }}</h1>
        <p class="featured-stage__facts">
          <span>{{ featuredShow ? formatDate(featuredShow.startTime) : '演出、戏剧、体育与展览' }}</span>
          <span v-if="featuredShow?.venue">{{ featuredShow.venue }}</span>
        </p>
        <button class="stage-action" type="button" @click="openFeatured">
          {{ featuredShow ? '查看票档' : '浏览演出' }}
          <el-icon><ArrowRight /></el-icon>
        </button>
      </div>
    </section>

    <section class="discovery-band" aria-label="演出发现">
      <div class="container discovery-band__inner">
        <div class="discovery-group">
          <span class="discovery-label">时间</span>
          <button
            v-for="option in timeOptions"
            :key="option.id"
            type="button"
            class="discovery-option"
            :class="{ 'is-active': activeTime === option.id }"
            @click="activeTime = option.id"
          >
            {{ option.label }}
          </button>
        </div>
        <div class="discovery-group discovery-group--categories">
          <span class="discovery-label">类型</span>
          <button
            v-for="category in categories"
            :key="category.id"
            type="button"
            class="discovery-option"
            @click="goToCategory(category.name)"
          >
            <el-icon><component :is="category.icon" /></el-icon>
            {{ category.name }}
          </button>
        </div>
      </div>
    </section>

    <section class="content-section content-section--on-sale">
      <div class="container">
        <div class="section-heading">
          <div>
            <p class="section-kicker">正在开票</p>
            <h2>当下值得去的现场</h2>
          </div>
          <button class="text-action" type="button" @click="goToCategory('演唱会')">
            查看全部
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>

        <div v-if="loading" class="on-sale-grid on-sale-grid--skeleton" aria-label="正在加载演出">
          <div v-for="index in 6" :key="index" class="show-card-skeleton">
            <div class="skeleton show-card-skeleton__image"></div>
            <div class="skeleton show-card-skeleton__line"></div>
            <div class="skeleton show-card-skeleton__line show-card-skeleton__line--short"></div>
          </div>
        </div>

        <div v-else-if="loadError" class="state-block home-state">
          <div>
            <h3>演出列表暂时无法加载</h3>
            <p>请检查网络后重新获取最新场次。</p>
            <button class="ui-button ui-button--primary" type="button" @click="loadData">
              <el-icon><RefreshRight /></el-icon>
              重新加载
            </button>
          </div>
        </div>

        <div v-else-if="visibleHotShows.length" class="on-sale-grid">
          <article
            v-for="(show, index) in visibleHotShows"
            :key="show.id"
            class="show-card"
            :class="{ 'show-card--lead': index === 0 }"
          >
            <button class="show-card__hit-area" type="button" :aria-label="`查看 ${show.name}`" @click="goToShowDetail(show.id)"></button>
            <div class="show-card__image">
              <img :src="posterFor(show)" :alt="show.name" @error="handleImageError" />
            </div>
            <div class="show-card__body">
              <div class="show-card__meta">
                <span>{{ formatDate(show.startTime) }}</span>
                <span>{{ show.venue || '场馆待公布' }}</span>
              </div>
              <h3>{{ show.name }}</h3>
              <div class="show-card__footer">
                <strong>{{ priceLabel(show) }}</strong>
                <span :class="['show-status', `show-status--${show.status}`]">{{ show.statusText }}</span>
              </div>
            </div>
          </article>
        </div>

        <div v-else class="state-block home-state">
          <div>
            <h3>暂时没有在售演出</h3>
            <p>新的场次正在陆续上架。</p>
            <button class="ui-button" type="button" @click="loadData">刷新列表</button>
          </div>
        </div>
      </div>
    </section>

    <section class="seckill-band" aria-labelledby="seckill-title">
      <div class="container">
        <div class="section-heading section-heading--compact">
          <div>
            <p class="section-kicker">限时抢票</p>
            <h2 id="seckill-title">准点开抢的场次</h2>
          </div>
          <router-link class="text-action" to="/seckill">
            全部场次
            <el-icon><ArrowRight /></el-icon>
          </router-link>
        </div>

        <div v-if="loading" class="seckill-rail seckill-rail--skeleton" aria-label="正在加载秒杀场次">
          <div v-for="index in 3" :key="index" class="seckill-row-skeleton">
            <div class="skeleton"></div><div class="skeleton"></div><div class="skeleton"></div>
          </div>
        </div>
        <div v-else-if="seckillLoadError" class="seckill-empty" role="status">
          限时抢票数据暂时不可用，其他演出仍可正常浏览。
        </div>
        <div v-else-if="seckillSessions.length" class="seckill-rail">
          <button
            v-for="session in seckillSessions"
            :key="session.id"
            class="seckill-row"
            type="button"
            @click="goToSeckillDetail(session.id)"
          >
            <span class="seckill-row__time">
              <span class="seckill-row__state">{{ session.statusText }}</span>
              <span v-if="session.uiStatus === 'upcoming'" class="seckill-row__countdown">{{ countdownLabel(session.countdown) }}</span>
              <span v-else>{{ session.uiStatus === 'active' ? '现在可参与' : '本场已结束' }}</span>
            </span>
            <span class="seckill-row__show">
              <strong>{{ session.showName }}</strong>
              <span>{{ session.venue || '场馆待公布' }}<template v-if="session.ticketName"> · {{ session.ticketName }}</template></span>
            </span>
            <span class="seckill-row__price">
              <strong v-if="session.price !== null">¥{{ session.price }}</strong>
              <strong v-else>查看价格</strong>
              <span v-if="session.originPrice">¥{{ session.originPrice }}</span>
            </span>
            <el-icon class="seckill-row__arrow"><ArrowRight /></el-icon>
          </button>
        </div>
        <div v-else-if="!loading" class="seckill-empty">当前没有可参与的限时抢票场次。</div>
      </div>
    </section>

    <section class="content-section content-section--recommendations">
      <div class="container">
        <div class="section-heading">
          <div>
            <p class="section-kicker">继续发现</p>
            <h2>更多即将发生的现场</h2>
          </div>
        </div>

        <div v-if="loading" class="recommendation-list recommendation-list--skeleton" aria-label="正在加载推荐演出">
          <div v-for="index in 4" :key="index" class="recommendation-skeleton">
            <div class="skeleton recommendation-skeleton__poster"></div>
            <div><div class="skeleton recommendation-skeleton__line"></div><div class="skeleton recommendation-skeleton__line recommendation-skeleton__line--short"></div></div>
          </div>
        </div>
        <div v-else-if="!loadError && recommendedShows.length" class="recommendation-list">
          <article v-for="show in recommendedShows" :key="show.id" class="recommendation-item">
            <button class="recommendation-item__hit-area" type="button" :aria-label="`查看 ${show.name}`" @click="goToShowDetail(show.id)"></button>
            <img :src="posterFor(show)" :alt="show.name" @error="handleImageError" />
            <div class="recommendation-item__content">
              <p>{{ formatDate(show.startTime) }}<template v-if="show.venue"> · {{ show.venue }}</template></p>
              <h3>{{ show.name }}</h3>
              <span>{{ priceLabel(show) }}</span>
            </div>
            <el-icon><ArrowRight /></el-icon>
          </article>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  ArrowRight,
  Calendar,
  Film,
  Picture,
  RefreshRight,
  Tickets,
  Trophy
} from '@element-plus/icons-vue';
import { showApi, seckillApi, ticketApi } from '../../api';
import { formatDateTime, seckillCountdownParts } from '../../utils/format';
import { filterShowsByTime, getSeckillSessionState } from '../../utils/ticketing-state';

const DEFAULT_POSTER = 'https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?auto=format&fit=crop&w=1200&q=85';
const router = useRouter();
const hotShows = ref([]);
const recommendedShows = ref([]);
const seckillSessions = ref([]);
const loading = ref(true);
const loadError = ref(false);
const seckillLoadError = ref(false);
const activeTime = ref('week');
const posterFallbacks = new WeakSet();
const priceRequests = new Map();
let seckillTimer = null;

const timeOptions = [
  { id: 'today', label: '今天' },
  { id: 'week', label: '本周' },
  { id: 'weekend', label: '周末' }
];

const categories = [
  { id: 'concert', name: '演唱会', icon: Tickets },
  { id: 'drama', name: '话剧', icon: Film },
  { id: 'sport', name: '体育', icon: Trophy },
  { id: 'exhibition', name: '展览', icon: Picture },
  { id: 'family', name: '儿童亲子', icon: Calendar }
];

const featuredShow = computed(() => hotShows.value[0] || recommendedShows.value[0] || null);
const featuredStageStyle = computed(() => ({
  backgroundImage: `url("${posterFor(featuredShow.value)}")`
}));
const visibleHotShows = computed(() => filterShowsByTime(hotShows.value, activeTime.value).slice(0, 6));

function posterFor(show) {
  return show?.poster || DEFAULT_POSTER;
}

function handleImageError(event) {
  const image = event.currentTarget;
  if (!posterFallbacks.has(image)) {
    posterFallbacks.add(image);
    image.src = DEFAULT_POSTER;
  }
}

function showStatusText(status) {
  return { 0: '已下架', 1: '售票中', 2: '进行中' }[status] || '场次待定';
}

function seckillState(session) {
  const key = getSeckillSessionState(session);
  return {
    key,
    label: {
      upcoming: '即将开始',
      active: '进行中',
      soldout: '已售罄',
      ended: '已结束',
      closed: '已关闭',
      unavailable: '状态异常'
    }[key]
  };
}

async function getMinPrice(showId) {
  if (!priceRequests.has(showId)) {
    priceRequests.set(showId, ticketApi.getByShowId(showId)
      .then((response) => {
        if (response.code !== 200 || !Array.isArray(response.data)) return null;
        const prices = response.data.map((ticket) => Number(ticket.price)).filter(Number.isFinite);
        return prices.length ? Math.min(...prices) : null;
      })
      .catch(() => null));
  }
  return priceRequests.get(showId);
}

async function enrichShowCards(shows) {
  return Promise.all((Array.isArray(shows) ? shows : []).map(async (show) => ({
    ...show,
    minPrice: await getMinPrice(show.id),
    statusText: showStatusText(show.status)
  })));
}

function mapSeckillRows(sessions) {
  return sessions.map((session) => {
    const state = seckillState(session);
    const ticket = session.ticket || {};
    const show = session.show || {};
    return {
      ...session,
      uiStatus: state.key,
      statusText: state.label,
      showName: show.name || '演出场次',
      venue: show.venue || '',
      price: session.seckillPrice == null ? null : Number(session.seckillPrice),
      originPrice: ticket.price == null ? null : Number(ticket.price),
      ticketName: ticket.name || '',
      countdown: seckillCountdownParts(session.startTime)
    };
  });
}

async function loadData() {
  loading.value = true;
  loadError.value = false;
  seckillLoadError.value = false;
  try {
    const [hotResult, recommendResult, activeResult, upcomingResult] = await Promise.allSettled([
      showApi.getHotShows(8),
      showApi.getShowList({ page: 1, size: 8 }),
      seckillApi.getActiveSessions(),
      seckillApi.getUpcomingSessions()
    ]);

    const responseOf = (result, label) => {
      if (result.status === 'rejected') {
        console.error(`${label}加载失败`, result.reason);
        return null;
      }
      if (result.value?.code !== 200) {
        console.error(`${label}加载失败`, result.value?.message || '接口返回异常');
        return null;
      }
      return result.value;
    };

    const hotResponse = responseOf(hotResult, '热门演出');
    const recommendResponse = responseOf(recommendResult, '推荐演出');
    const activeResponse = responseOf(activeResult, '进行中秒杀');
    const upcomingResponse = responseOf(upcomingResult, '即将开始秒杀');
    const hotList = hotResponse?.data || [];
    const recommendList = recommendResponse?.data?.records || [];
    const fallbackHotList = hotList.length ? hotList : recommendList;
    const fallbackRecommendList = recommendList.length ? recommendList : hotList;

    hotShows.value = await enrichShowCards(fallbackHotList);
    recommendedShows.value = await enrichShowCards(fallbackRecommendList);
    loadError.value = !hotResponse && !recommendResponse;

    const active = activeResponse?.data || [];
    const upcoming = upcomingResponse?.data || [];
    seckillSessions.value = mapSeckillRows([...active, ...upcoming].slice(0, 4));
    seckillLoadError.value = !activeResponse && !upcomingResponse;
  } catch (error) {
    console.error('首页数据加载失败', error);
    loadError.value = true;
    seckillLoadError.value = true;
  } finally {
    loading.value = false;
  }
}

function refreshSeckillCountdown() {
  seckillSessions.value = mapSeckillRows(seckillSessions.value);
}

function formatDate(value) {
  return formatDateTime(value, 'YYYY年MM月DD日') || '日期待公布';
}

function priceLabel(show) {
  return show.minPrice == null ? '票价见详情' : `¥${show.minPrice} 起`;
}

function countdownLabel(countdown) {
  return `${countdown.days}天 ${countdown.hours}:${countdown.minutes}:${countdown.seconds}`;
}

function goToCategory(category) {
  router.push(`/category/${encodeURIComponent(category)}`);
}

function goToShowDetail(id) {
  router.push(`/show/${id}`);
}

function goToSeckillDetail(id) {
  router.push(`/seckill/${id}`);
}

function openFeatured() {
  if (featuredShow.value?.id) {
    goToShowDetail(featuredShow.value.id);
    return;
  }
  goToCategory('演唱会');
}

onMounted(() => {
  loadData();
  seckillTimer = window.setInterval(refreshSeckillCountdown, 1000);
});

onUnmounted(() => {
  if (seckillTimer) window.clearInterval(seckillTimer);
});
</script>

<style scoped>
.home-page { overflow: hidden; padding-bottom: 72px; }

.featured-stage { position: relative; min-height: 520px; display: grid; align-items: end; background-color: var(--ink); background-position: center; background-size: cover; }
.featured-stage__shade { position: absolute; inset: 0; background: rgba(10, 12, 14, 0.54); }
.featured-stage__content { position: relative; z-index: 1; padding-bottom: 54px; color: #fff; }
.featured-stage__eyebrow, .section-kicker { margin-bottom: 10px; color: var(--brand); font-size: 12px; font-weight: 700; }
.featured-stage__eyebrow { color: #fff; }
.featured-stage h1 { max-width: 720px; color: #fff; font-size: clamp(38px, 5vw, 64px); font-weight: 760; line-height: 1.08; }
.featured-stage__facts { display: flex; flex-wrap: wrap; gap: 8px 20px; margin-top: 18px; font-size: 16px; color: rgba(255, 255, 255, 0.9); }
.featured-stage__facts span + span::before { content: ''; display: inline-block; width: 4px; height: 4px; margin: 0 20px 2px 0; border-radius: 50%; background: var(--brand); }
.stage-action, .text-action { display: inline-flex; align-items: center; gap: 8px; min-height: 40px; border: 0; background: transparent; font: inherit; font-weight: 700; cursor: pointer; }
.stage-action { margin-top: 30px; padding: 0 16px; border: 1px solid #fff; border-radius: var(--radius); color: #fff; }
.stage-action:hover { background: #fff; color: var(--ink); }
.stage-action:active, .text-action:active { transform: scale(0.98); }

.discovery-band { border-bottom: 1px solid var(--line); background: var(--surface); }
.discovery-band__inner { display: flex; align-items: center; justify-content: space-between; gap: 24px; min-height: 76px; }
.discovery-group { display: flex; align-items: center; gap: 4px; min-width: 0; }
.discovery-label { margin-right: 8px; color: var(--ink-3); font-size: 13px; }
.discovery-option { display: inline-flex; align-items: center; gap: 6px; min-height: 40px; padding: 0 10px; border: 0; border-bottom: 2px solid transparent; background: transparent; color: var(--ink-2); font: inherit; cursor: pointer; white-space: nowrap; }
.discovery-option:hover, .discovery-option.is-active { border-bottom-color: var(--brand); color: var(--ink); font-weight: 700; }

.content-section { padding-top: 66px; }
.section-heading { display: flex; align-items: end; justify-content: space-between; gap: 24px; margin-bottom: 26px; }
.section-heading h2 { font-size: clamp(26px, 3vw, 36px); font-weight: 760; }
.section-heading--compact { margin-bottom: 18px; }
.text-action { flex: 0 0 auto; color: var(--ink-2); }
.text-action:hover { color: var(--brand); }

.on-sale-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 20px; }
.show-card { position: relative; min-width: 0; overflow: hidden; border: 1px solid var(--line); border-radius: var(--radius); background: var(--surface); transition: border-color 160ms ease, transform 160ms ease, box-shadow 160ms ease; }
.show-card:hover { border-color: var(--ink-3); box-shadow: var(--shadow-float); transform: translateY(-2px); }
.show-card__hit-area, .recommendation-item__hit-area { position: absolute; z-index: 2; inset: 0; border: 0; background: transparent; cursor: pointer; }
.show-card__image { aspect-ratio: 3 / 4; overflow: hidden; background: var(--surface-3); }
.show-card__image img { width: 100%; height: 100%; object-fit: cover; transition: transform 220ms ease; }
.show-card:hover .show-card__image img { transform: scale(1.03); }
.show-card--lead { grid-row: span 2; grid-column: span 2; }
.show-card--lead .show-card__image { aspect-ratio: 16 / 11; }
.show-card__body { padding: 14px; }
.show-card__meta { display: flex; flex-wrap: wrap; gap: 4px 12px; margin-bottom: 7px; color: var(--ink-3); font-size: 12px; }
.show-card__meta span + span::before { content: ''; display: inline-block; width: 3px; height: 3px; margin: 0 12px 2px 0; border-radius: 50%; background: var(--line-strong); }
.show-card h3 { display: -webkit-box; min-height: 42px; overflow: hidden; font-size: 16px; font-weight: 700; line-height: 1.35; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.show-card__footer { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-top: 14px; }
.show-card__footer strong { color: var(--brand); font-size: 16px; }
.show-status { color: var(--ink-3); font-size: 12px; white-space: nowrap; }
.show-status--1, .show-status--2 { color: var(--success); }
.show-card-skeleton { overflow: hidden; border: 1px solid var(--line); border-radius: var(--radius); background: var(--surface); }
.show-card-skeleton:first-child { grid-row: span 2; grid-column: span 2; }
.show-card-skeleton__image { aspect-ratio: 3 / 4; }
.show-card-skeleton:first-child .show-card-skeleton__image { aspect-ratio: 16 / 11; }
.show-card-skeleton__line { height: 16px; margin: 14px 14px 0; border-radius: 2px; }
.show-card-skeleton__line--short { width: 56%; margin-bottom: 14px; }
.home-state h3 { margin-bottom: 8px; font-size: 20px; }
.home-state p { margin-bottom: 18px; }

.seckill-band { margin-top: 66px; padding: 44px 0; border-block: 1px solid var(--line); background: var(--surface); }
.seckill-rail { border-top: 1px solid var(--line); }
.seckill-row { width: 100%; display: grid; grid-template-columns: 184px minmax(0, 1fr) 126px 20px; align-items: center; gap: 24px; padding: 18px 0; border: 0; border-bottom: 1px solid var(--line); background: transparent; color: var(--ink); text-align: left; cursor: pointer; }
.seckill-row:hover { color: var(--brand); }
.seckill-row__time, .seckill-row__show, .seckill-row__price { display: grid; gap: 3px; min-width: 0; }
.seckill-row__state { color: var(--brand); font-size: 13px; font-weight: 700; }
.seckill-row__countdown, .seckill-row__time > span:last-child, .seckill-row__show > span { color: var(--ink-3); font-size: 13px; }
.seckill-row__show strong { overflow: hidden; font-size: 16px; text-overflow: ellipsis; white-space: nowrap; }
.seckill-row__price { text-align: right; }
.seckill-row__price strong { color: var(--brand); font-size: 19px; }
.seckill-row__price span { color: var(--ink-3); font-size: 13px; text-decoration: line-through; }
.seckill-row__arrow { justify-self: end; }
.seckill-empty { padding: 26px 0; color: var(--ink-3); border-top: 1px solid var(--line); }
.seckill-rail--skeleton { display: grid; gap: 0; }
.seckill-row-skeleton { display: grid; grid-template-columns: 184px minmax(0, 1fr) 126px; gap: 24px; padding: 18px 0; border-bottom: 1px solid var(--line); }
.seckill-row-skeleton > div { height: 38px; border-radius: 2px; }

.content-section--recommendations { padding-bottom: 0; }
.recommendation-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1px; background: var(--line); border: 1px solid var(--line); border-radius: var(--radius); overflow: hidden; }
.recommendation-item { position: relative; z-index: 0; display: grid; grid-template-columns: 76px minmax(0, 1fr) 20px; align-items: center; gap: 14px; min-height: 108px; padding: 14px; background: var(--surface); transition: background 160ms ease; }
.recommendation-item:hover { background: #fafafa; }
.recommendation-item img, .recommendation-skeleton__poster { width: 76px; height: 80px; border-radius: 4px; object-fit: cover; background: var(--surface-3); }
.recommendation-item__content { display: grid; min-width: 0; gap: 5px; }
.recommendation-item__content p, .recommendation-item__content span { overflow: hidden; color: var(--ink-3); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.recommendation-item__content h3 { overflow: hidden; font-size: 16px; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.recommendation-item__content span { color: var(--brand); font-weight: 700; }
.recommendation-list--skeleton { gap: 1px; }
.recommendation-skeleton { display: grid; grid-template-columns: 76px minmax(0, 1fr); gap: 14px; min-height: 108px; padding: 14px; background: var(--surface); }
.recommendation-skeleton > div:last-child { display: grid; align-content: center; gap: 12px; }
.recommendation-skeleton__line { height: 16px; border-radius: 2px; }
.recommendation-skeleton__line--short { width: 58%; }

@media (max-width: 1000px) {
  .discovery-band__inner { align-items: flex-start; flex-direction: column; gap: 0; padding-block: 10px; }
  .discovery-group { width: 100%; overflow-x: auto; scrollbar-width: none; }
  .discovery-group::-webkit-scrollbar { display: none; }
  .on-sale-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .show-card--lead { grid-column: span 2; }
  .seckill-row { grid-template-columns: 158px minmax(0, 1fr) 110px 20px; gap: 16px; }
  .seckill-row-skeleton { grid-template-columns: 158px minmax(0, 1fr) 110px; gap: 16px; }
}

@media (max-width: 768px) {
  .home-page { padding-bottom: 48px; }
  .featured-stage { min-height: 440px; }
  .featured-stage__content { padding-bottom: 38px; }
  .featured-stage h1 { font-size: 38px; }
  .featured-stage__facts { gap: 5px 12px; font-size: 14px; }
  .featured-stage__facts span + span::before { margin-right: 12px; }
  .discovery-option { padding-inline: 9px; }
  .content-section { padding-top: 44px; }
  .section-heading { align-items: end; margin-bottom: 20px; }
  .section-heading h2 { font-size: 28px; }
  .on-sale-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
  .show-card--lead { grid-row: auto; grid-column: span 2; }
  .show-card--lead .show-card__image { aspect-ratio: 16 / 10; }
  .show-card__body { padding: 12px; }
  .show-card h3 { font-size: 15px; }
  .seckill-band { margin-top: 44px; padding: 32px 0; }
  .seckill-row { grid-template-columns: minmax(0, 1fr) auto; gap: 10px 14px; padding: 16px 0; }
  .seckill-row__time { grid-column: 1 / -1; grid-auto-flow: column; justify-content: start; gap: 12px; }
  .seckill-row__show { grid-column: 1; }
  .seckill-row__price { grid-column: 2; grid-row: 2; }
  .seckill-row__arrow { display: none; }
  .seckill-row-skeleton { grid-template-columns: 1fr auto; gap: 12px; }
  .seckill-row-skeleton > div:first-child { grid-column: 1 / -1; }
  .recommendation-list { grid-template-columns: 1fr; }
}
</style>
