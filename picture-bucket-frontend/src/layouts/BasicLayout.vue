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
            <router-view />
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
  height: 72px;
  padding-inline: 28px;
  line-height: 72px;
  background: rgba(255, 255, 255, 0.76);
  border-bottom: 1px solid rgba(229, 233, 242, 0.88);
  backdrop-filter: blur(18px);
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
  min-height: calc(100vh - 72px - 54px);
  padding: 30px 32px 54px;
  background: transparent;
}

.content-inner {
  max-width: 1440px;
  margin: 0 auto;
}

.footer {
  padding: 16px 24px 20px;
  color: #9aa6ba;
  font-size: 12px;
  text-align: center;
  background: transparent;
}

.footer-brand {
  color: #6d74cf;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.footer-sep {
  margin: 0 9px;
  color: #c1cad9;
}

@media (max-width: 768px) {
  .header {
    height: 64px;
    padding-inline: 14px;
    line-height: 64px;
  }

  .content {
    padding: 20px 14px 38px;
  }
}
</style>
