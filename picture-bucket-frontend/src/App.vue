<template>
  <div id="app">
    <a-config-provider :locale="zhCN" :theme="theme">
      <BasicLayout />
    </a-config-provider>
  </div>
</template>

<script setup lang="ts">
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import type { ThemeConfig } from 'ant-design-vue/es/config-provider/context'
import BasicLayout from '@/layouts/BasicLayout.vue'

dayjs.locale('zh-cn')

/**
 * 白色流光主题：
 * - 白色基底 + 青/紫/粉流光渐变
 * - 玻璃拟态卡片、丝滑过渡动效
 */
const theme: ThemeConfig = {
  token: {
    colorPrimary: '#6c5ce7',
    colorInfo: '#6c5ce7',
    colorSuccess: '#22b07d',
    colorWarning: '#f0a03c',
    colorError: '#ef5b7a',
    colorText: '#2b2d42',
    colorTextSecondary: '#7a7f9a',
    colorBgLayout: '#f6f7fb',
    colorBgContainer: '#ffffff',
    colorBorder: '#e8eaf3',
    borderRadius: 12,
    borderRadiusLG: 16,
    controlHeight: 38,
    fontFamily:
      '"Plus Jakarta Sans", "Aptos", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif',
  },
}
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');

:root {
  --pb-ink: #2b2d42;
  --pb-muted: #7a7f9a;
  --pb-faint: #b6bacd;
  --pb-bg: #f6f7fb;
  --pb-panel: #ffffff;
  --pb-line: #e8eaf3;
  --pb-violet: #6c5ce7;
  --pb-teal: #00c9a7;
  --pb-pink: #f572b6;
  --pb-sky: #38bdf8;
  --pb-grad: linear-gradient(120deg, #6c5ce7, #38bdf8 55%, #00c9a7);
  --pb-shadow: 0 18px 48px rgba(93, 102, 160, 0.12);
  --pb-shadow-lg: 0 28px 72px rgba(93, 102, 160, 0.18);
}

html,
body,
#app {
  margin: 0;
  min-height: 100%;
  background: var(--pb-bg);
  color: var(--pb-ink);
}

/* ---------- 全局流光背景（缓慢漂移的彩色光斑） ---------- */
body {
  position: relative;
  overflow-x: hidden;
  background: var(--pb-bg);
}

body::before,
body::after {
  position: fixed;
  z-index: -1;
  width: 56vmax;
  height: 56vmax;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.5;
  content: '';
  pointer-events: none;
}

body::before {
  top: -18vmax;
  left: -14vmax;
  background: radial-gradient(circle, rgba(108, 92, 231, 0.28), rgba(56, 189, 248, 0.14) 55%, transparent 72%);
  animation: aurora-a 26s ease-in-out infinite alternate;
}

body::after {
  right: -16vmax;
  bottom: -20vmax;
  background: radial-gradient(circle, rgba(0, 201, 167, 0.24), rgba(245, 114, 182, 0.14) 55%, transparent 72%);
  animation: aurora-b 32s ease-in-out infinite alternate;
}

@keyframes aurora-a {
  0% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(9vw, 6vh) scale(1.12);
  }
  100% {
    transform: translate(-4vw, 10vh) scale(0.94);
  }
}

@keyframes aurora-b {
  0% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(-8vw, -7vh) scale(1.1);
  }
  100% {
    transform: translate(5vw, -10vh) scale(0.95);
  }
}

button,
input,
textarea {
  font: inherit;
}

a {
  color: inherit;
  text-decoration: none;
}

* {
  box-sizing: border-box;
  scrollbar-width: thin;
  scrollbar-color: #c9cee2 transparent;
}

::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #c9cee2, #dfe3f2);
  border-radius: 10px;
}

::selection {
  color: #ffffff;
  background: var(--pb-violet);
}

/* ---------- Ant Design 丝滑化微调 ---------- */

.ant-btn {
  box-shadow: none;
  font-weight: 600;
}

.ant-btn-primary {
  border: 0;
  background: var(--pb-grad);
  background-size: 160% 160%;
  box-shadow: 0 10px 24px rgba(108, 92, 231, 0.28);
  transition: background-position 0.5s ease, transform 0.25s ease, box-shadow 0.25s ease;
}

.ant-btn-primary:not(:disabled):hover {
  background: var(--pb-grad);
  background-size: 160% 160%;
  background-position: 90% 50%;
  box-shadow: 0 14px 32px rgba(108, 92, 231, 0.38);
  transform: translateY(-2px);
}

.ant-btn-primary:not(:disabled):active {
  transform: translateY(0);
}

.ant-input,
.ant-input-affix-wrapper,
.ant-input-search .ant-input-group-addon .ant-btn,
.ant-input-number,
.ant-select .ant-select-selector,
.ant-picker {
  border-color: var(--pb-line);
  box-shadow: none;
  transition: border-color 0.25s ease, box-shadow 0.25s ease;
}

.ant-input:hover,
.ant-input-affix-wrapper:hover,
.ant-input:focus,
.ant-input-affix-wrapper-focused {
  border-color: #b7aef5;
  box-shadow: 0 0 0 4px rgba(108, 92, 231, 0.1);
}

.ant-card,
.ant-table-wrapper .ant-table,
.ant-modal-content {
  border: 1px solid rgba(232, 234, 243, 0.9);
  box-shadow: var(--pb-shadow);
}

.ant-card {
  overflow: hidden;
}

.ant-table-wrapper .ant-table-thead > tr > th {
  color: #7a7f9a;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
  background: #f8f9fd;
}

.ant-tag {
  border: 0;
  border-radius: 999px;
}

.ant-tabs .ant-tabs-tab {
  color: var(--pb-muted);
  font-weight: 600;
}

.ant-tabs .ant-tabs-tab-active .ant-tabs-tab-btn {
  color: var(--pb-violet);
}

.ant-tabs .ant-tabs-ink-bar {
  background: var(--pb-grad);
  height: 3px;
  border-radius: 3px;
}

.ant-pagination .ant-pagination-item-active {
  border-color: var(--pb-violet);
}

.ant-pagination .ant-pagination-item-active a {
  color: var(--pb-violet);
}

@media (prefers-reduced-motion: no-preference) {
  .ant-card {
    transition: transform 0.3s cubic-bezier(0.2, 0.7, 0.2, 1), box-shadow 0.3s ease,
      border-color 0.3s ease;
  }
}

/* ---------- 通用玻璃容器（管理页表单/表格外壳） ---------- */

.admin-panel {
  background: rgba(255, 255, 255, 0.78) !important;
  border: 1px solid var(--pb-line) !important;
  border-radius: 18px !important;
  box-shadow: var(--pb-shadow) !important;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.admin-panel .ant-card-body {
  padding: 20px 22px;
}

/* 让管理页表格卡也呈现玻璃质感 */
.admin-table-card {
  background: rgba(255, 255, 255, 0.82) !important;
  border: 1px solid var(--pb-line) !important;
  border-radius: 18px !important;
  box-shadow: var(--pb-shadow) !important;
  overflow: hidden;
}

.admin-table-card .ant-card-body {
  padding: 6px 8px;
}

.admin-table-card .ant-table {
  background: transparent;
}

/* 让表格行 hover 出现轻柔的流光描边 */
@media (prefers-reduced-motion: no-preference) {
  .admin-table-card .ant-table-tbody > tr:hover > td {
    background: rgba(108, 92, 231, 0.06) !important;
  }
}

</style>
