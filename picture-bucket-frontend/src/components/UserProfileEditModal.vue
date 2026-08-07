<template>
  <a-modal
    :open="open"
    title="编辑个人资料"
    :confirm-loading="submitting"
    ok-text="保存"
    cancel-text="取消"
    width="560px"
    :mask-closable="false"
    @ok="handleSubmit"
    @update:open="handleOpenChange"
  >
    <div class="profile-edit">
      <div class="avatar-row">
        <a-avatar :src="form.userAvatar" :size="72">
          {{ form.userName ? form.userName.charAt(0) : 'U' }}
        </a-avatar>
        <div class="avatar-field">
          <div class="avatar-field__label">头像</div>
          <div class="avatar-field__hint">支持 JPG、PNG、WebP，最大 3 MB。</div>
          <a-upload
            :show-upload-list="false"
            accept="image/jpeg,image/png,image/webp"
            :custom-request="handleAvatarUpload"
            :before-upload="beforeAvatarUpload"
          >
            <a-button size="small" :loading="avatarUploading">
              <template #icon><UploadOutlined /></template>
              上传头像
            </a-button>
          </a-upload>
        </div>
      </div>

      <a-form layout="vertical" class="profile-form">
        <a-form-item label="登录账号" required>
          <a-input
            v-model:value="form.userAccount"
            placeholder="3–64 位字母、数字、下划线或连字符"
            :maxlength="64"
            autocomplete="username"
            allow-clear
          />
          <div class="field-hint">修改后可立即使用新账号登录。</div>
        </a-form-item>
        <a-form-item label="昵称" required>
          <a-input
            v-model:value="form.userName"
            placeholder="给自己起个名字"
            :maxlength="20"
            show-count
          />
        </a-form-item>
        <a-form-item class="profile-composer-item">
          <div class="profile-composer">
            <div class="profile-composer__header">
              <span class="profile-composer__label">个人简介</span>
              <span class="profile-composer__count">{{ profileLength }} / 128</span>
            </div>
            <a-textarea
              v-model:value="form.userProfile"
              :auto-size="{ minRows: 3, maxRows: 5 }"
              :bordered="false"
              placeholder="写下你的创作偏好、收藏主题或正在寻找的灵感…"
              :maxlength="128"
            />
            <div class="profile-composer__footer">让其他人更快认识你</div>
          </div>
        </a-form-item>
      </a-form>

      <section class="security-section" aria-label="安全设置">
        <div class="security-section__header">
          <div>
            <div class="security-section__title">安全设置</div>
            <div class="security-section__hint">身份信息和登录凭证在此单独管理。</div>
          </div>
        </div>

        <div class="security-action">
          <div>
            <div class="security-action__title">绑定邮箱</div>
            <div class="security-section__hint">{{ maskedEmail }}</div>
          </div>
          <a-button type="link" size="small" @click="toggleEmailForm">
            {{ emailFormVisible ? '收起' : '修改邮箱' }}
          </a-button>
        </div>

        <a-form v-if="emailFormVisible" layout="vertical" class="email-form">
          <a-form-item label="新邮箱" required>
            <a-input
              v-model:value="newEmail"
              type="email"
              placeholder="输入新的绑定邮箱"
              :maxlength="254"
              autocomplete="email"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="新邮箱验证码" required>
            <div class="email-code-control">
              <a-input
                v-model:value="form.emailCode"
                placeholder="请输入 6 位验证码"
                :maxlength="6"
                autocomplete="one-time-code"
              />
              <a-button :loading="emailCodeSending" @click="handleSendEmailCode">发送验证码</a-button>
            </div>
            <div class="field-hint">验证码将发送到新邮箱，验证通过后才会替换原邮箱。</div>
          </a-form-item>
        </a-form>

        <div class="security-action security-action--separated">
          <div>
            <div class="security-action__title">登录密码</div>
            <div class="security-section__hint">修改密码时需要验证当前密码。</div>
          </div>
          <a-button type="link" size="small" @click="passwordFormVisible = !passwordFormVisible">
            <template #icon><LockOutlined /></template>
            {{ passwordFormVisible ? '收起' : '修改密码' }}
          </a-button>
        </div>

        <a-form v-if="passwordFormVisible" layout="vertical" class="password-form">
          <a-form-item label="当前密码" required>
            <a-input-password
              v-model:value="passwordForm.currentPassword"
              placeholder="请输入当前登录密码"
              :maxlength="64"
              autocomplete="current-password"
            />
          </a-form-item>
          <a-form-item label="新密码" required>
            <a-input-password
              v-model:value="passwordForm.newPassword"
              placeholder="8–64 位密码"
              :maxlength="64"
              autocomplete="new-password"
            />
          </a-form-item>
          <a-form-item label="确认新密码" required>
            <a-input-password
              v-model:value="passwordForm.confirmPassword"
              placeholder="再次输入新密码"
              :maxlength="64"
              autocomplete="new-password"
            />
          </a-form-item>
          <div class="password-form__actions">
            <span>修改后需使用新密码登录。</span>
            <a-button type="primary" ghost :loading="passwordSubmitting" @click="handlePasswordUpdate">
              更新密码
            </a-button>
          </div>
        </a-form>
      </section>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
