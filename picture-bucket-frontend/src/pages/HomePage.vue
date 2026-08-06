<template>
  <div id="homePage">
    <div class="hero-panel">
      <div class="hero-copy">
        <div class="hero-kicker">
          <span class="kicker-dot"></span>PUBLIC GALLERY
        </div>
        <h1 class="hero-title">探索<span class="grad-text">公共图库</span></h1>
        <p class="hero-desc">按分类与标签检索素材，快速找到你需要的图片。</p>
      </div>
      <div class="search-bar">
        <a-input-search
          v-model:value="searchParams.searchText"
          placeholder="搜索图片名称、简介或关键词"
          enter-button="搜索"
          size="large"
          @search="doSearch"
        />
      </div>
      <!-- 漂浮装饰 -->
      <div class="float-orb orb-a"></div>
      <div class="float-orb orb-b"></div>
      <div class="float-orb orb-c"></div>
    </div>

    <div class="filter-panel">
      <a-tabs v-model:active-key="selectedCategory" @change="doSearch">
        <a-tab-pane key="all" tab="全部" />
        <a-tab-pane v-for="category in categoryList" :tab="category" :key="category" />
      </a-tabs>
      <div class="tag-bar">
        <span class="tag-label">标签</span>
        <a-space :size="[0, 8]" wrap>
          <a-checkable-tag
            v-for="(tag, index) in tagList"
            :key="tag"
            v-model:checked="selectedTagList[index]"
            @change="doSearch"
          >
            {{ tag }}
          </a-checkable-tag>
        </a-space>
      </div>
    </div>

    <PictureList :dataList="dataList" :loading="loading" />
    <div class="pager">
      <a-pagination
        v-model:current="searchParams.current"
        v-model:pageSize="searchParams.pageSize"
        :total="total"
        @change="onPageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  listPictureTagCategoryUsingGet,
  listPictureVoByPageUsingPost,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import PictureList from '@/components/PictureList.vue' // 定义数据

// 定义数据
const dataList = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(true)

// 搜索条件
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
})

// 获取数据
const fetchData = async () => {
  loading.value = true
  // 转换搜索参数
  const params = {
    ...searchParams,
    tags: [] as string[],
  }
  if (selectedCategory.value !== 'all') {
    params.category = selectedCategory.value
  }
  // [true, false, false] => ['java']
  selectedTagList.value.forEach((useTag, index) => {
    if (useTag) {
      params.tags.push(tagList.value[index])
    }
  })
  const res = await listPictureVoByPageUsingPost(params)
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

// 页面加载时获取数据，请求一次
onMounted(() => {
  fetchData()
})

// 分页参数
const onPageChange = (page: number, pageSize: number) => {
  searchParams.current = page
  searchParams.pageSize = pageSize
  fetchData()
}

// 搜索
const doSearch = () => {
  // 重置搜索条件
  searchParams.current = 1
  fetchData()
}

// 标签和分类列表
const categoryList = ref<string[]>([])
const selectedCategory = ref<string>('all')
const tagList = ref<string[]>([])
const selectedTagList = ref<boolean[]>([])

/**
 * 获取标签和分类选项
 * @param values
 */
const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagList.value = res.data.data.tagList ?? []
    categoryList.value = res.data.data.categoryList ?? []
  } else {
    message.error('获取标签分类列表失败，' + res.data.message)
  }
}

onMounted(() => {
  getTagCategoryOptions()
})
</script>

<style scoped>
#homePage {
  margin-bottom: 16px;
}

/* ---------- Hero：白色流光玻璃面板 ---------- */
.hero-panel {
  position: relative;
  overflow: hidden;
  display: grid;
  gap: 22px;
  margin-bottom: 22px;
  padding: 48px 40px 40px;
  border-radius: 28px;
  background:
    radial-gradient(38rem 22rem at 90% -32%, rgba(56, 189, 248, 0.16), transparent 62%),
    radial-gradient(32rem 20rem at -10% 122%, rgba(108, 92, 231, 0.14), transparent 60%),
    rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(255, 255, 255, 0.95);
  box-shadow: var(--pb-shadow-lg);
  backdrop-filter: blur(20px);
}

/* 背景细网格 */
.hero-panel::before {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(108, 92, 231, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(108, 92, 231, 0.05) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: radial-gradient(42rem 26rem at 72% 28%, black, transparent 78%);
  content: '';
  pointer-events: none;
}

/* 漂浮彩色光球 */
.float-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(2px);
  pointer-events: none;
}

