import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 120000,
})

export function health() {
  return api.get('/health')
}

// 通用对话
export function chatSend(message, fileIds = []) {
  return api.post('/chat/send', { message, fileIds })
}

// 文件上传
export function uploadFile(file) {
  const fd = new FormData()
  fd.append('file', file)
  return api.post('/upload', fd)
}

// PPT 大纲生成
export function generateOutline(topic, fileIds = []) {
  return api.post('/ppt/generate-outline', { topic, fileIds })
}

// PPT 确认生成
export function confirmPpt(outline, adjustments = '') {
  return api.post('/ppt/confirm', { outline, adjustments })
}

// 下载 PPT 文件
export function downloadFile(fileId) {
  return api.get(`/ppt/download/${fileId}`, { responseType: 'blob' })
}

// 文件列表
export function listPptFiles() {
  return api.get('/ppt/files')
}

export default api
