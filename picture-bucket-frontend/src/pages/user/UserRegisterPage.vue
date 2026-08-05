<template>
  <div id="userRegisterPage" class="auth-page">
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
          />
        </a-form-item>

        <a-form-item name="inviteCode" label="邀请码（可选）">
          <a-input
            v-model:value="formState.inviteCode"
            size="large"
            placeholder="有邀请码可填写"
            allow-clear
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
</template>

<script lang="ts" setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { sendEmailCodeUsingPost, userRegisterUsingPost } from '@/api/userController.ts'

const router = useRouter()
const route = useRoute()

const formState = reactive<API.UserRegisterRequest>({
  email: '',
  userPassword: '',
  checkPassword: '',
  emailCode: '',
  inviteCode: '',
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
  background:
    radial-gradient(22rem 15rem at 18% 18%, rgba(223, 227, 255, 0.68), transparent 70%),
    radial-gradient(22rem 16rem at 82% 78%, rgba(218, 241, 232, 0.58), transparent 70%);
}

.auth-card {
  width: 100%;
  max-width: 420px;
  padding: 36px 32px 28px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(255, 255, 255, 0.96);
  box-shadow: 0 24px 60px rgba(71, 83, 123, 0.12);
  backdrop-filter: blur(16px);
}

.auth-badge {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: #eef0ff;
  color: #6470ca;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 14px;
}

.title {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: #293247;
}

.desc {
  color: #7d8ba4;
  margin-bottom: 28px;
  font-size: 14px;
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
  background: #94a3b8;
  border-color: #94a3b8;
  opacity: 0.8;
}

.tips {
  color: #94a3b8;
  text-align: right;
  font-size: 13px;
  margin-bottom: 16px;
}

.tips a {
  color: #4f46e5;
  font-weight: 500;
}
</style>
