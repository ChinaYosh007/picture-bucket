import axios from 'axios'
import { message } from 'ant-design-vue'

/**
 * 后端 picture-service：
 * - port: 8080
 * - context-path: /api
 * - 前端开发端口: 5173
 */
const DEV_BASE_URL = 'http://localhost:8080'
// const PROD_BASE_URL = ''

const myAxios = axios.create({
  baseURL: DEV_BASE_URL,
  timeout: 10000,
  withCredentials: true,
})

myAxios.interceptors.request.use(
  function (config) {
    return config
  },
  function (error) {
    return Promise.reject(error)
  },
)

myAxios.interceptors.response.use(
  function (response) {
    const { data } = response
    // 未登录（ErrorCode.NOT_LOGIN_ERROR = 40100）
    if (data?.code === 40100) {
      if (
        !response.request.responseURL.includes('user/get/login') &&
        !window.location.pathname.includes('/user/login')
      ) {
        message.warning('请先登录')
        window.location.href = `/user/login?redirect=${encodeURIComponent(window.location.href)}`
      }
    }
    return response
  },
  function (error) {
    return Promise.reject(error)
  },
)

export default myAxios
