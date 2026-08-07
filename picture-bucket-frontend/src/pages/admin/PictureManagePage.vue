<template>
  <div id="pictureManagePage">
    <AdminPageHeader title="图片管理" subtitle="审核、编辑与维护平台全部图片">
      <template #actions>
        <a-button type="primary" href="/add_picture" target="_blank">+ 创建图片</a-button>
        <a-button type="primary" href="/add_picture/batch" target="_blank" ghost>
          + 批量创建图片
        </a-button>
      </template>
    </AdminPageHeader>

    <a-card class="admin-panel" :bordered="false">
      <a-form layout="inline" :model="searchParams" @finish="doSearch">
        <a-form-item label="关键词">
          <a-input
            v-model:value="searchParams.searchText"
            placeholder="从名称和简介搜索"
            allow-clear
          />
        </a-form-item>
        <a-form-item label="类型">
          <a-input v-model:value="searchParams.category" placeholder="请输入类型" allow-clear />
        </a-form-item>
        <a-form-item label="标签">
          <a-select
            v-model:value="searchParams.tags"
            mode="tags"
            placeholder="请输入标签"
            style="min-width: 180px"
            allow-clear
          />
        </a-form-item>
        <a-form-item name="reviewStatus" label="审核状态">
          <a-select
            v-model:value="searchParams.reviewStatus"
            style="min-width: 180px"
            placeholder="请选择审核状态"
            :options="PIC_REVIEW_STATUS_OPTIONS"
            allow-clear
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">搜索</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card class="admin-table-card" :bordered="false">
      <a-table
        class="compact-admin-table"
        :columns="columns"
        :data-source="dataList"
        :pagination="pagination"
        :row-key="(record: API.Picture) => record.id ?? ''"
        :scroll="{ x: 1050 }"
        size="small"
        @change="doTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'url'">
            <a-image class="picture-thumb" :src="record.url" :width="64" :height="64" />
          </template>
          <template v-else-if="column.key === 'asset'">
            <div class="asset-cell">
              <a-typography-text
                class="asset-name"
                :ellipsis="{ tooltip: record.name || '未命名图片' }"
              >
                {{ record.name || '未命名图片' }}
              </a-typography-text>
              <div class="asset-tags">
                <a-tag v-if="record.category" color="blue">{{ record.category }}</a-tag>
                <a-tag v-for="tag in getTags(record.tags).slice(0, 2)" :key="tag">
                  {{ tag }}
                </a-tag>
                <a-tag v-if="getTags(record.tags).length > 2">
                  +{{ getTags(record.tags).length - 2 }}
                </a-tag>
              </div>
            </div>
          </template>
          <template v-else-if="column.key === 'owner'">
            <div class="owner-cell">
              <span>用户 {{ record.userId || '—' }}</span>
              <span>{{ record.spaceId ? `空间 ${record.spaceId}` : '公共图库' }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'review'">
            <div class="review-cell">
              <a-badge
                :status="getReviewBadgeStatus(record.reviewStatus)"
                :text="getReviewStatusText(record.reviewStatus)"
              />
              <span v-if="record.reviewMessage" :title="record.reviewMessage">
                {{ record.reviewMessage }}
              </span>
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ formatDate(record.createTime) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space class="action-cell" wrap>
              <a-button type="link" size="small" @click="openDetail(record)">详细</a-button>
              <a-button
                v-if="record.reviewStatus !== PIC_REVIEW_STATUS_ENUM.PASS"
                type="link"
                size="small"
                @click="handleReview(record, PIC_REVIEW_STATUS_ENUM.PASS)"
              >
                通过
              </a-button>
              <a-button
                v-if="record.reviewStatus !== PIC_REVIEW_STATUS_ENUM.REJECT"
                type="link"
                size="small"
                danger
                @click="handleReview(record, PIC_REVIEW_STATUS_ENUM.REJECT)"
              >
                拒绝
              </a-button>
              <a-button type="link" size="small" :href="`/add_picture?id=${record.id}`" target="_blank">
                编辑
              </a-button>
              <a-button type="link" size="small" danger @click="doDelete(record.id)">删除</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="detailDrawerOpen" title="图片详细信息" width="520">
      <template #extra>
        <a-button v-if="currentPicture.id" type="link" :href="`/picture/${currentPicture.id}`" target="_blank">
          打开详情页
        </a-button>
      </template>
      <div class="drawer-preview">
        <a-image :src="currentPicture.url" :alt="currentPicture.name || '图片预览'" />
      </div>
      <a-descriptions :column="1" bordered size="small">
        <a-descriptions-item label="图片 ID">
          <a-typography-text copyable>{{ currentPicture.id || '—' }}</a-typography-text>
        </a-descriptions-item>
        <a-descriptions-item label="名称">{{ currentPicture.name || '未命名图片' }}</a-descriptions-item>
        <a-descriptions-item label="简介">{{ currentPicture.introduction || '—' }}</a-descriptions-item>
        <a-descriptions-item label="分类">{{ currentPicture.category || '默认' }}</a-descriptions-item>
        <a-descriptions-item label="标签">
          <a-space wrap>
            <a-tag v-for="tag in getTags(currentPicture.tags)" :key="tag">{{ tag }}</a-tag>
            <span v-if="getTags(currentPicture.tags).length === 0">—</span>
          </a-space>
        </a-descriptions-item>
        <a-descriptions-item label="图片信息">
          {{ currentPicture.picFormat || '—' }} · {{ currentPicture.picWidth || '—' }} ×
          {{ currentPicture.picHeight || '—' }} · {{ formatSize(currentPicture.picSize) }}
        </a-descriptions-item>
        <a-descriptions-item label="宽高比">{{ currentPicture.picScale || '—' }}</a-descriptions-item>
        <a-descriptions-item label="主色调">
          <a-space>
            <span>{{ currentPicture.picColor || '—' }}</span>
            <span
              v-if="currentPicture.picColor"
              class="color-dot"
              :style="{ backgroundColor: toHexColor(currentPicture.picColor) }"
            />
          </a-space>
        </a-descriptions-item>
        <a-descriptions-item label="归属">
          用户 {{ currentPicture.userId || '—' }} /
          {{ currentPicture.spaceId ? `空间 ${currentPicture.spaceId}` : '公共图库' }}
        </a-descriptions-item>
        <a-descriptions-item label="审核">
          {{ getReviewStatusText(currentPicture.reviewStatus) }}
          {{ currentPicture.reviewMessage ? `：${currentPicture.reviewMessage}` : '' }}
        </a-descriptions-item>
        <a-descriptions-item label="审核信息">
          审核人 {{ currentPicture.reviewerId || '—' }} / {{ formatDate(currentPicture.reviewTime) }}
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ formatDate(currentPicture.createTime) }}</a-descriptions-item>
        <a-descriptions-item label="编辑时间">{{ formatDate(currentPicture.editTime) }}</a-descriptions-item>
        <a-descriptions-item label="原始地址">
          <a-typography-paragraph class="url-value" copyable>
            {{ currentPicture.url || '—' }}
          </a-typography-paragraph>
        </a-descriptions-item>
      </a-descriptions>
    </a-drawer>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  deletePictureUsingPost,
  doPictureReviewUsingPost,
  listPictureByPageUsingPost,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import {
  PIC_REVIEW_STATUS_ENUM,
  PIC_REVIEW_STATUS_MAP,
  PIC_REVIEW_STATUS_OPTIONS,
} from '../../constants/picture.ts'
import dayjs from 'dayjs'
import AdminPageHeader from '@/components/AdminPageHeader.vue'
import { formatSize, toHexColor } from '@/utils'

