<template>
  <div id="userLoginPage" class="auth-page">
    <div class="auth-shell">
      <!-- 左侧：互动小人展区 -->
      <div class="buddy-stage">
        <div class="stage-ring ring-a"></div>
        <div class="stage-ring ring-b"></div>
        <div class="stage-star star-a">✦</div>
        <div class="stage-star star-b">✧</div>
        <LoginBuddy :state="buddyState" :trackRatio="trackRatio" />
        <div class="stage-slogan">
          <div class="slogan-title">PICTURE BUCKET</div>
          <div class="slogan-sub">你的云端灵感图库</div>
        </div>
      </div>

      <!-- 右侧：登录表单 -->
      <div class="auth-card">
        <div class="auth-badge">Picture Bucket</div>
        <h2 class="title">欢迎回来</h2>
        <div class="desc">使用邮箱或账号 + 密码 + 验证码登录</div>
        <a-form
          :model="formState"
          name="login"
          autocomplete="off"
          layout="vertical"
          @finish="handleSubmit"
        >
          <a-form-item
            name="account"
            label="邮箱 / 账号"
            :rules="[{ required: true, message: '请输入邮箱或账号' }]"
          >
            <a-input
              v-model:value="formState.account"
              size="large"
              placeholder="邮箱或用户账号"
              allow-clear
              @focus="activeField = 'account'"
              @blur="activeField = ''"
            />
          </a-form-item>

          <a-form-item
            name="userPassword"
            label="密码"
            :rules="[
              { required: true, message: '请输入密码' },
              { min: 8, message: '密码长度不能小于 8 位' },
            ]"
          >
            <a-input-password
              v-model:value="formState.userPassword"
              size="large"
              placeholder="请输入密码"
              :visibility-toggle="{
                visible: passwordVisible,
                onVisibleChange: handlePasswordVisibilityChange,
              }"
              @focus="activeField = 'password'"
              @blur="activeField = ''"
            />
          </a-form-item>

          <a-form-item
            name="emailCode"
            label="邮箱验证码"
            :rules="[
              { required: true, message: '请输入邮箱验证码' },
              { len: 6, message: '验证码为 6 位数字' },
            ]"
          >
            <div class="code-row">
              <a-input
                v-model:value="formState.emailCode"
                size="large"
                placeholder="6 位验证码"
              maxlength="6"
              allow-clear
              @focus="activeField = 'code'"
              @blur="activeField = ''"
              />
              <a-button
                class="code-button"
                size="large"
                :disabled="countdown > 0 || sendingCode"
                :loading="sendingCode"
                @click="sendCode"
              >
                {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
              </a-button>
            </div>
          </a-form-item>

          <div class="tips">
            没有账号？
            <RouterLink to="/user/register">去注册</RouterLink>
          </div>
          <a-form-item>
            <a-button type="primary" html-type="submit" size="large" block :loading="submitting">
              登录
            </a-button>
          </a-form-item>
        </a-form>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { sendEmailCodeUsingPost, userLoginUsingPost } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import LoginBuddy from '@/components/LoginBuddy.vue'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()

const formState = reactive<API.UserLoginRequest>({
  account: '',
  userPassword: '',
  emailCode: '',
})

// 小人状态：输入账号时跟随、输入密码时闭眼，点击密码可见按钮时偷看。
const activeField = ref<'account' | 'password' | 'code' | ''>('')
const passwordVisible = ref(false)
const buddyState = computed<'idle' | 'peek' | 'cover'>(() => {
  if (passwordVisible.value) return 'peek'
  if (activeField.value === 'password') return 'cover'
  if (activeField.value === 'account' || activeField.value === 'code') return 'peek'
  return 'idle'
})

const handlePasswordVisibilityChange = (visible: boolean) => {
  passwordVisible.value = visible
}

// 眼球跟随：账号输入越多，眼球看得越“入神”
const trackRatio = computed(() => {
  const len = formState.account?.length ?? 0
  return Math.min(len / 24, 1)
})

const submitting = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const startCountdown = (seconds = 60) => {
  countdown.value = seconds
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0 && timer) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

const sendCode = async () => {
  const account = formState.account?.trim()
  if (!account) {
    message.warning('请先填写邮箱或账号')
    return
  }

  sendingCode.value = true
  // 前端先锁定 60 秒，避免用户在接口返回前重复点击。
  startCountdown(60)
  try {
    // 后端支持邮箱，或按账号反查绑定邮箱后发送
    const res = await sendEmailCodeUsingPost(account)
    if (res.data.code === 0) {
      message.success('验证码已发送到绑定邮箱')
    } else {
      message.error(res.data.message || '验证码发送失败')
    }
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || '验证码发送失败，请确认后端已启动')
  } finally {
    sendingCode.value = false
  }
}

