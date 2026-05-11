<template>
  <router-view v-if="isAuthLayout" />

  <div v-else class="app-container">
    <Sidebar />
    <main class="main-content">
      <Header />
      <section class="content-view">
        <router-view />
      </section>
      <DetailPanel />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import Sidebar from '@/components/layout/Sidebar.vue'
import Header from '@/components/layout/Header.vue'
import DetailPanel from '@/components/gallery/DetailPanel.vue'
import { useAppStore } from '@/stores/app'

const appStore = useAppStore()
const route = useRoute()
const isAuthLayout = computed(() => route.meta.layout === 'auth')

onMounted(() => {
  document.documentElement.setAttribute('data-theme', appStore.theme)
})
</script>

<style>
.app-container {
  display: flex;
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  background-color: var(--bg);
  overflow: hidden; /* Ensure DetailPanel is clipped when closed */
}

.content-view {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem 2.5rem 2.5rem;
}
</style>
