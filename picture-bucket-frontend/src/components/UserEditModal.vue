<template>
  <a-modal
    :open="open"
    :title="isEdit ? '编辑用户' : '添加用户'"
    :confirm-loading="submitting"
    :ok-text="isEdit ? '保存' : '创建'"
    cancel-text="取消"
    width="480px"
    :mask-closable="false"
    @ok="handleSubmit"
    @update:open="handleOpenChange"
  >
    <a-alert
      v-if="isEdit"
      class="security-notice"
      type="info"
      show-icon
      message="密码不会显示或修改；如需重置密码，请使用独立的密码重置流程。"
    />
    <a-form layout="vertical" class="user-edit-form">
      <a-form-item label="账号" required>
        <a-input
          v-model:value="form.userAccount"
          placeholder="登录账号（唯一）"
          :maxlength="64"
          autocomplete="off"
          allow-clear
        />
        <div v-if="isEdit" class="field-hint">保存后，用户可使用新账号登录。</div>
      </a-form-item>
      <a-form-item label="邮箱" required>
        <a-input
          v-model:value="form.email"
          type="email"
          placeholder="用于接收登录验证码"
          :maxlength="254"
          autocomplete="off"
          allow-clear
        />
      </a-form-item>
      <template v-if="!isEdit">
        <a-form-item label="初始密码" required>
          <a-input-password
            v-model:value="form.userPassword"
            placeholder="8–64 位"
            :maxlength="64"
            autocomplete="new-password"
          />
        </a-form-item>
      </template>
      <a-form-item label="昵称">
        <a-input v-model:value="form.userName" placeholder="用户昵称（可选）" :maxlength="20" allow-clear />
      </a-form-item>
      <a-form-item label="头像链接">
        <div class="avatar-upload-field">
          <a-input
            v-model:value="form.userAvatar"
            placeholder="头像图片 URL（可选，仅支持 http(s)）"
            :maxlength="500"
            autocomplete="off"
            allow-clear
          />
          <a-upload
            accept="image/jpeg,image/png,image/webp"
            :show-upload-list="false"
            :custom-request="handleAvatarUpload"
            :before-upload="beforeAvatarUpload"
          >
            <a-button :loading="avatarUploading">
              <template #icon><UploadOutlined /></template>
              上传
            </a-button>
          </a-upload>
        </div>
        <div class="field-hint">支持 JPG、PNG、WebP，最大 3 MB；上传后会自动填入头像链接。</div>
      </a-form-item>
      <a-form-item label="用户角色">
        <a-select
          v-model:value="form.userRole"
          placeholder="请选择角色"
          :options="roleOptions"
        />
      </a-form-item>
      <a-form-item label="个人简介">
        <a-textarea
          v-model:value="form.userProfile"
          placeholder="个人简介（可选）"
          :rows="3"
          :maxlength="128"
          show-count
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script lang="ts" setup>
import { reactive, ref, watch } from 'vue'
import { UploadOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { uploadFileUsingPost } from '@/api/fileController.ts'
import { addUserUsingPost, updateUserUsingPost } from '@/api/userController.ts'

const props = defineProps<{
  open: boolean
  isEdit: boolean
  record?: API.User | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  success: []
}>()

const roleOptions = [
  { label: '普通用户', value: 'user' },
  { label: '管理员', value: 'admin' },
]

const submitting = ref(false)
const avatarUploading = ref(false)

const form = reactive<{
  userAccount?: string
  email?: string
  userPassword?: string
  userName?: string
  userAvatar?: string
  userRole?: string
  userProfile?: string
}>({
  userAccount: '',
  email: '',
  userPassword: '',
  userName: '',
  userAvatar: '',
  userRole: 'user',
  userProfile: '',
})

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const isHttpUrl = (value: string) => {
  if (!value) return true
  try {
    const url = new URL(value)
    return url.protocol === 'https:' || url.protocol === 'http:'
  } catch {
    return false
  }
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

// 打开时按模式回填：编辑态用行记录，新增态重置为默认
watch(
  () => [props.open, props.record],
  () => {
    if (!props.open) return
    if (props.isEdit && props.record) {
      form.userName = props.record.userName ?? ''
      form.userAvatar = props.record.userAvatar ?? ''
      form.userRole = props.record.userRole ?? 'user'
      form.userProfile = props.record.userProfile ?? ''
      form.userAccount = props.record.userAccount ?? ''
      form.email = props.record.email ?? ''
      form.userPassword = ''
    } else {
      form.userAccount = ''
      form.email = ''
      form.userPassword = ''
      form.userName = ''
      form.userAvatar = ''
      form.userRole = 'user'
      form.userProfile = ''
    }
  },
  { immediate: false },
)

const handleSubmit = async () => {
  const userAccount = (form.userAccount ?? '').trim()
  const email = (form.email ?? '').trim()
  const userAvatar = (form.userAvatar ?? '').trim()
  const userName = (form.userName ?? '').trim()
  const userProfile = (form.userProfile ?? '').trim()
  const userPassword = form.userPassword ?? ''
  if (userAccount.length < 3 || userAccount.length > 64) {
    message.warning('账号长度应为 3–64 个字符')
    return
  }
  if (!EMAIL_PATTERN.test(email)) {
    message.warning('请输入有效的邮箱地址')
    return
  }
  if (!props.isEdit && (userPassword.length < 8 || userPassword.length > 64)) {
    message.warning('初始密码长度应为 8–64 位')
    return
  }
  if (!isHttpUrl(userAvatar)) {
    message.warning('头像仅支持 http(s) 图片链接')
    return
  }
  if (props.isEdit && !props.record?.id) {
    message.error('未找到要更新的用户')
    return
  }
  submitting.value = true
  try {
    const payload = {
      userAccount,
      email,
      userName,
      userAvatar,
      userProfile,
      userRole: form.userRole,
    }
    const res = props.isEdit
      ? await updateUserUsingPost({ id: props.record?.id, ...payload })
      : await addUserUsingPost({
          ...payload,
          userPassword,
        })

    if (res.data.code === 0) {
      form.userPassword = ''
      message.success(props.isEdit ? '用户已更新' : '用户已创建')
      emit('success')
      emit('update:open', false)
    } else {
      message.error((props.isEdit ? '更新' : '创建') + '失败，' + (res.data.message ?? '未知错误'))
    }
  } catch (e: any) {
    message.error(e?.response?.data?.message || e?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleOpenChange = (open: boolean) => {
  if (!open) {
    form.userPassword = ''
  }
  emit('update:open', open)
}
</script>

<style scoped>
.user-edit-form {
  margin-top: 8px;
  margin-bottom: 0;
}

.security-notice {
  margin-bottom: 16px;
}

.user-edit-form :deep(.ant-form-item-label > label) {
  font-weight: 600;
  color: var(--pb-ink, #293247);
}

.field-hint {
  margin-top: 5px;
  color: var(--pb-muted, #71809a);
  font-size: 12px;
  line-height: 1.45;
}

.avatar-upload-field {
  display: flex;
  gap: 8px;
}

.avatar-upload-field > :first-child {
  flex: 1;
  min-width: 0;
}

.avatar-upload-field :deep(.ant-upload) {
  display: block;
}
</style>