const handleSubmit = async (values: API.UserLoginRequest) => {
  submitting.value = true
  try {
    const payload: API.UserLoginRequest = {
      account: values.account?.trim(),
      userPassword: values.userPassword,
      emailCode: values.emailCode?.trim(),
    }
    const res = await userLoginUsingPost(payload)
    if (res.data.code === 0 && res.data.data) {
      loginUserStore.setLoginUser(res.data.data)
      message.success('登录成功')
      const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
      await router.replace(redirect || '/')
    } else {
      message.error(res.data.message || '登录失败')
    }
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || '登录失败，请确认后端已启动')
  } finally {
    submitting.value = false
  }
}

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.auth-page {
  min-height: calc(100vh - 128px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px 12px;
}

/* ---------- 整体外壳：玻璃拟态大卡片 ---------- */
.auth-shell {
  display: grid;
  grid-template-columns: 292px minmax(0, 1fr);
  width: 100%;
  max-width: 800px;
  border-radius: 22px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.95);
  box-shadow: 0 24px 60px rgba(64, 75, 143, 0.18);
  backdrop-filter: blur(22px);
}

/* ---------- 左侧小人舞台 ---------- */
.buddy-stage {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 28px 20px 24px;
  overflow: hidden;
  background:
    radial-gradient(20rem 14rem at 20% 12%, rgba(255, 255, 255, 0.5), transparent 60%),
    radial-gradient(16rem 12rem at 85% 88%, rgba(255, 255, 255, 0.32), transparent 62%),
    linear-gradient(160deg, #6658db 0%, #4e91eb 52%, #45c7d9 100%);
  background-size: 100% 100%, 100% 100%, 220% 220%;
  animation: stage-flow 10s ease-in-out infinite;
}

@keyframes stage-flow {
  0%,
  100% {
    background-position: 0 0, 0 0, 0% 40%;
  }
  50% {
    background-position: 0 0, 0 0, 100% 60%;
  }
}

/* 旋转光环 */
.stage-ring {
  position: absolute;
  border: 2px dashed rgba(255, 255, 255, 0.35);
  border-radius: 50%;
  pointer-events: none;
}

.ring-a {
  width: 208px;
  height: 208px;
  top: 14px;
  animation: ring-spin 18s linear infinite;
}

.ring-b {
  width: 262px;
  height: 262px;
  top: -12px;
  border-style: dotted;
  opacity: 0.55;
  animation: ring-spin 26s linear infinite reverse;
}

@keyframes ring-spin {
  to {
    transform: rotate(360deg);
  }
}

.stage-star {
  position: absolute;
  color: rgba(255, 255, 255, 0.85);
  pointer-events: none;
  animation: star-twinkle 2.6s ease-in-out infinite;
}

.star-a {
  top: 26px;
  left: 30px;
  font-size: 20px;
}

.star-b {
  bottom: 96px;
  right: 26px;
  font-size: 15px;
  animation-delay: 1.2s;
}

@keyframes star-twinkle {
  0%,
  100% {
    opacity: 0.9;
    transform: scale(1);
  }
  50% {
    opacity: 0.35;
    transform: scale(0.72) rotate(15deg);
  }
}

.buddy-stage :deep(.login-buddy) {
  position: relative;
  z-index: 1;
  margin-top: 10px;
}

/* 舞台上把气泡改成白底紫字更醒目 */
.buddy-stage :deep(.buddy-bubble) {
  border: 0;
}

.stage-slogan {
  position: relative;
  z-index: 1;
  text-align: center;
}

.slogan-title {
  color: #ffffff;
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 0.18em;
}

.slogan-sub {
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.82);
  font-size: 12px;
  letter-spacing: 0.1em;
}

/* ---------- 右侧表单 ---------- */
.auth-card {
  padding: 30px 34px 22px;
}

.auth-badge {
  display: inline-flex;
  padding: 5px 14px;
  border-radius: 999px;
  background: linear-gradient(120deg, rgba(108, 92, 231, 0.12), rgba(56, 189, 248, 0.12));
  border: 1px solid rgba(108, 92, 231, 0.22);
  color: var(--pb-violet);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  margin-bottom: 12px;
}

.title {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--pb-ink);
}

.desc {
  color: var(--pb-muted);
  margin-bottom: 18px;
  font-size: 13px;
}

.code-row {
  display: flex;
  gap: 8px;
}

.code-row :deep(.ant-input-affix-wrapper),
.code-row :deep(.ant-input) {
  flex: 1;
}

.code-row .code-button:disabled {
  color: #ffffff;
  background: #b6bacd;
  border-color: #b6bacd;
  opacity: 0.85;
}

.auth-card :deep(.ant-form-item) {
  margin-bottom: 14px;
}

.auth-card :deep(.ant-form-item-label) {
  padding-bottom: 4px;
}

.auth-card :deep(.ant-form-item-label > label) {
  color: #4f5270;
  font-size: 13px;
  font-weight: 600;
}

.auth-card :deep(.ant-input),
.auth-card :deep(.ant-input-affix-wrapper),
.auth-card :deep(.ant-btn-lg) {
  min-height: 40px;
}

.auth-card :deep(.ant-btn-primary) {
  box-shadow: 0 12px 24px rgba(86, 91, 219, 0.24);
}

.tips {
  color: var(--pb-faint);
  text-align: right;
  font-size: 13px;
  margin-bottom: 12px;
}

.tips a {
  color: var(--pb-violet);
  font-weight: 600;
}

@media (max-width: 720px) {
  .auth-shell {
    grid-template-columns: 1fr;
  }

  .buddy-stage {
    padding: 28px 20px 22px;
  }

  .buddy-stage :deep(.login-buddy) {
    max-width: 180px;
  }

  .ring-b {
    display: none;
  }

  .auth-card {
    padding: 26px 22px 18px;
  }
}
</style>
