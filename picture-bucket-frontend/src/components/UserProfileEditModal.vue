<template>
  <a-modal
    :open="open"
    title="编辑个人资料"
    :confirm-loading="submitting"
    ok-text="保存"
    cancel-text="取消"
    width="480px"
    :mask-closable="false"
    @ok="handleSubmit"
    @update:open="(v: boolean) => emit('update:open', v)"
  >
    <div class="profile-edit">
      <div class="avatar-row">
        <a-avatar :src="form.userAvatar" :size="72">
          {{ form.userName ? form.userName.charAt(0) : 'U' }}
        </a-avatar>
        <div class="avatar-field">
          <div class="avatar-field__label">头像</div>
          <a-input
            v-model:value="form.userAvatar"
            placeholder="头像图片链接（可选，仅支持 http(s)）"
            :maxlength="500"
            autocomplete="off"
            allow-clear
          />
        </div>
      </div>

      <a-form layout="vertical" class="profile-form">
        <a-form-item label="昵称" required>
          <a-input
            v-model:value="form.userName"
            placeholder="给自己起个名字"
            :maxlength="20"
            show-count
          />
        </a-form-item>
        <a-form-item label="个人简介">
          <a-textarea
            v-model:value="form.userProfile"
            placeholder="介绍一下自己吧 ~"
            :rows="3"
            :maxlength="128"
            show-count
          />
        </a-form-item>
      </a-form>
    </div>
  </a-modal>
</template>

<script lang="ts" setup>
import { reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { editUserUsingPost } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{
  'update:open': [value: boolean]
  success: []
}>()

const loginUserStore = useLoginUserStore()
const submitting = ref(false)

const form = reactive<API.UserEditRequest>({
  userName: '',
  userAvatar: '',
  userProfile: '',
})

const isHttpUrl = (value: string) => {
  if (!value) return true
  try {
    const url = new URL(value)
    return url.protocol === 'https:' || url.protocol === 'http:'
  } catch {
    return false
  }
}

// 每次打开时，用当前登录用户的最新资料回填表单
watch(
  () => props.open,
  (open) => {
    if (!open) return
    const u = loginUserStore.loginUser
    form.userName = u.userName ?? ''
    form.userAvatar = u.userAvatar ?? ''
    form.userProfile = u.userProfile ?? ''
  },
)

const handleSubmit = async () => {
  const userName = (form.userName ?? '').trim()
  const userAvatar = (form.userAvatar ?? '').trim()
  const userProfile = (form.userProfile ?? '').trim()
  if (!userName) {
    message.warning('昵称不能为空')
    return
  }
  if (!isHttpUrl(userAvatar)) {
    message.warning('头像仅支持 http(s) 图片链接')
    return
  }
  submitting.value = true
  try {
    const res = await editUserUsingPost({
      userName,
      userAvatar,
      userProfile,
    })
    if (res.data.code === 0) {
      message.success('资料已更新')
      // 同步刷新顶部头像/昵称
      await loginUserStore.fetchLoginUser()
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

.profile-form {
  margin-bottom: 0;
}

.profile-form :deep(.ant-form-item-label > label) {
  font-weight: 600;
  color: var(--pb-ink, #293247);
}
</style>