import { computed, reactive, ref, watch } from 'vue'
import { LockOutlined, UploadOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { uploadFileUsingPost } from '@/api/fileController.ts'
import { editUserUsingPost, sendEmailCodeUsingPost, updatePasswordUsingPost } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{
  'update:open': [value: boolean]
  success: []
}>()

const loginUserStore = useLoginUserStore()
const submitting = ref(false)
const passwordSubmitting = ref(false)
const passwordFormVisible = ref(false)
const emailFormVisible = ref(false)
const emailCodeSending = ref(false)
const avatarUploading = ref(false)
const originalEmail = ref('')
const newEmail = ref('')

const form = reactive<API.UserEditRequest>({
  userAccount: '',
  email: '',
  emailCode: '',
  userName: '',
  userAvatar: '',
  userProfile: '',
})

const emailChanged = computed(() => {
  const email = newEmail.value.trim().toLowerCase()
  return email.length > 0 && email !== originalEmail.value
})

const profileLength = computed(() => (form.userProfile ?? '').length)

const maskedEmail = computed(() => {
  const email = originalEmail.value
  const [localPart, domain] = email.split('@')
  if (!localPart || !domain) return '邮箱信息加载中'
  const visiblePart = localPart.slice(0, Math.min(2, localPart.length))
  return `${visiblePart}${'*'.repeat(Math.max(2, localPart.length - visiblePart.length))}@${domain}`
})

const passwordForm = reactive<API.UserPasswordUpdateRequest>({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const USER_ACCOUNT_PATTERN = /^[A-Za-z0-9_-]{3,64}$/

const resetPasswordForm = () => {
  passwordForm.currentPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

const resetEmailForm = () => {
  newEmail.value = ''
  form.emailCode = ''
}

const toggleEmailForm = () => {
  if (emailFormVisible.value) {
    resetEmailForm()
  }
  emailFormVisible.value = !emailFormVisible.value
}

const beforeAvatarUpload = (file: File) => {
  const supportedTypes = ['image/jpeg', 'image/png', 'image/webp']
  if (!supportedTypes.includes(file.type)) {
    message.error('头像仅支持 JPG、PNG 或 WebP 图片')
    return false
  }
  if (file.size > 3 * 1024 * 1024) {
    message.error('头像图片不能超过 3 MB')
    return false
  }
  return true
}

const handleAvatarUpload = async ({ file, onSuccess, onError }: any) => {
  avatarUploading.value = true
  try {
    const res = await uploadFileUsingPost({}, file as File)
    const avatarUrl = res.data?.data
    if (res.data?.code !== 0 || !avatarUrl) {
      const error = new Error(res.data?.message || '头像上传失败')
      onError?.(error)
      message.error(error.message)
      return
    }
    form.userAvatar = avatarUrl
    onSuccess?.(res.data, file)
    message.success('头像上传成功')
  } catch (error: any) {
    onError?.(error)
    message.error(error?.response?.data?.message || error?.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}

const handleSendEmailCode = async () => {
  const email = newEmail.value.trim().toLowerCase()
  if (!EMAIL_PATTERN.test(email)) {
    message.warning('请输入有效的新邮箱地址')
    return
  }
  if (!emailChanged.value) {
    message.warning('请先修改绑定邮箱')
    return
  }

  emailCodeSending.value = true
  try {
    const res = await sendEmailCodeUsingPost(email)
    if (res.data.code === 0) {
      message.success('验证码已发送，请查收新邮箱')
    } else {
      message.error('验证码发送失败：' + (res.data.message ?? '未知错误'))
    }
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '验证码发送失败')
  } finally {
    emailCodeSending.value = false
  }
}

// 每次打开时，用当前登录用户的最新资料回填表单
watch(
  () => props.open,
  async (open) => {
    if (!open) return
    await loginUserStore.fetchLoginUser()
    const u = loginUserStore.loginUser
    form.userAccount = u.userAccount ?? ''
    form.email = u.email ?? ''
    form.emailCode = ''
    form.userName = u.userName ?? ''
    form.userAvatar = u.userAvatar ?? ''
    form.userProfile = u.userProfile ?? ''
    originalEmail.value = (u.email ?? '').trim().toLowerCase()
    newEmail.value = ''
    emailFormVisible.value = false
    passwordFormVisible.value = false
    resetPasswordForm()
  },
)

const handleOpenChange = (open: boolean) => {
  if (!open) {
    resetEmailForm()
    emailFormVisible.value = false
    passwordFormVisible.value = false
    resetPasswordForm()
  }
  emit('update:open', open)
}

const handleSubmit = async () => {
  const userAccount = (form.userAccount ?? '').trim()
  const email = emailChanged.value ? newEmail.value.trim().toLowerCase() : originalEmail.value
  const emailCode = (form.emailCode ?? '').trim()
  const userName = (form.userName ?? '').trim()
  const userAvatar = (form.userAvatar ?? '').trim()
  const userProfile = (form.userProfile ?? '').trim()
  if (!USER_ACCOUNT_PATTERN.test(userAccount)) {
    message.warning('账号仅支持 3–64 位字母、数字、下划线或连字符')
    return
  }
  if (!EMAIL_PATTERN.test(email)) {
    message.warning('请输入有效的邮箱地址')
    return
  }
  if (emailChanged.value && !/^\d{6}$/.test(emailCode)) {
    message.warning('请输入新邮箱收到的 6 位验证码')
    return
  }
  if (!userName) {
    message.warning('昵称不能为空')
    return
  }
  submitting.value = true
  try {
    const res = await editUserUsingPost({
      userAccount,
      email,
      emailCode,
      userName,
      userAvatar,
      userProfile,
    })
    if (res.data.code === 0) {
      message.success('资料已更新')
      // 同步刷新顶部头像/昵称
      await loginUserStore.fetchLoginUser()
      originalEmail.value = email
      emit('success')
      emit('update:open', false)
    } else {
      message.error('更新失败，' + (res.data.message ?? '未知错误'))
    }
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || '更新失败')
  } finally {
    submitting.value = false
  }
}

const handlePasswordUpdate = async () => {
  const currentPassword = passwordForm.currentPassword ?? ''
  const newPassword = passwordForm.newPassword ?? ''
  const confirmPassword = passwordForm.confirmPassword ?? ''
  if (!currentPassword || !newPassword || !confirmPassword) {
    message.warning('请完整填写当前密码、新密码和确认密码')
    return
  }
  if (newPassword.length < 8 || newPassword.length > 64) {
    message.warning('新密码长度应为 8–64 位')
    return
  }
  if (newPassword !== confirmPassword) {
    message.warning('两次输入的新密码不一致')
    return
  }
  if (currentPassword === newPassword) {
    message.warning('新密码不能与当前密码相同')
    return
  }

  passwordSubmitting.value = true
  try {
    const res = await updatePasswordUsingPost({ currentPassword, newPassword, confirmPassword })
    if (res.data.code === 0) {
      message.success('密码已更新，请在下次登录时使用新密码')
      passwordFormVisible.value = false
      resetPasswordForm()
    } else {
      message.error('密码更新失败：' + (res.data.message ?? '未知错误'))
    }
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '密码更新失败')
  } finally {
    passwordSubmitting.value = false
  }
}
</script>

<style scoped>
.profile-edit {
  padding-top: 6px;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}

.avatar-row :deep(.ant-avatar) {
  flex: 0 0 auto;
  background: linear-gradient(135deg, #6c5ce7, #38bdf8);
  color: #fff;
  font-weight: 700;
  font-size: 26px;
  box-shadow: 0 8px 18px rgba(108, 92, 231, 0.28);
}

.avatar-field {
  flex: 1 1 auto;
  min-width: 0;
}

.avatar-field__label {
  font-size: 12px;
  font-weight: 600;
  color: var(--pb-muted, #71809a);
  margin-bottom: 6px;
}

.avatar-field__hint {
  margin-bottom: 8px;
  color: var(--pb-muted, #71809a);
  font-size: 12px;
}

.profile-form {
  margin-bottom: 14px;
}

.profile-form :deep(.ant-form-item-label > label) {
  font-weight: 600;
  color: var(--pb-ink, #293247);
}

.profile-composer-item {
  margin-bottom: 0;
}

.profile-composer {
  padding: 12px 14px 10px;
  border: 1px solid var(--pb-line, #e8eaf3);
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(108, 92, 231, 0.045), rgba(56, 189, 248, 0.035));
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.profile-composer:focus-within {
  border-color: #b7aef5;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 0 0 4px rgba(108, 92, 231, 0.1);
}

.profile-composer__header,
.profile-composer__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.profile-composer__label {
  color: var(--pb-ink, #293247);
  font-size: 13px;
  font-weight: 700;
}

.profile-composer__count,
.profile-composer__footer {
  color: var(--pb-muted, #71809a);
  font-size: 12px;
}

.profile-composer :deep(textarea.ant-input) {
  min-height: 76px !important;
  padding: 8px 0 6px;
  background: transparent;
  box-shadow: none;
  line-height: 1.65;
  resize: vertical;
}

.profile-composer :deep(textarea.ant-input:focus) {
  box-shadow: none;
}

.field-hint {
  margin-top: 5px;
  color: var(--pb-muted, #71809a);
  font-size: 12px;
  line-height: 1.45;
}

.email-code-control {
  display: flex;
  gap: 8px;
}

.email-code-control > :first-child {
  flex: 1;
  min-width: 0;
}

.security-section {
  padding-top: 16px;
  border-top: 1px solid var(--pb-line, #e8eaf3);
}

.security-section__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.security-section__title {
  color: var(--pb-ink, #293247);
  font-size: 14px;
  font-weight: 700;
}

.security-section__hint,
.password-form__actions span {
  color: var(--pb-muted, #71809a);
  font-size: 12px;
  line-height: 1.5;
}

.security-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 0;
}

.security-action--separated {
  margin-top: 14px;
  border-top: 1px solid var(--pb-line, #e8eaf3);
}

.security-action__title {
  color: var(--pb-ink, #293247);
  font-size: 13px;
  font-weight: 700;
}

.email-form {
  margin-top: 2px;
}

.email-form :deep(.ant-form-item) {
  margin-bottom: 14px;
}

.email-form :deep(.ant-form-item-label > label) {
  font-weight: 600;
  color: var(--pb-ink, #293247);
}

.password-form {
  margin-top: 14px;
}

.password-form :deep(.ant-form-item) {
  margin-bottom: 14px;
}

.password-form :deep(.ant-form-item-label > label) {
  font-weight: 600;
  color: var(--pb-ink, #293247);
}

.password-form__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

@media (max-width: 520px) {
  .email-code-control {
    align-items: stretch;
    flex-direction: column;
  }

  .password-form__actions {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
