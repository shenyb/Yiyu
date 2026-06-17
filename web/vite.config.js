import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  // 构建产物输出到 Spring Boot 的 static 目录
  build: {
    outDir: '../java/src/main/resources/static',
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        timeout: 300000, // 5 分钟，匹配后端 DeepSeek 最长 180s + 余量
      },
    },
  },
})
