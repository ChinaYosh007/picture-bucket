<template>
  <div id="basicLayout">
    <a-layout class="app-layout">
      <a-layout-header class="header">
        <GlobalHeader />
      </a-layout-header>
      <a-layout class="body-layout">
        <GlobalSider class="sider" />
        <a-layout-content class="content">
          <div class="content-inner">
            <router-view v-slot="{ Component }">
              <transition name="page-fade" mode="out-in">
                <component :is="Component" />
              </transition>
            </router-view>
          </div>
        </a-layout-content>
      </a-layout>
      <a-layout-footer class="footer">
        <span class="footer-brand">Picture Bucket</span>
        <span class="footer-sep">·</span>
        <span class="footer-copy">让每一张图片都有舒服的位置</span>
      </a-layout-footer>
    </a-layout>
  </div>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalSider from '@/components/GlobalSider.vue'
</script>

<style scoped>
.app-layout,
.body-layout {
  min-height: 100vh;
  background: transparent;
}

.header {
  position: sticky;
  top: 0;
  z-index: 100;
  height: 68px;
  padding-inline: 28px;
  line-height: 68px;
  background: rgba(255, 255, 255, 0.68);
  border-bottom: 1px solid rgba(232, 234, 243, 0.85);
  backdrop-filter: blur(20px) saturate(150%);
}

.sider {
  background: transparent;
}

#basicLayout :deep(.ant-menu-root) {
  border-bottom: none !important;
  border-inline-end: none !important;
  background: transparent;
}

.content {
  min-height: calc(100vh - 68px - 56px);
  padding: 32px 32px 56px;
  background: transparent;
}

.content-inner {
  max-width: 1440px;
  margin: 0 auto;
}

/* 页面切换 —— 轻盈的淡入上浮 */
.page-fade-enter-active {
  transition: opacity 0.3s ease, transform 0.3s cubic-bezier(0.2, 0.7, 0.2, 1);
}

.page-fade-leave-active {
  transition: opacity 0.18s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(14px);
}

.page-fade-leave-to {
  opacity: 0;
}

.footer {
  padding: 16px 24px 20px;
  color: var(--pb-faint);
  font-size: 12px;
  text-align: center;
  background: transparent;
}

.footer-brand {
  font-weight: 800;
  letter-spacing: 0.02em;
  background: var(--pb-grad);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.footer-sep {
  margin: 0 9px;
  color: #d4d8e8;
}

@media (max-width: 768px) {
  .header {
    height: 60px;
    padding-inline: 14px;
    line-height: 60px;
  }

  .content {
    padding: 20px 14px 38px;
  }
}
</style>
