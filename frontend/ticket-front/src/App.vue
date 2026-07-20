<template>
  <div class="app-shell">
    <Header />
    <main class="app-main">
      <router-view v-slot="{ Component }">
        <transition name="route-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
    <Footer />
  </div>
</template>

<script setup>
import Header from './components/Header.vue';
import Footer from './components/Footer.vue';
</script>

<style>
@import './assets/styles/main.css';
.app-shell { min-height: 100dvh; display: flex; flex-direction: column; }
.app-main { flex: 1; min-width: 0; }
.route-fade-enter-active, .route-fade-leave-active { transition: opacity 140ms ease, transform 140ms ease; }
.route-fade-enter-from { opacity: 0; transform: translateY(4px); }
.route-fade-leave-to { opacity: 0; }
</style>