const columns = [
  {
    title: '图片',
    dataIndex: 'url',
    width: 88,
  },
  {
    title: '图片名称',
    key: 'asset',
    width: 240,
  },
  {
    title: '归属',
    key: 'owner',
    width: 168,
  },
  {
    title: '审核状态',
    key: 'review',
    width: 190,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 154,
  },
  {
    title: '操作',
    key: 'action',
    width: 210,
    fixed: 'right',
  },
]

const dataList = ref<API.Picture[]>([])
const total = ref(0)
const detailDrawerOpen = ref(false)
const currentPicture = ref<API.Picture>({})

const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'descend',
})

const fetchData = async () => {
  const res = await listPictureByPageUsingPost({
    ...searchParams,
    nullSpaceId: true,
  })
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
}

onMounted(() => {
  fetchData()
})

const pagination = computed(() => {
  return {
    current: searchParams.current,
    pageSize: searchParams.pageSize,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: number) => `共 ${total} 条`,
  }
})

const doTableChange = (page: any) => {
  searchParams.current = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

const doSearch = () => {
  searchParams.current = 1
  fetchData()
}

const getTags = (tags?: string): string[] => {
  if (!tags) {
    return []
  }
  try {
    const parsedTags = JSON.parse(tags)
    return Array.isArray(parsedTags) ? parsedTags.filter((tag): tag is string => typeof tag === 'string') : []
  } catch {
    return []
  }
}

const getReviewBadgeStatus = (reviewStatus?: number) => {
  const status = Number(reviewStatus)
  if (status === PIC_REVIEW_STATUS_ENUM.PASS) return 'success'
  if (status === PIC_REVIEW_STATUS_ENUM.REJECT) return 'error'
  return 'processing'
}

const getReviewStatusText = (reviewStatus?: number) => {
  return PIC_REVIEW_STATUS_MAP[Number(reviewStatus)] || PIC_REVIEW_STATUS_MAP[PIC_REVIEW_STATUS_ENUM.REVIEWING]
}

const formatDate = (date?: string) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '—'
}

