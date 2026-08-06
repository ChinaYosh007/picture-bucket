<template>
  <div class="login-buddy" :class="`buddy--${props.state}`">
    <div class="buddy-scale">
      <div class="buddy-clip">
        <img class="buddy-sprite" :src="spriteSource" alt="登录小助手" />
      </div>
    </div>
    <div class="buddy-bubble">{{ bubbleText }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import buddyCover from '@/assets/buddy-cover.png'
import buddyIdle from '@/assets/buddy-idle.png'
import buddyPeek from '@/assets/buddy-peek.png'

interface Props {
  state?: 'idle' | 'peek' | 'cover'
  trackRatio?: number
}

const props = withDefaults(defineProps<Props>(), {
  state: 'idle',
  trackRatio: 0,
})

const bubbleText = computed(() => {
  if (props.state === 'cover') return '我、我不看啦！'
  if (props.state === 'peek') return '诶？让我看看…'
  return '欢迎回来呀 ~'
})

const spriteSource = computed(() => {
  if (props.state === 'cover') return buddyCover
  if (props.state === 'peek') return buddyPeek
  return buddyIdle
})
</script>

<style scoped>
/*
 * 三张贴图均为 1536 × 208 的横向精灵条，单帧实际宽度是 192px。
 * idle/cover 各 6 帧，peek 有 8 帧；此前按 256px 裁剪，才会露出下一帧的残片。
 */
.login-buddy {
  position: relative;
  width: 192px;
  height: 166px;
}

.buddy-scale {
  width: 192px;
  height: 208px;
  transform: scale(0.8);
  transform-origin: top center;
}

.buddy-clip {
  position: relative;
  width: 192px;
  height: 208px;
  overflow: hidden;
}

.buddy-sprite {
  position: absolute;
  top: 0;
  left: 0;
  display: block;
  width: 1536px;
  height: 208px;
  max-width: none;
  will-change: transform;
}

.buddy--idle .buddy-sprite,
.buddy--cover .buddy-sprite {
  animation: buddy-frames-6 3.2s steps(6) infinite;
}

.buddy--peek .buddy-sprite {
  animation: buddy-frames-8 2.4s steps(8) infinite;
}

.buddy-bubble {
  position: absolute;
  z-index: 1;
  top: 0;
  right: -17px;
  padding: 6px 13px;
  border-radius: 16px 16px 16px 4px;
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(23, 76, 148, 0.2);
  color: #167eaa;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  animation: bubble-pop 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes buddy-frames-6 {
  to {
    transform: translateX(-1152px);
  }
}

@keyframes buddy-frames-8 {
  to {
    transform: translateX(-1536px);
  }
}

@keyframes bubble-pop {
  from {
    opacity: 0;
    transform: translateY(6px) scale(0.85);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (prefers-reduced-motion: reduce) {
  .buddy-sprite,
  .buddy-bubble {
    animation: none !important;
  }
}
</style>
