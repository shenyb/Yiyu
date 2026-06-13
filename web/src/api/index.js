import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 60000,
})

export function health() {
  return api.get('/health')
}

export function chatSend(message, files) {
  return api.post('/chat/send', { message, files })
}

export function generateOutline(topic, files) {
  return api.post('/ppt/generate-outline', { topic, files })
}

export function confirmPpt(outline, adjustments) {
  return api.post('/ppt/confirm', { outline, adjustments })
}

export function downloadFile(fileId) {
  return api.get(`/ppt/download/${fileId}`, { responseType: 'blob' })
}

export function uploadFile(file) {
  const fd = new FormData()
  fd.append('file', file)
  return api.post('/upload', fd)
}

export default api
