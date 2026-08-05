<template>
  <div id="globalHeader">
    <a-row :wrap="false" align="middle">
      <a-col flex="220px">
        <router-link to="/">
          <div class="title-bar">
            <div class="logo-mark">
              <img class="logo" src="../assets/logo.png" alt="logo" />
            </div>
            <div class="brand">
              <div class="title">Picture Bucket</div>
              <div class="subtitle">Cloud Gallery</div>
            </div>
          </div>
        </router-link>
      </a-col>
      <a-col flex="auto">
        <a-menu
          v-model:selectedKeys="current"
          mode="horizontal"
          :items="items"
          class="nav-menu"
          @click="doMenuClick"
        />
      </a-col>
      <a-col flex="160px">
        <div class="user-login-status">
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown>
              <a class="user-chip" @click.prevent>
                <a-space>
                  <a-avatar :src="loginUserStore.loginUser.userAvatar" :size="32" />
                  <span class="user-name">{{ loginUserStore.loginUser.userName ?? '用户' }}</span>
                </a-space>
              </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <router-link to="/my_space">
                      <UserOutlined />
                      我的空间
                    </router-link>
                  </a-menu-item>
                  <a-menu-item @click="doLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
          <div v-else class="auth-actions">
            <a-button type="text" href="/user/login">登录</a-button>
            <a-button type="primary" href="/user/register">注册</a-button>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script lang="ts" setup>
import { computed, h, ref } from 'vue'
import { HomeOutlined, LogoutOutlined, UserOutlined } from '@ant-design/icons-vue'
import type { MenuProps } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { userLogoutUsingPost } from '@/api/userController.ts'

const loginUserStore = useLoginUserStore()

const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/add_picture',
    label: '创建图片',
    title: '创建图片',
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/pictureManage',
    label: '图片管理',
    title: '图片管理',
  },
  {
    key: '/admin/spaceManage',
    label: '空间管理',
    title: '空间管理',
  },
]

const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    if (String(menu?.key ?? '').startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

const items = computed(() => filterMenus(originItems))

const router = useRouter()
const current = ref<string[]>([])

router.afterEach((to) => {
  current.value = [to.path]
})

const doMenuClick = ({ key }: { key: string | number }) => {
  router.push({
    path: String(key),
  })
}

const doLogout = async () => {
  try {
    const res = await userLogoutUsingPost()
    if (res.data.code === 0) {
      loginUserStore.clearLoginUser()
      message.success('退出登录成功')
      await router.push('/user/login')
    } else {
      message.error('退出登录失败，' + res.data.message)
    }
  } catch (e: any) {
    // 接口异常时仍清理本地态，避免卡在“已登录”假象
    loginUserStore.clearLoginUser()
    message.error(e?.response?.data?.message || e?.message || '退出登录失败')
    await router.push('/user/login')
  }
}
</script>

<style scoped>
#globalHeader .title-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 72px;
}

.logo-mark {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: linear-gradient(145deg, #f1f2ff, #e2e8ff);
  box-shadow: 0 8px 18px rgba(109, 116, 207, 0.16);
  overflow: hidden;
}

.logo {
  height: 28px;
  width: 28px;
  object-fit: contain;
}

.brand {
  display: flex;
  flex-direction: column;
  justify-content: center;
  line-height: 1.15;
}

.title {
  color: #293247;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.subtitle {
  color: #8e9bb0;
  font-size: 11px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.nav-menu {
  background: transparent !important;
  line-height: 70px;
  border-bottom: none !important;
}

.nav-menu :deep(.ant-menu-item) {
  margin-inline: 4px;
  padding-inline: 14px;
  border-radius: 10px;
}

.nav-menu :deep(.ant-menu-item::after) {
  display: none;
}

.nav-menu :deep(.ant-menu-item-selected) {
  color: #5962bc !important;
  background: #eef0ff;
}

.user-login-status {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  height: 72px;
}

.user-chip {
  display: inline-flex;
  padding: 4px 10px 4px 4px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid #e5e9f2;
  transition: all 0.2s ease;
}

.user-chip:hover {
  border-color: #c7cdf6;
  background: #f3f4ff;
}

.user-name {
  max-width: 88px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #334155;
  font-size: 13px;
}

.auth-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

@media (max-width: 720px) {
  #globalHeader .title-bar {
    gap: 7px;
  }

  .brand .subtitle,
  .nav-menu {
    display: none;
  }
}
</style>
