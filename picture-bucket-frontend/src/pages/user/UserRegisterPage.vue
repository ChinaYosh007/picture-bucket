<template>
  <div id="userRegisterPage" class="auth-page">
    <div class="auth-shell">
      <!-- 左侧：互动小人展区 -->
      <div class="buddy-stage">
        <div class="stage-ring ring-a"></div>
        <div class="stage-ring ring-b"></div>
        <div class="stage-star star-a">✦</div>
        <div class="stage-star star-b">✧</div>
        <LoginBuddy :state="buddyState" :trackRatio="trackRatio" />
        <div class="stage-slogan">
          <div class="slogan-title">JOIN US</div>
          <div class="slogan-sub">开启你的云端图库之旅</div>
        </div>
      </div>

      <!-- 右侧：注册表单 -->
      <div class="auth-card">
        <div class="auth-badge">Picture Bucket</div>
        <h2 class="title">创建账号</h2>
        <div class="desc">使用邮箱验证码完成注册，昵称可在登录后完善</div>
        <a-form
          :model="formState"
          name="register"
          autocomplete="off"
          layout="vertical"
          @finish="handleSubmit"
        >
          <a-form-item
            name="email"
            label="邮箱"
            :rules="[
              { required: true, message: '请输入邮箱' },
              { type: 'email', message: '邮箱格式不正确' },
            ]"
          >
            <a-input
              v-model:value="formState.email"
              size="large"
              placeholder="请输入邮箱"
              allow-clear
              @focus="buddyState = 'peek'"
              @blur="buddyState = 'idle'"
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
                @focus="buddyState = 'peek'"
                @blur="buddyState = 'idle'"
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

          <a-form-item
            name="userPassword"
            label="密码"
            :rules="[
              { required: true, message: '请输入密码' },
              { min: 8, message: '密码长度不能小于 8 位' },
              { max: 64, message: '密码长度不能超过 64 位' },
            ]"
          >
            <a-input-password
              v-model:value="formState.userPassword"
              size="large"
              placeholder="密码（8–64 位）"
              @focus="buddyState = 'cover'"
              @blur="buddyState = 'idle'"
            />
          </a-form-item>

          <a-form-item
            name="checkPassword"
            label="确认密码"
            :rules="[
              { required: true, message: '请输入确认密码' },
              { min: 8, message: '确认密码长度不能小于 8 位' },
              { max: 64, message: '确认密码长度不能超过 64 位' },
            ]"
          >
            <a-input-password
              v-model:value="formState.checkPassword"
              size="large"
              placeholder="再次输入密码"
              @focus="buddyState = 'cover'"
              @blur="buddyState = 'idle'"
            />
          </a-form-item>

          <a-form-item name="inviteCode" label="邀请码（可选）">
            <a-input
              v-model:value="formState.inviteCode"
              size="large"
              placeholder="有邀请码可填写"
              allow-clear
              @focus="buddyState = 'peek'"
              @blur="buddyState = 'idle'"
            />
          </a-form-item>

          <div class="tips">
            已有账号？
            <RouterLink to="/user/login">去登录</RouterLink>
          </div>
          <a-form-item>
            <a-button type="primary" html-type="submit" size="large" block :loading="submitting">
              注册
            </a-button>
          </a-form-item>
        </a-form>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { sendEmailCodeUsingPost, userRegisterUsingPost } from '@/api/userController.ts'
import LoginBuddy from '@/components/LoginBuddy.vue'

const router = useRouter()
const route = useRoute()

const formState = reactive<API.UserRegisterRequest>({
  email: '',
  userPassword: '',
  checkPassword: '',
  emailCode: '',
  inviteCode: '',
})

// 小人状态：peek=偷看 cover=捂眼 idle=待机
const buddyState = ref<'idle' | 'peek' | 'cover'>('idle')

