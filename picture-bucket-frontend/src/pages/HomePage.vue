<template>
  <div id="homePage">
    <div class="hero-panel">
      <div class="hero-copy">
        <div class="hero-kicker">Public Gallery</div>
        <h1 class="hero-title">探索公共图库</h1>
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

.hero-panel {
  position: relative;
  overflow: hidden;
  display: grid;
  gap: 18px;
  margin-bottom: 18px;
  padding: 28px 28px 24px;
  border-radius: 24px;
  background:
    linear-gradient(122deg, rgba(227, 230, 255, 0.85), rgba(238, 249, 245, 0.76)),
    rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(229, 233, 242, 0.94);
  box-shadow: 0 18px 38px rgba(71, 83, 123, 0.07);
}

.hero-panel::after {
  position: absolute;
  top: -58px;
  right: 50px;
  width: 180px;
  height: 180px;
  border: 18px solid rgba(255, 255, 255, 0.48);
  border-radius: 42px;
  box-shadow: -36px 40px 0 rgba(209, 231, 224, 0.36);
  content: '';
  transform: rotate(16deg);
}

.hero-copy,
.search-bar {
  position: relative;
  z-index: 1;
}

.hero-kicker {
  display: inline-flex;
  color: #6570c9;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin-bottom: 8px;
}

.hero-title {
  margin: 0 0 8px;
  font-size: clamp(28px, 3vw, 38px);
  font-weight: 800;
  letter-spacing: -0.03em;
  color: #293247;
}

.hero-desc {
  margin: 0;
  color: #71809a;
  font-size: 14px;
}

#homePage .search-bar {
  max-width: 560px;
}

.filter-panel {
  margin-bottom: 16px;
  padding: 8px 16px 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(229, 233, 242, 0.92);
}

#homePage .tag-bar {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 4px;
}

.tag-label {
  flex-shrink: 0;
  margin-top: 4px;
  color: #71809a;
  font-size: 13px;
  font-weight: 500;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .hero-panel {
    padding: 20px 16px;
  }

  .hero-title {
    font-size: 24px;
  }
}
</style>
