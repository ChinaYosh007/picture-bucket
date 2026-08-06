<template>
  <div id="globalSider">
    <a-layout-sider
      v-if="loginUserStore.loginUser.id"
      width="200"
      breakpoint="lg"
      collapsed-width="0"
    >
      <a-menu
        v-model:selectedKeys="current"
        mode="inline"
        :items="menuItems"
        @click="doMenuClick"
      />
    </a-layout-sider>
  </div>
</template>
<script lang="ts" setup>
import { computed, h, ref, watchEffect } from 'vue'
import { PictureOutlined, TeamOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { SPACE_TYPE_ENUM } from '@/constants/space.ts'
import { listMyTeamSpaceUsingPost } from '@/api/spaceUserController.ts'
import { message } from 'ant-design-vue'

const loginUserStore = useLoginUserStore()

// 固定的菜单列表
const fixedMenuItems = [
  {
    key: '/',
    icon: () => h(PictureOutlined),
    label: '公共图库',
  },
  {
    key: '/my_space',
    label: '我的空间',
    icon: () => h(UserOutlined),
  },
  {
    key: '/add_space?type=' + SPACE_TYPE_ENUM.TEAM,
    label: '创建团队',
    icon: () => h(TeamOutlined),
  },
]

const teamSpaceList = ref<API.SpaceUserVO[]>([])
const menuItems = computed(() => {
  // 如果用户没有团队空间，则只展示固定菜单
  if (teamSpaceList.value.length < 1) {
    return fixedMenuItems
  }
  // 如果用户有团队空间，则展示固定菜单和团队空间菜单
  // 展示团队空间分组
  const teamSpaceSubMenus = teamSpaceList.value.map((spaceUser) => {
    const space = spaceUser.space
    return {
      key: '/space/' + spaceUser.spaceId,
      label: space?.spaceName,
    }
  })
  const teamSpaceMenuGroup = {
    type: 'group',
    label: '我的团队',
    key: 'teamSpace',
    children: teamSpaceSubMenus,
  }
  return [...fixedMenuItems, teamSpaceMenuGroup]
})

// 加载团队空间列表
const fetchTeamSpaceList = async () => {
  const res = await listMyTeamSpaceUsingPost()
  if (res.data.code === 0 && res.data.data) {
    teamSpaceList.value = res.data.data
  } else {
    message.error('加载我的团队空间失败，' + res.data.message)
  }
}

/**
 * 监听变量，改变时触发数据的重新加载
 */
watchEffect(() => {
  // 登录才加载
  if (loginUserStore.loginUser.id) {
    fetchTeamSpaceList()
  }
})

const router = useRouter()
// 当前要高亮的菜单项
const current = ref<string[]>([])
// 监听路由变化，更新高亮菜单项
router.afterEach((to, from, next) => {
  current.value = [to.path]
})

// 路由跳转事件
const doMenuClick = ({ key }: { key: string | number }) => {
  router.push(String(key))
}
</script>

<style scoped>
#globalSider {
  padding: 24px 0 24px 18px;
}

#globalSider .ant-layout-sider {
  background: rgba(255, 255, 255, 0.72) !important;
  border: 1px solid rgba(232, 234, 243, 0.9);
  border-radius: 22px;
  margin: 0;
  min-height: calc(100vh - 174px);
  box-shadow: 0 16px 40px rgba(93, 102, 160, 0.08);
  overflow: hidden;
  backdrop-filter: blur(16px);
}

#globalSider :deep(.ant-menu) {
  padding: 10px 6px;
  background: transparent;
}

#globalSider :deep(.ant-menu-item-group-title) {
  color: var(--pb-faint);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

#globalSider :deep(.ant-menu-item) {
  height: 44px;
  margin-inline: 8px;
  width: calc(100% - 16px);
  border-radius: 13px;
  line-height: 44px;
  color: var(--pb-muted);
  font-size: 13px;
  font-weight: 600;
  transition: color 0.2s ease, background 0.25s ease, transform 0.2s ease;
}

#globalSider :deep(.ant-menu-item:hover) {
  color: var(--pb-ink);
  transform: translateX(3px);
}

#globalSider :deep(.ant-menu-item-selected) {
  color: var(--pb-violet);
  background: linear-gradient(120deg, rgba(108, 92, 231, 0.12), rgba(56, 189, 248, 0.09));
}
</style>
