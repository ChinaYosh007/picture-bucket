<template>
  <div id="userManagePage">
    <AdminPageHeader title="用户管理" subtitle="查看、新增与维护平台用户">
      <template #actions>
        <a-button type="primary" @click="openAddModal">
          <template #icon><UserAddOutlined /></template>
          添加用户
        </a-button>
      </template>
    </AdminPageHeader>

    <!-- 搜索表单 -->
    <a-card class="admin-panel" :bordered="false">
      <a-form layout="inline" :model="searchParams" @finish="doSearch">
        <a-form-item label="账号">
          <a-input v-model:value="searchParams.userAccount" placeholder="输入账号" allow-clear />
        </a-form-item>
        <a-form-item label="用户名">
          <a-input v-model:value="searchParams.userName" placeholder="输入用户名" allow-clear />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">搜索</a-button>
        </a-form-item>
      </a-form>
    </a-card>
    <div style="margin-bottom: 16px" />
    <!-- 表格 -->
    <a-table
      class="compact-admin-table"
        :columns="columns"
        :data-source="dataList"
        :loading="loading"
      :pagination="pagination"
      :row-key="(record: API.UserVO) => record.id ?? ''"
      :scroll="{ x: 1216 }"
      size="small"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'id'">
          <span class="id-cell">{{ record.id }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'userAvatar'">
          <a-avatar :src="record.userAvatar" :size="32">
            {{ record.userName?.slice(0, 1) || 'U' }}
          </a-avatar>
        </template>
        <template v-else-if="column.dataIndex === 'userRole'">
          <div v-if="record.userRole === 'admin'">
            <a-tag color="green">管理员</a-tag>
          </div>
          <div v-else>
            <a-tag color="blue">普通用户</a-tag>
          </div>
        </template>
        <template v-else-if="column.dataIndex === 'userProfile'">
          <a-typography-paragraph :ellipsis="{ rows: 2 }" class="profile-cell">
            {{ record.userProfile || '—' }}
          </a-typography-paragraph>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ record.createTime ? dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') : '—' }}
        </template>
        <template v-else-if="column.key === 'action'">
          <div class="action-cell">
            <a-button type="link" size="small" :loading="editLoading" @click="openEditModal(record)">
              编辑
            </a-button>
            <a-popconfirm
              v-if="!isCurrentUser(record.id)"
              title="确认删除该用户？此操作无法撤销。"
              ok-text="删除"
              cancel-text="取消"
              @confirm="doDelete(record.id)"
            >
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
            <span v-else class="current-user-label">当前账号</span>
          </div>
        </template>
      </template>
    </a-table>

    <UserEditModal
      v-model:open="editModalOpen"
      :is-edit="editModalIsEdit"
      :record="editModalRecord"
      @success="fetchData"
    />
  </div>
</template>
<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  deleteUserUsingPost,
  getUserByIdUsingGet,
  listUserVoByPageUsingPost,
} from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import UserEditModal from '@/components/UserEditModal.vue'
import AdminPageHeader from '@/components/AdminPageHeader.vue'
import { UserAddOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'

const columns = [
  {
    title: 'id',
    dataIndex: 'id',
    width: 172,
    ellipsis: true,
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
    width: 180,
    ellipsis: true,
  },
  {
    title: '用户名',
    dataIndex: 'userName',
    width: 130,
    ellipsis: true,
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
    width: 72,
  },
  {
    title: '简介',
    dataIndex: 'userProfile',
    width: 240,
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
    width: 110,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 164,
  },
  {
    title: '操作',
    key: 'action',
    width: 148,
    fixed: 'right',
  },
]

// 定义数据
const dataList = ref<API.UserVO[]>([])
const total = ref(0)
const loading = ref(false)
const editLoading = ref(false)
const loginUserStore = useLoginUserStore()

// 搜索条件
const searchParams = reactive<API.UserQueryRequest>({
  current: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'ascend',
})

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await listUserVoByPageUsingPost({
      ...searchParams,
    })
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data.records ?? []
      total.value = res.data.data.total ?? 0
    } else {
      message.error('获取用户失败：' + (res.data.message ?? '未知错误'))
    }
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '获取用户失败')
  } finally {
    loading.value = false
  }
}

// 页面加载时获取数据，请求一次
onMounted(() => {
  fetchData()
})

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.current,
    pageSize: searchParams.pageSize,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: number) => `共 ${total} 条`,
  }
})

// 表格变化之后，重新获取数据
const doTableChange = (page: any) => {
  searchParams.current = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 搜索数据
const doSearch = () => {
  // 重置页码
  searchParams.current = 1
  fetchData()
}

// 删除数据
const isCurrentUser = (id?: number) => id != null && id === loginUserStore.loginUser.id

const doDelete = async (id?: number) => {
  if (!id || isCurrentUser(id)) {
    message.warning('不能删除当前登录账号')
    return
  }
  try {
    const res = await deleteUserUsingPost({ id })
    if (res.data.code === 0) {
      message.success('用户已删除')
      if (dataList.value.length === 1 && (searchParams.current ?? 1) > 1) {
        searchParams.current = (searchParams.current ?? 1) - 1
      }
      await fetchData()
    } else {
      message.error('删除失败：' + (res.data.message ?? '未知错误'))
    }
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '删除失败')
  }
}

// 新增 / 编辑用户弹窗
const editModalOpen = ref(false)
const editModalIsEdit = ref(false)
const editModalRecord = ref<API.User | null>(null)

const openAddModal = () => {
  editModalIsEdit.value = false
  editModalRecord.value = null
  editModalOpen.value = true
}

const openEditModal = async (record: API.UserVO) => {
  if (!record.id) return

  editLoading.value = true
  try {
    // 列表只使用脱敏视图；管理员主动编辑单条记录时才读取必要字段。
    const res = await getUserByIdUsingGet({ id: record.id })
    if (res.data.code !== 0 || !res.data.data) {
      message.error('读取用户失败：' + (res.data.message ?? '未知错误'))
      return
    }
    const user = res.data.data
    // 明确白名单，避免将接口中可能出现的其他敏感字段保留在前端状态中。
    editModalRecord.value = {
      id: user.id,
      email: user.email,
      userAccount: user.userAccount,
      userAvatar: user.userAvatar,
      userName: user.userName,
      userProfile: user.userProfile,
      userRole: user.userRole,
    }
    editModalIsEdit.value = true
    editModalOpen.value = true
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '读取用户失败')
  } finally {
    editLoading.value = false
  }
}

</script>

<style scoped>
.compact-admin-table :deep(.ant-table) {
  table-layout: fixed;
}

.compact-admin-table :deep(.ant-table-thead > tr > th) {
  padding: 10px 12px;
  color: var(--pb-muted);
  font-size: 12px;
  font-weight: 700;
}

.compact-admin-table :deep(.ant-table-tbody > tr > td) {
  padding: 10px 12px;
  vertical-align: middle;
}

.id-cell {
  display: block;
  overflow: hidden;
  color: var(--pb-muted);
  font-family: 'Aptos Mono', Consolas, monospace;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-cell {
  margin: 0;
  color: var(--pb-muted);
  font-size: 13px;
}

.action-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 24px;
  white-space: nowrap;
}

.action-cell :deep(.ant-btn-link) {
  height: 24px;
  padding: 0 4px;
  font-weight: 600;
}

.current-user-label {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border: 1px solid rgba(108, 92, 231, 0.18);
  border-radius: 999px;
  color: var(--pb-violet);
  background: rgba(108, 92, 231, 0.08);
  font-size: 12px;
  font-weight: 600;
}
</style>