// 眼球跟随邮箱输入长度
const trackRatio = computed(() => {
  const len = formState.email?.length ?? 0
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
  const email = formState.email?.trim()
  if (!email) {
    message.warning('请先填写邮箱')
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    message.warning('邮箱格式不正确')
    return
  }

  sendingCode.value = true
  // 前端先锁定 60 秒，避免用户在接口返回前重复点击。
  startCountdown(60)
  try {
    const res = await sendEmailCodeUsingPost(email)
    if (res.data.code === 0) {
      message.success('验证码已发送，请查收邮件')
    } else {
      message.error(res.data.message || '验证码发送失败')
    }
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || '验证码发送失败，请确认后端已启动')
  } finally {
    sendingCode.value = false
  }
}

const handleSubmit = async (values: API.UserRegisterRequest) => {
  if (values.userPassword !== values.checkPassword) {
    message.error('两次输入的密码不一致')
    return
  }

  submitting.value = true
  try {
    const payload: API.UserRegisterRequest = {
      email: values.email?.trim(),
      userPassword: values.userPassword,
      checkPassword: values.checkPassword,
      emailCode: values.emailCode?.trim(),
      inviteCode: values.inviteCode?.trim() || undefined,
    }
    const res = await userRegisterUsingPost(payload)
    if (res.data.code === 0 && res.data.data) {
      message.success('注册成功，请登录')
      await router.push({ path: '/user/login', replace: true })
    } else {
      message.error(res.data.message || '注册失败')
    }
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || '注册失败，请确认后端已启动')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  const inviteFromQuery = route.query.inviteCode
  if (typeof inviteFromQuery === 'string' && inviteFromQuery) {
    formState.inviteCode = inviteFromQuery
  }
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.auth-page {
  min-height: calc(100vh - 180px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 12px;
}

/* ---------- 整体外壳：玻璃拟态大卡片 ---------- */
.auth-shell {
  display: grid;
  grid-template-columns: 320px 1fr;
  width: 100%;
  max-width: 920px;
  border-radius: 28px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.95);
  box-shadow: 0 32px 80px rgba(93, 102, 160, 0.2);
  backdrop-filter: blur(22px);
}

/* ---------- 左侧小人舞台 ---------- */
.buddy-stage {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  padding: 40px 24px 32px;
  overflow: hidden;
  background:
    radial-gradient(20rem 14rem at 20% 12%, rgba(255, 255, 255, 0.5), transparent 60%),
    radial-gradient(16rem 12rem at 85% 88%, rgba(255, 255, 255, 0.32), transparent 62%),
    linear-gradient(160deg, #6bb8ff 0%, #38cfe0 55%, #5eead4 100%);
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

.stage-ring {
  position: absolute;
  border: 2px dashed rgba(255, 255, 255, 0.35);
  border-radius: 50%;
  pointer-events: none;
}

.ring-a {
  width: 230px;
  height: 230px;
  top: 30px;
  animation: ring-spin 18s linear infinite;
}

.ring-b {
  width: 290px;
  height: 290px;
  top: 0;
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
}

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
  font-size: 18px;
  font-weight: 800;
  letter-spacing: 0.18em;
}

.slogan-sub {
  margin-top: 6px;
  color: rgba(255, 255, 255, 0.82);
  font-size: 12px;
  letter-spacing: 0.1em;
}

/* ---------- 右侧表单 ---------- */
.auth-card {
  padding: 34px 42px 24px;
}

.auth-badge {
  display: inline-flex;
  padding: 5px 14px;
  border-radius: 999px;
  background: linear-gradient(120deg, rgba(0, 201, 167, 0.12), rgba(56, 189, 248, 0.12));
  border: 1px solid rgba(0, 201, 167, 0.26);
  color: #00a488;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  margin-bottom: 14px;
}

.title {
  margin: 0 0 8px;
  font-size: 30px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--pb-ink);
}

.desc {
  color: var(--pb-muted);
  margin-bottom: 22px;
  font-size: 14px;
}

.auth-card :deep(.ant-form-item) {
  margin-bottom: 16px;
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

.tips {
  color: var(--pb-faint);
  text-align: right;
  font-size: 13px;
  margin-bottom: 14px;
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
    padding: 26px 20px 20px;
  }

  .buddy-stage :deep(.login-buddy) {
    max-width: 160px;
  }

  .ring-b {
    display: none;
  }

  .auth-card {
    padding: 24px 22px 18px;
  }
}
</style>
