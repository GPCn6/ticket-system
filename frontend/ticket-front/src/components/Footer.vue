<template>
  <footer class="site-footer">
    <div class="footer-inner">
      <div class="footer-brand"><strong>Ticket+</strong><p>发现现场，抵达此刻。</p></div>
      <nav aria-label="页脚导航"><router-link to="/">浏览演出</router-link><router-link to="/seckill">限时抢票</router-link><router-link to="/order">我的订单</router-link><router-link to="/user">账户中心</router-link></nav>
      <div class="footer-meta"><span>客服 09:00 - 22:00</span><span>© 2026 Ticket+</span></div>
    </div>
  </footer>
</template>

<style scoped>
.site-footer { margin-top: 72px; border-top: 1px solid #30343a; color: #d5d8dc; background: #17191c; }
.footer-inner { width: min(100% - 48px, var(--container)); min-height: 168px; margin: auto; display: grid; grid-template-columns: 1.2fr 1fr auto; align-items: center; gap: 48px; }
.footer-brand strong { color: #fff; font-size: 22px; }.footer-brand p { margin-top: 6px; color: #8e949c; }
.footer-inner nav { display: grid; grid-template-columns: repeat(2, max-content); gap: 10px 28px; }.footer-inner nav a:hover { color: #fff; }
.footer-meta { display: flex; flex-direction: column; align-items: end; gap: 8px; color: #8e949c; }
@media (max-width: 700px) { .site-footer { margin-top: 48px; }.footer-inner { width: calc(100% - 32px); padding: 32px 0; grid-template-columns: 1fr; gap: 24px; }.footer-meta { align-items: start; } }
</style>
