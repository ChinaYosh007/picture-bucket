<template>
  <div class="admin-hero">
    <div class="admin-hero__main">
      <h1 class="admin-hero__title">{{ title }}</h1>
      <p v-if="subtitle" class="admin-hero__sub">{{ subtitle }}</p>
    </div>
    <div class="admin-hero__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script lang="ts" setup>
defineProps<{
  title: string
  subtitle?: string
}>()
</script>

<style scoped>
.admin-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 16px 20px;
  margin-bottom: 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--pb-line, #eef0f6);
  box-shadow: var(--pb-shadow, 0 10px 30px rgba(41, 50, 71, 0.06));
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.admin-hero__title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.01em;
  background: var(--pb-grad, linear-gradient(120deg, #6c5ce7, #38bdf8 55%, #00c9a7));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.admin-hero__sub {
  margin: 3px 0 0;
  color: var(--pb-muted, #71809a);
  font-size: 13px;
}

.admin-hero__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