const openDetail = (record: API.Picture) => {
  currentPicture.value = record
  detailDrawerOpen.value = true
}

const doDelete = async (id: string | number | undefined) => {
  if (!id) {
    return
  }
  const res = await deletePictureUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    fetchData()
  } else {
    message.error('删除失败')
  }
}

const handleReview = async (record: API.Picture, reviewStatus: number) => {
  const reviewMessage =
    reviewStatus === PIC_REVIEW_STATUS_ENUM.PASS ? '管理员操作通过' : '管理员操作拒绝'
  const res = await doPictureReviewUsingPost({
    id: record.id,
    reviewStatus,
    reviewMessage,
  })
  if (res.data.code === 0) {
    message.success('审核操作成功')
    fetchData()
  } else {
    message.error('审核操作失败，' + res.data.message)
  }
}
</script>

<style scoped>
.admin-table-card {
  margin-top: 16px;
}

.compact-admin-table :deep(.ant-table) {
  table-layout: fixed;
}

.compact-admin-table :deep(.ant-table-thead > tr > th) {
  padding: 9px 10px;
  color: var(--pb-muted);
  font-size: 12px;
  font-weight: 700;
}

.compact-admin-table :deep(.ant-table-tbody > tr > td) {
  padding: 10px;
  color: #4e536b;
  font-size: 13px;
  line-height: 1.5;
  vertical-align: middle;
}

.picture-thumb :deep(img) {
  border-radius: 10px;
  object-fit: cover;
}

.asset-cell,
.owner-cell,
.review-cell {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.asset-name {
  max-width: 100%;
  color: var(--pb-ink, #262a3b);
  font-weight: 700;
}

.asset-tags {
  display: flex;
  align-items: center;
  gap: 4px;
  overflow: hidden;
  white-space: nowrap;
}

.asset-tags :deep(.ant-tag) {
  margin-inline-end: 0;
}

.owner-cell,
.review-cell span {
  overflow: hidden;
  color: var(--pb-muted, #71809a);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-cell {
  column-gap: 2px;
  row-gap: 0;
}

.drawer-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 180px;
  margin-bottom: 16px;
  overflow: hidden;
  border-radius: 12px;
  background: linear-gradient(135deg, #f1f4ff, #f7fbff);
}

.drawer-preview :deep(.ant-image),
.drawer-preview :deep(img) {
  max-width: 100%;
  max-height: 260px;
  object-fit: contain;
}

.color-dot {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 50%;
}

.url-value {
  max-width: 300px;
  margin: 0;
  overflow-wrap: anywhere;
}
</style>