.orb-a {
  top: -40px;
  right: 72px;
  width: 150px;
  height: 150px;
  background: radial-gradient(circle at 32% 30%, #a5b4fc, #6c5ce7 68%, transparent 78%);
  opacity: 0.5;
  animation: drift-a 11s ease-in-out infinite;
}

.orb-b {
  top: 52px;
  right: 208px;
  width: 68px;
  height: 68px;
  background: radial-gradient(circle at 32% 30%, #7dd3fc, #38bdf8 68%, transparent 78%);
  opacity: 0.55;
  animation: drift-b 9s ease-in-out infinite;
}

.orb-c {
  bottom: -30px;
  right: 96px;
  width: 96px;
  height: 96px;
  background: radial-gradient(circle at 32% 30%, #6ee7b7, #00c9a7 68%, transparent 78%);
  opacity: 0.42;
  animation: drift-c 13s ease-in-out infinite;
}

@keyframes drift-a {
  0%,
  100% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(-26px, 22px) scale(1.08);
  }
}

@keyframes drift-b {
  0%,
  100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(18px, -16px);
  }
}

@keyframes drift-c {
  0%,
  100% {
    transform: translate(0, 0) rotate(0deg);
  }
  50% {
    transform: translate(-20px, -18px) rotate(10deg);
  }
}

.hero-copy,
.search-bar {
  position: relative;
  z-index: 1;
}

.hero-kicker {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 5px 14px;
  border-radius: 999px;
  background: rgba(108, 92, 231, 0.08);
  border: 1px solid rgba(108, 92, 231, 0.18);
  color: var(--pb-violet);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  margin-bottom: 18px;
}

.kicker-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--pb-teal);
  box-shadow: 0 0 10px rgba(0, 201, 167, 0.8);
  animation: pulse-dot 2.2s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(0.8);
  }
}

.hero-title {
  margin: 0 0 14px;
  font-size: clamp(38px, 5.4vw, 64px);
  font-weight: 800;
  line-height: 1.08;
  letter-spacing: -0.02em;
  color: var(--pb-ink);
}

.grad-text {
  margin-left: 0.35em;
  background: var(--pb-grad);
  background-size: 200% 200%;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: grad-flow 6s ease-in-out infinite;
}

@keyframes grad-flow {
  0%,
  100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

.hero-desc {
  margin: 0;
  color: var(--pb-muted);
  font-size: 15px;
}

#homePage .search-bar {
  max-width: 560px;
}

#homePage .search-bar :deep(.ant-input),
#homePage .search-bar :deep(.ant-input-affix-wrapper) {
  background: rgba(255, 255, 255, 0.9);
}

/* ---------- 筛选面板 ---------- */
.filter-panel {
  margin-bottom: 18px;
  padding: 8px 18px 14px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: var(--pb-shadow);
  backdrop-filter: blur(14px);
}

#homePage .tag-bar {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 4px;
}

.tag-label {
  flex-shrink: 0;
  margin-top: 6px;
  color: var(--pb-muted);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

#homePage .tag-bar :deep(.ant-tag) {
  background: #f2f3fa;
  border: 1px solid transparent;
  color: var(--pb-muted);
  transition: all 0.25s cubic-bezier(0.2, 0.7, 0.2, 1);
}

#homePage .tag-bar :deep(.ant-tag:hover) {
  color: var(--pb-violet);
  border-color: rgba(108, 92, 231, 0.3);
  transform: translateY(-2px);
}

#homePage .tag-bar :deep(.ant-tag-checkable-checked) {
  background: linear-gradient(120deg, rgba(108, 92, 231, 0.14), rgba(56, 189, 248, 0.12));
  border-color: rgba(108, 92, 231, 0.4);
  color: var(--pb-violet);
  box-shadow: 0 6px 14px rgba(108, 92, 231, 0.16);
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 22px;
}

@media (max-width: 768px) {
  .hero-panel {
    padding: 30px 20px 26px;
  }

  .orb-a {
    right: 20px;
    width: 100px;
    height: 100px;
  }

  .orb-b,
  .orb-c {
    display: none;
  }

  .hero-title {
    font-size: 32px;
  }
}
</style>
