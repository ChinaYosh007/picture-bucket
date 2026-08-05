import { generateService } from '@umijs/openapi'

/**
 * 从后端 SpringDoc 生成 API 客户端。
 * 使用前请先启动 picture-service（默认 http://localhost:8080）。
 */
generateService({
  requestLibPath: "import request from '@/request'",
  schemaPath: 'http://localhost:8080/api/v3/api-docs',
  serversPath: './src',
})
