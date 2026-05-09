<template>
  <div class="home-page">
    <!-- 轮播图 -->
    <div class="banner">
      <el-carousel :interval="5000" type="card" height="400px">
        <el-carousel-item v-for="(item, index) in banners" :key="index">
          <div class="banner-item">
            <img :src="item.image" :alt="item.title" class="banner-image" />
            <div class="banner-content">
              <h2 class="banner-title">{{ item.title }}</h2>
              <p class="banner-desc">{{ item.description }}</p>
              <router-link :to="item.link" class="banner-btn">立即查看</router-link>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>

    <!-- 分类导航 -->
    <div class="category-nav">
      <div class="container">
        <div class="category-list">
          <div 
            v-for="category in categories" 
            :key="category.id"
            class="category-item"
            @click="goToCategory(category.name)"
          >
            <div class="category-icon">{{ category.icon }}</div>
            <span class="category-name">{{ category.name }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 热门演出 -->
    <div class="hot-shows">
      <div class="container">
        <div class="section-header">
          <h2 class="section-title">热门演出</h2>
          <router-link :to="{ name: 'Category', params: { category: '演唱会' } }" class="more-link">查看更多 ></router-link>
        </div>
        <div class="show-grid">
          <div 
            v-for="show in hotShows" 
            :key="show.id"
            class="show-card"
            @click="goToShowDetail(show.id)"
          >
            <div class="show-image">
              <img :src="show.poster" :alt="show.name" />
              <div class="show-tag" v-if="show.tag">{{ show.tag }}</div>
            </div>
            <div class="show-info">
              <h3 class="show-name">{{ show.name }}</h3>
              <p class="show-venue">{{ show.venue }}</p>
              <p class="show-time">{{ formatTime(show.startTime) }}</p>
              <div class="show-footer">
                <span class="show-price">{{ show.minPrice != null ? `¥${show.minPrice}起` : '票价见详情' }}</span>
                <span class="show-status">{{ show.statusText }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 限时抢购 -->
    <div class="seckill-section">
      <div class="container">
        <div class="section-header">
          <h2 class="section-title">
            <span class="seckill-icon">🔥</span>
            限时抢购
          </h2>
          <router-link to="/seckill" class="more-link">全部场次 ></router-link>
        </div>
        <div class="seckill-list">
          <div 
            v-for="session in seckillSessions" 
            :key="session.id"
            class="seckill-item"
            @click="goToSeckillDetail(session.id)"
          >
            <div class="seckill-countdown">
              <div class="countdown-label">{{ session.statusText }}</div>
              <div v-if="session.uiStatus === 'upcoming'" class="countdown-time">
                <span class="countdown-item">{{ session.countdown.days }}</span>
                <span class="countdown-separator">:</span>
                <span class="countdown-item">{{ session.countdown.hours }}</span>
                <span class="countdown-separator">:</span>
                <span class="countdown-item">{{ session.countdown.minutes }}</span>
                <span class="countdown-separator">:</span>
                <span class="countdown-item">{{ session.countdown.seconds }}</span>
              </div>
              <div v-else-if="session.uiStatus === 'active'" class="countdown-time">
                <span class="countdown-item active">进行中</span>
              </div>
              <div v-else class="countdown-time">
                <span class="countdown-item ended">已结束</span>
              </div>
            </div>
            <div class="seckill-info">
              <h3 class="seckill-name">{{ session.showName }}</h3>
              <p class="seckill-venue">{{ session.venue }}</p>
              <div class="seckill-price" v-if="session.price != null">
                <span class="ticket-type" v-if="session.ticketName">{{ session.ticketName }}</span>
                <span class="price">¥{{ session.price }}</span>
                <span v-if="session.originPrice" class="origin-price">¥{{ session.originPrice }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 推荐演出 -->
    <div class="recommended-shows">
      <div class="container">
        <div class="section-header">
          <h2 class="section-title">为您推荐</h2>
        </div>
        <div class="show-grid">
          <div 
            v-for="show in recommendedShows" 
            :key="show.id"
            class="show-card"
            @click="goToShowDetail(show.id)"
          >
            <div class="show-image">
              <img :src="show.poster" :alt="show.name" />
            </div>
            <div class="show-info">
              <h3 class="show-name">{{ show.name }}</h3>
              <p class="show-venue">{{ show.venue }}</p>
              <p class="show-time">{{ formatTime(show.startTime) }}</p>
              <div class="show-footer">
                <span class="show-price">{{ show.minPrice != null ? `¥${show.minPrice}起` : '票价见详情' }}</span>
                <span class="show-status">{{ show.statusText }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { showApi, seckillApi, ticketApi } from '../../api';
import { formatDateTime, showStatusText, seckillUiState, seckillCountdownParts } from '../../utils/format';

const router = useRouter();
const hotShows = ref([]);
const recommendedShows = ref([]);
const seckillSessions = ref([]);
let seckillTimer = null;

// 轮播图：与数据库中文分类示例风格一致（图片可对应 doc/sql 中 poster 主题）
const banners = ref([
  {
    image: 'https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?w=1920&h=400&auto=format&fit=crop',
    title: '演唱会 · 星河巡回',
    description: '大型巡演、舞美与音响全升级，热门场次一键直达分类。',
    link: '/category/演唱会'
  },
  {
    image: 'https://images.unsplash.com/photo-1503095396549-807759245b35?w=1920&h=400&auto=format&fit=crop',
    title: '话剧 · 经典驻演',
    description: '名团驻场、口碑剧目，城市文化客厅等你入座。',
    link: '/category/话剧'
  },
  {
    image: 'https://images.unsplash.com/photo-1546519638-68e109498ffc?w=1920&h=400&auto=format&fit=crop',
    title: '体育 · 热血现场',
    description: '联赛、全明星与杯赛，现场氛围拉满。',
    link: '/category/体育'
  },
  {
    image: 'https://images.unsplash.com/photo-1549887534-1541e9326642?w=1920&h=400&auto=format&fit=crop',
    title: '展览 · 艺术光影',
    description: '沉浸式策展与互动体验，适合打卡与亲子共览。',
    link: '/category/展览'
  },
  {
    image: 'https://images.unsplash.com/photo-1513885535751-8b9238bd345a?w=1920&h=400&auto=format&fit=crop',
    title: '儿童亲子 · 欢乐剧场',
    description: '互动舞台剧与家庭套票，安全有趣的亲子时光。',
    link: '/category/儿童亲子'
  }
]);

// 分类入口（与 biz_show.category 中文取值一致，便于 /api/show/category/{name} 查询）
const categories = ref([
  { id: 1, name: '演唱会', icon: '🎤' },
  { id: 2, name: '话剧', icon: '🎭' },
  { id: 3, name: '体育', icon: '⚽' },
  { id: 4, name: '展览', icon: '🖼️' },
  { id: 5, name: '儿童亲子', icon: '👶' }
]);

async function enrichShowCards(shows) {
  const list = Array.isArray(shows) ? shows : [];
  return Promise.all(
    list.map(async (s) => {
      let minPrice = null;
      try {
        const tr = await ticketApi.getByShowId(s.id);
        if (tr.code === 200 && Array.isArray(tr.data) && tr.data.length) {
          const prices = tr.data.map((t) => Number(t.price)).filter((n) => !Number.isNaN(n));
          if (prices.length) minPrice = Math.min(...prices);
        }
      } catch {
        /* 忽略单场票价加载失败 */
      }
      return {
        ...s,
        minPrice,
        statusText: showStatusText(s.status)
      };
    })
  );
}

function mapSeckillRows(sessions) {
  return sessions.map((session) => {
    const ui = seckillUiState(session);
    const ticket = session.ticket;
    const show = session.show;
    const price = session.seckillPrice != null ? Number(session.seckillPrice) : null;
    const originPrice = ticket?.price != null ? Number(ticket.price) : null;
    const ticketName = ticket?.name ?? '';
    return {
      ...session,
      uiStatus: ui.key,
      statusText: ui.label,
      showName: show?.name ?? '演出',
      venue: show?.venue ?? '',
      price,
      originPrice,
      ticketName,
      countdown: seckillCountdownParts(session.startTime)
    };
  });
}

const loadData = async () => {
  try {
    const hotResponse = await showApi.getHotShows(8);
    const hotList = hotResponse.code === 200 && hotResponse.data ? hotResponse.data : [];
    hotShows.value = await enrichShowCards(hotList);

    const recommendResponse = await showApi.getShowList({ page: 1, size: 8 });
    const recList =
      recommendResponse.code === 200 && recommendResponse.data?.records
        ? recommendResponse.data.records
        : [];
    recommendedShows.value = await enrichShowCards(recList);

    const [activeRes, upcomingRes] = await Promise.all([
      seckillApi.getActiveSessions(),
      seckillApi.getUpcomingSessions()
    ]);
    const active = activeRes.code === 200 && activeRes.data ? activeRes.data : [];
    const upcoming = upcomingRes.code === 200 && upcomingRes.data ? upcomingRes.data : [];
    const merged = [...active, ...upcoming].slice(0, 4);
    seckillSessions.value = mapSeckillRows(merged);
  } catch (error) {
    console.error('加载数据失败:', error);
  }
};

const refreshSeckillCountdown = () => {
  seckillSessions.value = seckillSessions.value.map((session) => ({
    ...session,
    countdown: seckillCountdownParts(session.startTime)
  }));
};

onMounted(() => {
  loadData();
  seckillTimer = setInterval(refreshSeckillCountdown, 1000);
});

onUnmounted(() => {
  if (seckillTimer) clearInterval(seckillTimer);
});

const goToCategory = (category) => {
  router.push(`/category/${encodeURIComponent(category)}`);
};

const goToShowDetail = (id) => {
  router.push(`/show/${id}`);
};

const goToSeckillDetail = (id) => {
  router.push(`/seckill/${id}`);
};

const formatTime = (time) => formatDateTime(time, 'YYYY-MM-DD');
</script>

<style scoped>
.home-page {
  .banner {
    margin-bottom: 40px;

    .banner-item {
      position: relative;
      height: 400px;

      .banner-image {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      .banner-content {
        position: absolute;
        left: 10%;
        top: 50%;
        transform: translateY(-50%);
        color: #fff;
        max-width: 400px;

        .banner-title {
          font-size: 36px;
          font-weight: 700;
          margin-bottom: 16px;
          text-shadow: 0 2px 4px rgba(0, 0, 0, 0.5);
        }

        .banner-desc {
          font-size: 18px;
          margin-bottom: 24px;
          text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
        }

        .banner-btn {
          display: inline-block;
          padding: 12px 24px;
          background: var(--primary-color);
          color: #fff;
          border-radius: var(--border-radius-md);
          text-decoration: none;
          font-size: 16px;
          transition: all 0.3s;

          &:hover {
            background: var(--primary-dark);
            transform: translateY(-2px);
          }
        }
      }
    }
  }

  .category-nav {
    background: #fff;
    padding: 32px 0;
    margin-bottom: 40px;

    .category-list {
      display: flex;
      justify-content: space-around;
      flex-wrap: wrap;

      .category-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        cursor: pointer;
        padding: 16px;
        transition: transform 0.3s;

        &:hover {
          transform: translateY(-4px);
        }

        .category-icon {
          font-size: 32px;
          margin-bottom: 8px;
        }

        .category-name {
          font-size: 14px;
          color: var(--text-regular);
        }
      }
    }
  }

  .hot-shows,
  .recommended-shows {
    margin-bottom: 40px;

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;

      .section-title {
        font-size: 24px;
        font-weight: 600;
        color: var(--text-primary);
      }

      .more-link {
        color: var(--text-secondary);
        text-decoration: none;
        font-size: 14px;

        &:hover {
          color: var(--primary-color);
        }
      }
    }

    .show-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 24px;

      .show-card {
        background: #fff;
        border-radius: var(--border-radius-md);
        overflow: hidden;
        box-shadow: var(--shadow-light);
        transition: all 0.3s;
        cursor: pointer;

        &:hover {
          transform: translateY(-4px);
          box-shadow: var(--shadow-medium);
        }

        .show-image {
          position: relative;
          height: 200px;

          img {
            width: 100%;
            height: 100%;
            object-fit: cover;
          }

          .show-tag {
            position: absolute;
            top: 12px;
            left: 0;
            padding: 4px 12px;
            background: var(--primary-color);
            color: #fff;
            font-size: 12px;
            border-radius: 0 var(--border-radius-md) var(--border-radius-md) 0;
          }
        }

        .show-info {
          padding: 16px;

          .show-name {
            font-size: 16px;
            font-weight: 600;
            color: var(--text-primary);
            margin-bottom: 8px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .show-venue {
            font-size: 14px;
            color: var(--text-secondary);
            margin-bottom: 4px;
          }

          .show-time {
            font-size: 14px;
            color: var(--text-secondary);
            margin-bottom: 12px;
          }

          .show-footer {
            display: flex;
            justify-content: space-between;
            align-items: center;

            .show-price {
              font-size: 18px;
              font-weight: 600;
              color: var(--primary-color);
            }

            .show-status {
              font-size: 12px;
              padding: 2px 8px;
              border-radius: var(--border-radius-sm);
              background: var(--bg-hover);
              color: var(--primary-color);
            }
          }
        }
      }
    }
  }

  .seckill-section {
    background: linear-gradient(135deg, #FF4D4F, #FF7875);
    padding: 40px 0;
    margin-bottom: 40px;

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;

      .section-title {
        font-size: 24px;
        font-weight: 600;
        color: #fff;

        .seckill-icon {
          margin-right: 8px;
        }
      }

      .more-link {
        color: #fff;
        text-decoration: none;
        font-size: 14px;

        &:hover {
          text-decoration: underline;
        }
      }
    }

    .seckill-list {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 24px;

      .seckill-item {
        background: #fff;
        border-radius: var(--border-radius-md);
        overflow: hidden;
        box-shadow: var(--shadow-medium);
        transition: all 0.3s;
        cursor: pointer;

        &:hover {
          transform: translateY(-4px);
          box-shadow: var(--shadow-heavy);
        }

        .seckill-countdown {
          background: var(--primary-color);
          color: #fff;
          padding: 16px;
          text-align: center;

          .countdown-label {
            font-size: 14px;
            margin-bottom: 8px;
          }

          .countdown-time {
            display: flex;
            justify-content: center;
            align-items: center;
            gap: 8px;

            .countdown-item {
              background: rgba(255, 255, 255, 0.2);
              padding: 4px 8px;
              border-radius: var(--border-radius-sm);
              font-size: 18px;
              font-weight: 600;

              &.active {
                background: #fff;
                color: var(--primary-color);
              }

              &.ended {
                background: #999;
                color: #fff;
              }
            }

            .countdown-separator {
              font-size: 18px;
              font-weight: 600;
            }
          }
        }

        .seckill-info {
          padding: 16px;

          .seckill-name {
            font-size: 16px;
            font-weight: 600;
            color: var(--text-primary);
            margin-bottom: 8px;
          }

          .seckill-venue {
            font-size: 14px;
            color: var(--text-secondary);
            margin-bottom: 12px;
          }

          .seckill-price {
            display: flex;
            align-items: center;
            gap: 8px;

            .ticket-type {
              font-size: 12px;
              color: #d46b08;
              background: #fff7e6;
              border: 1px solid #ffd591;
              padding: 1px 7px;
              border-radius: 4px;
              font-weight: 500;
              line-height: 1.6;
            }

            .price {
              font-size: 20px;
              font-weight: 600;
              color: var(--primary-color);
            }

            .origin-price {
              font-size: 14px;
              color: var(--text-secondary);
              text-decoration: line-through;
            }
          }
        }
      }
    }
  }
}

@media (max-width: 768px) {
  .home-page {
    .banner {
      .banner-item {
        .banner-content {
          left: 5%;
          max-width: 80%;

          .banner-title {
            font-size: 24px;
          }

          .banner-desc {
            font-size: 14px;
          }
        }
      }
    }

    .category-nav {
      .category-list {
        grid-template-columns: repeat(3, 1fr);
        gap: 16px;
      }
    }

    .show-grid {
      grid-template-columns: repeat(2, 1fr);
      gap: 16px;
    }

    .seckill-list {
      grid-template-columns: 1fr;
      gap: 16px;
    }
  }
}
</style>