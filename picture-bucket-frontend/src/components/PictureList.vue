<template>
  <div class="picture-list">
    <!-- 图片列表 -->
    <a-list
      :grid="{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 4, xl: 5, xxl: 6 }"
      :data-source="dataList"
      :loading="loading"
    >
      <template #renderItem="{ item: picture }">
        <a-list-item style="padding: 0">
          <!-- 单张图片 -->
          <a-card class="gallery-card" hoverable @click="doClickPicture(picture)">
            <template #cover>
              <div class="gallery-cover">
                <img
                  :alt="picture.name"
                  :src="picture.thumbnailUrl ?? picture.url"
                />
              </div>
            </template>
            <a-card-meta :title="picture.name">
              <template #description>
                <a-flex>
                  <a-tag color="green">
                    {{ picture.category ?? '默认' }}
                  </a-tag>
                  <a-tag v-for="tag in picture.tags" :key="tag">
                    {{ tag }}
                  </a-tag>
                </a-flex>
              </template>
            </a-card-meta>
            <template v-if="showOp" #actions>
              <ShareAltOutlined @click="(e) => doShare(picture, e)" />
              <SearchOutlined @click="(e) => doSearch(picture, e)" />
              <EditOutlined v-if="canEdit" @click="(e) => doEdit(picture, e)" />
              <DeleteOutlined v-if="canDelete" @click="(e) => doDelete(picture, e)" />
            </template>
          </a-card>
        </a-list-item>
      </template>
    </a-list>
    <ShareModal ref="shareModalRef" :link="shareLink" />
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import {
  DeleteOutlined,
  EditOutlined,
  SearchOutlined,
  ShareAltOutlined,
} from '@ant-design/icons-vue'
import { deletePictureUsingPost } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import ShareModal from '@/components/ShareModal.vue'
import { ref } from 'vue'

interface Props {
  dataList?: API.PictureVO[]
  loading?: boolean
  showOp?: boolean
  canEdit?: boolean
  canDelete?: boolean
  onReload?: () => void
}

const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
  loading: false,
  showOp: false,
  canEdit: false,
  canDelete: false,
})

const router = useRouter()
// 跳转至图片详情页
const doClickPicture = (picture: API.PictureVO) => {
  router.push({
    path: `/picture/${picture.id}`,
  })
}

// 搜索
const doSearch = (picture: API.PictureVO, e: Event) => {
  // 阻止冒泡
  e.stopPropagation()
  // 打开新的页面
  window.open(`/search_picture?pictureId=${picture.id}`)
}

// 编辑
const doEdit = (picture: API.PictureVO, e: Event) => {
  // 阻止冒泡
  e.stopPropagation()
  // 跳转时一定要携带 spaceId
  router.push({
    path: '/add_picture',
    query: {
      id: picture.id,
      spaceId: picture.spaceId,
    },
  })
}

// 删除数据
const doDelete = async (picture: API.PictureVO, e: Event) => {
  // 阻止冒泡
  e.stopPropagation()
  const id = picture.id
  if (!id) {
    return
  }
  const res = await deletePictureUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    props.onReload?.()
  } else {
    message.error('删除失败')
  }
}

// ----- 分享操作 ----
const shareModalRef = ref()
// 分享链接
const shareLink = ref<string>()
// 分享
const doShare = (picture: API.PictureVO, e: Event) => {
  // 阻止冒泡
  e.stopPropagation()
  shareLink.value = `${window.location.protocol}//${window.location.host}/picture/${picture.id}`
  if (shareModalRef.value) {
    shareModalRef.value.openModal()
  }
}
</script>

<style scoped>
.gallery-card {
  border-color: rgba(232, 234, 243, 0.95);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 10px 26px rgba(93, 102, 160, 0.07);
}

.gallery-card:hover {
  border-color: rgba(108, 92, 231, 0.35);
  box-shadow: 0 22px 44px rgba(93, 102, 160, 0.16);
  transform: translateY(-5px);
}

.gallery-cover {
  position: relative;
  height: 190px;
  overflow: hidden;
  background: linear-gradient(140deg, #eef0fa, #e6f4fb);
}

/* 悬停时顶部扫过的高光 */
.gallery-cover::after {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    115deg,
    transparent 30%,
    rgba(255, 255, 255, 0.45) 48%,
    transparent 62%
  );
  opacity: 0;
  transform: translateX(-60%);
  transition: opacity 0.35s ease, transform 0.6s ease;
  content: '';
}

.gallery-card:hover .gallery-cover::after {
  opacity: 1;
  transform: translateX(60%);
}

.gallery-cover img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.55s cubic-bezier(0.2, 0.7, 0.2, 1);
}

.gallery-card:hover .gallery-cover img {
  transform: scale(1.07);
}

.gallery-card :deep(.ant-card-body) {
  padding: 14px 16px 16px;
}

.gallery-card :deep(.ant-card-meta-title) {
  color: var(--pb-ink);
  font-size: 14px;
  font-weight: 700;
}

.gallery-card :deep(.ant-card-meta-description) {
  margin-top: 10px;
}

.gallery-card :deep(.ant-card-actions) {
  background: rgba(248, 249, 253, 0.8);
  border-top-color: var(--pb-line);
}

.gallery-card :deep(.ant-card-actions > li) {
  color: var(--pb-muted);
  transition: color 0.2s ease, transform 0.2s ease;
}

.gallery-card :deep(.ant-card-actions > li:hover) {
  color: var(--pb-violet);
  transform: translateY(-2px);
}

.gallery-card :deep(.ant-card-actions > li + li) {
  border-inline-start-color: var(--pb-line);
}
</style>
