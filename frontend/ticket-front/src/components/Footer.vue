<template>
  <footer class="app-footer">
    <div class="footer-container">
      <div class="footer-content">
        <div class="footer-section">
          <h3 class="footer-title">关于我们</h3>
          <ul class="footer-links">
            <li><a href="#">公司简介</a></li>
            <li><a href="#">加入我们</a></li>
            <li><a href="#">联系方式</a></li>
            <li><a href="#">隐私政策</a></li>
          </ul>
        </div>
        <div class="footer-section">
          <h3 class="footer-title">服务支持</h3>
          <ul class="footer-links">
            <li><a href="#">帮助中心</a></li>
            <li><a href="#">购票须知</a></li>
            <li><a href="#">退款政策</a></li>
            <li><a href="#">常见问题</a></li>
          </ul>
        </div>
        <div class="footer-section">
          <h3 class="footer-title">关注我们</h3>
          <ul class="footer-links">
            <li><a href="#">官方微博</a></li>
            <li><a href="#">官方微信</a></li>
            <li><a href="#">官方APP</a></li>
            <li><a href="#">意见反馈</a></li>
          </ul>
        </div>
        <div class="footer-section">
          <h3 class="footer-title">联系方式</h3>
          <ul class="footer-links">
            <li>客服电话：13956828147</li>
            <li>工作时间：9:00-22:00</li>
            <li>邮箱：2399276050@qq.com</li>
            <li>地址：吉首大学~巴拉巴拉</li>
          </ul>
        </div>
      </div>
      <div class="footer-bottom">
        <p class="copyright">© 2026 Ticket+ 票务平台. All rights reserved.</p>
        <p class="icp">京ICP备12345678号</p>
      </div>
    </div>
  </footer>
</template>

<script setup>
// Footer组件
</script>

<style scoped>
.app-footer {
  background: #1F1F1F;
  color: #fff;
  padding: 40px 0 20px;

  .footer-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 24px;
  }

  .footer-content {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 40px;
    margin-bottom: 40px;
  }

  .footer-section {
    .footer-title {
      font-size: 16px;
      font-weight: 600;
      margin-bottom: 20px;
      color: #fff;
    }

    .footer-links {
      list-style: none;

      li {
        margin-bottom: 12px;

        a {
          color: #BFBFBF;
          text-decoration: none;
          font-size: 14px;
          transition: color 0.3s;

          &:hover {
            color: var(--primary-color);
          }
        }

        &:not(:has(a)) {
          color: #BFBFBF;
          font-size: 14px;
        }
      }
    }
  }

  .footer-bottom {
    border-top: 1px solid #333;
    padding-top: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .copyright,
    .icp {
      font-size: 12px;
      color: #8C8C8C;
    }
  }
}

@media (max-width: 768px) {
  .app-footer {
    .footer-content {
      grid-template-columns: repeat(2, 1fr);
      gap: 24px;
    }

    .footer-bottom {
      flex-direction: column;
      gap: 12px;
      text-align: center;
    }
  }
}
</style>