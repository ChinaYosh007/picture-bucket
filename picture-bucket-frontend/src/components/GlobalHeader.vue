<template>
  <div id="globalHeader">
    <a-row :wrap="false" align="middle">
      <a-col flex="240px">
        <router-link to="/">
          <div class="title-bar">
            <div class="logo-mark">
              <img class="logo" src="../assets/logo.png" alt="logo" />
            </div>
            <div class="brand">
              <div class="title">PICTURE BUCKET</div>
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
                  <a-avatar :src="loginUserStore.loginUser.userAvatar" :size="30" />
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
                  <a-menu-item @click="profileModalOpen = true">
                    <ProfileOutlined />
                    个人资料
                  </a-menu-item>
                  <a-menu-item @click="doLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>

            <UserProfileEditModal v-model:open="profileModalOpen" />
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
import { HomeOutlined, LogoutOutlined, ProfileOutlined, UserOutlined } from '@ant-design/icons-vue'
import type { MenuProps } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { userLogoutUsingPost } from '@/api/userController.ts'
import UserProfileEditModal from '@/components/UserProfileEditModal.vue'

const loginUserStore = useLoginUserStore()
const profileModalOpen = ref(false)

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
  gap: 12px;
  height: 68px;
}

.logo-mark {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: linear-gradient(145deg, #eef0ff, #e0f7ff);
  box-shadow: 0 8px 20px rgba(108, 92, 231, 0.2);
  overflow: hidden;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.3s ease;
}

.title-bar:hover .logo-mark {
  transform: rotate(-8deg) scale(1.06);
  box-shadow: 0 12px 26px rgba(108, 92, 231, 0.3);
}

.logo {
  height: 26px;
  width: 26px;
  object-fit: contain;
}

.brand {
  display: flex;
  flex-direction: column;
  justify-content: center;
  line-height: 1.2;
}

.title {
  font-size: 16px;
  font-weight: 800;
  letter-spacing: -0.01em;
  background: var(--pb-grad);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.subtitle {
  color: var(--pb-faint);
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.14em;
}

.nav-menu {
  background: transparent !important;
  line-height: 66px;
  border-bottom: none !important;
}

.nav-menu :deep(.ant-menu-item) {
  margin-inline: 3px;
  padding-inline: 14px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
  color: var(--pb-muted);
  transition: color 0.2s ease, background 0.25s ease, transform 0.2s ease;
}

.nav-menu :deep(.ant-menu-item::after) {
  display: none;
}

.nav-menu :deep(.ant-menu-item:hover) {
  color: var(--pb-ink) !important;
  background: rgba(108, 92, 231, 0.06);
  transform: translateY(-1px);
}

.nav-menu :deep(.ant-menu-item-selected) {
  color: var(--pb-violet) !important;
  background: linear-gradient(120deg, rgba(108, 92, 231, 0.12), rgba(56, 189, 248, 0.1)) !important;
}

.user-login-status {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  height: 68px;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  padding: 0;
  color: inherit;
  transition: color 0.2s ease;
}

.user-chip:hover {
  color: var(--pb-violet);
}

.user-chip:focus-visible {
  border-radius: 6px;
  outline: 2px solid rgba(108, 92, 231, 0.45);
  outline-offset: 4px;
}

.user-name {
  max-width: 88px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--pb-ink);
  font-size: 13px;
}

.auth-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.auth-actions :deep(.ant-btn-text) {
  color: var(--pb-muted);
}

.auth-actions :deep(.ant-btn-text:hover) {
  color: var(--pb-violet) !important;
}

@media (max-width: 720px) {
  #globalHeader .title-bar {
    gap: 8px;
  }

  .brand .subtitle,
  .nav-menu {
    display: none;
  }
}
</style>
