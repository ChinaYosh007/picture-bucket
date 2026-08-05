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

const theme: ThemeConfig = {
  token: {
    colorPrimary: '#6d74cf',
    colorInfo: '#6d74cf',
    colorSuccess: '#58a88a',
    colorWarning: '#d99b5b',
    colorError: '#d96d77',
    colorText: '#293247',
    colorTextSecondary: '#71809a',
    colorBgLayout: '#f7f8fc',
    colorBgContainer: '#ffffff',
    colorBorder: '#e5e9f2',
    borderRadius: 14,
    borderRadiusLG: 20,
    controlHeight: 42,
    fontFamily:
      '"Aptos", "Plus Jakarta Sans", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif',
  },
}
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap');

:root {
  --pb-ink: #293247;
  --pb-muted: #71809a;
  --pb-paper: #ffffff;
  --pb-mist: #f7f8fc;
  --pb-lilac: #eef0ff;
  --pb-lilac-strong: #6d74cf;
  --pb-mint: #edf8f2;
  --pb-peach: #fff4ea;
  --pb-line: #e5e9f2;
  --pb-shadow: 0 14px 38px rgba(71, 83, 123, 0.08);
}

html,
body,
#app {
  margin: 0;
  min-height: 100%;
  background: var(--pb-mist);
  color: var(--pb-ink);
}

body {
  background:
    radial-gradient(70rem 32rem at -10% -8%, rgba(191, 207, 255, 0.32), transparent 58%),
    radial-gradient(58rem 28rem at 106% 0%, rgba(206, 238, 229, 0.3), transparent 62%),
    var(--pb-mist);
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
  scrollbar-color: #c8d0df transparent;
}

::selection {
  color: #33408d;
  background: #e2e6ff;
}

.ant-btn {
  box-shadow: none;
  font-weight: 600;
}

.ant-btn-primary {
  border: 0;
  box-shadow: 0 7px 16px rgba(109, 116, 207, 0.2);
}

.ant-btn-primary:not(:disabled):hover {
  background: #5e66c1;
  box-shadow: 0 9px 20px rgba(109, 116, 207, 0.28);
  transform: translateY(-1px);
}

.ant-input,
.ant-input-affix-wrapper,
.ant-input-search .ant-input-group-addon .ant-btn {
  border-color: var(--pb-line);
  box-shadow: none;
}

.ant-input:hover,
.ant-input-affix-wrapper:hover,
.ant-input:focus,
.ant-input-affix-wrapper-focused {
  border-color: #b6bceb;
  box-shadow: 0 0 0 3px rgba(109, 116, 207, 0.1);
}

.ant-card,
.ant-table-wrapper .ant-table,
.ant-modal-content {
  border: 1px solid var(--pb-line);
  box-shadow: var(--pb-shadow);
}

.ant-card {
  overflow: hidden;
}

.ant-table-wrapper .ant-table-thead > tr > th {
  color: #66748d;
  font-size: 12px;
  font-weight: 700;
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
  color: var(--pb-lilac-strong);
}

@media (prefers-reduced-motion: no-preference) {
  .ant-btn-primary,
  .ant-card {
    transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
  }
}
</style>
