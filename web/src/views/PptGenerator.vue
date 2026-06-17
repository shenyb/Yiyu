<template>
  <div class="chat-panel">
    <div class="chat-header">
      <h2>📊 AI 做PPT <span class="badge" style="background:#2e7d32;">引导模式</span></h2>
      <p>告诉我主题和要求，我帮你生成大纲和PPT</p>
    </div>
    <div class="messages" ref="msgContainer">
      <div class="msg ai">
        <div class="msg-avatar">🤖</div>
        <div class="msg-bubble">
          好的！我们来做一个PPT。请告诉我：
          <div style="margin-top:10px;padding:12px;background:#fff5f5;border-radius:8px;border-left:3px solid #c00000;font-size:13.5px;color:#555;">
            <b>例如：</b>"帮我做一个关于高血压基层防治的PPT，大约12页，面向社区医生，风格简洁专业"
          </div>
          <div style="margin-top:10px;display:flex;gap:12px;font-size:13px;color:#888;">
            <span>📐 可上传参考资料或PPT模板，我会参考里面的内容</span>
          </div>
        </div>
      </div>
      <div v-for="(msg, i) in messages" :key="i" class="msg" :class="msg.role">
        <div class="msg-avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
        <div class="msg-bubble">
          <span v-html="msg.content"></span>
          <span v-if="msg.role==='ai'" class="copy-btn" @click="copyMsg($event, i)">复制</span>
        </div>
      </div>
    </div>
    <div class="input-area" @dragover.prevent="dragOver=true" @dragleave="dragOver=false" @drop.prevent="onDrop" :class="{ 'drag-over': dragOver }">
      <div class="drop-indicator">📄 松开以添加文件</div>
      <div class="input-row">
        <textarea v-model="inputText" placeholder="输入PPT主题和要求…" rows="1" @keydown.enter.prevent="sendMessage" @input="autoResize" :disabled="loading"></textarea>
        <button class="send-btn" :disabled="!inputText.trim() || loading" @click="sendMessage">↵</button>
      </div>
      <div class="input-hint">
        <span>💡 建议说明：主题、页数、风格、面向对象</span>
        <span style="color:#c00000;cursor:pointer;" @click="inputText='帮我做一个关于高血压基层防治的PPT，约12页，面向社区医生，风格简洁专业'">点此填入示例</span>
        <span class="file-btn" @click="triggerRefInput">📎 参考资料或拖拽到此</span>
        <span class="file-btn" style="color:#2e7d32;" @click="triggerTplInput">📐 PPT模板(.pptx)</span>
      </div>
      <input type="file" ref="refFileInput" style="display:none" accept=".pdf,.docx,.doc,.txt" @change="onRefFileSelected" />
      <input type="file" ref="tplFileInput" style="display:none" accept=".pptx" @change="onTplFileSelected" />
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import api from '../api/index.js'

const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const dragOver = ref(false)
const step = ref(0)
const outlineStr = ref('')
const msgContainer = ref(null)
const refFileInput = ref(null)
const tplFileInput = ref(null)
const refContent = ref('')
const templatePath = ref('')

function autoResize(e) {
  e.target.style.height = 'auto'
  e.target.style.height = Math.min(e.target.scrollHeight, 120) + 'px'
}

function addMsg(role, content) {
  messages.value.push({ role, content })
  scrollBottom()
}

function scrollBottom() {
  nextTick(() => {
    if (msgContainer.value) {
      msgContainer.value.scrollTop = msgContainer.value.scrollHeight
    }
  })
}

function triggerRefInput() { refFileInput.value?.click() }
function triggerTplInput() { tplFileInput.value?.click() }

function onRefFileSelected(e) {
  const file = e.target.files[0]
  if (file) handleRefFile(file)
  e.target.value = ''
}

function onTplFileSelected(e) {
  const file = e.target.files[0]
  if (file) handleTplFile(file)
  e.target.value = ''
}

function onDrop(e) {
  dragOver.value = false
  const file = e.dataTransfer.files[0]
  if (!file) return
  if (file.name.endsWith('.pptx')) handleTplFile(file)
  else handleRefFile(file)
}

async function handleRefFile(file) {
  const iconMap = { pdf: '📄', docx: '📝', doc: '📝', txt: '📄' }
  const ext = file.name.split('.').pop().toLowerCase()
  addMsg('user', `${iconMap[ext]||'📎'} <b>${file.name}</b> 已上传`)
  addMsg('ai', `正在读取 <b>${file.name}</b> ...⏳`)
  try {
    const fd = new FormData()
    fd.append('file', file)
    const res = await api.post('/ppt/upload-ref', fd)
    if (res.data.success) {
      refContent.value = res.data.content
      updateLastMsg(`已读取 <b>${file.name}</b> ✅ 文件内容已作为参考资料。`)
    } else {
      updateLastMsg('文件读取失败：' + (res.data.error || '未知错误'))
    }
  } catch (err) {
    console.error('文件上传失败:', err, err?.response?.data)
    updateLastMsg('文件上传失败😅')
  }
}

async function handleTplFile(file) {
  addMsg('user', `📐 <b>${file.name}</b> 已选为PPT模板`)
  addMsg('ai', `正在上传模板 <b>${file.name}</b> ...⏳`)
  try {
    const fd = new FormData()
    fd.append('file', file)
    const res = await api.post('/ppt/upload-template', fd)
    if (res.data.success) {
      templatePath.value = res.data.templatePath
      updateLastMsg(`模板 <b>${file.name}</b> 上传成功 ✅ 生成PPT时会套用此模板的样式和布局。`)
    } else {
      updateLastMsg('模板上传失败：' + (res.data.error || '未知错误'))
    }
  } catch (err) {
    console.error('模板上传失败:', err, err?.response?.data)
    updateLastMsg('模板上传失败😅')
  }
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || loading.value) return
  inputText.value = ''
  loading.value = true
  addMsg('user', text)

  if (step.value === 0) {
    // 生成大纲
    addMsg('ai', '好的，我来整理大纲...⏳')
    try {
      const res = await api.post('/ppt/generate-outline', { topic: text, reference: refContent.value })
      if (res.data.success && res.data.outline) {
        outlineStr.value = res.data.outline
        let outline
        try {
          outline = JSON.parse(res.data.outline)
        } catch (parseErr) {
          console.error('outline JSON 解析失败:', parseErr)
          updateLastMsg('大纲解析失败，AI 返回格式异常。请重试或换个说法。')
          loading.value = false
          return
        }
        updateLastMsg(buildOutlineHtml(outline))
        step.value = 1
      } else {
        updateLastMsg('没成功生成大纲，换个说法试试？')
      }
    } catch (err) {
      console.error('生成大纲失败:', err, err?.response?.data)
      updateLastMsg('没成功，换个说法试试？😅')
    }
  } else if (step.value === 1) {
    // 用户输入修改意见 → 调整大纲 → 展示新大纲让用户确认
    addMsg('ai', '好的，根据您的意见调整大纲...⏳')
    try {
      const outlineRes = await api.post('/ppt/generate-outline', {
        topic: text,
        reference: refContent.value,
        prevOutline: outlineStr.value
      })
      if (outlineRes.data.success && outlineRes.data.outline) {
        let outline
        try {
          outline = JSON.parse(outlineRes.data.outline)
        } catch (parseErr) {
          console.error('outline JSON 解析失败:', parseErr)
          updateLastMsg('大纲解析失败，请重试。')
          loading.value = false
          return
        }
        outlineStr.value = outlineRes.data.outline
        updateLastMsg(buildOutlineHtml(outline))
        // step 保持 1，用户可以继续调整或点"满意，生成PPT"
      } else {
        updateLastMsg('调整大纲失败，换个说法试试？')
      }
    } catch (err) {
      console.error('调整大纲失败:', err, err?.response?.data)
      updateLastMsg('调整大纲失败，请稍后重试😅')
    }
  }
  loading.value = false
}

async function confirmOutline() {
  if (!outlineStr.value) return
  loading.value = true
  addMsg('user', '大纲没问题，生成吧！')

  // 先获取可用模板列表
  try {
    const tplRes = await api.get('/ppt/templates')
    const templates = tplRes.data.templates || []
    if (templates.length > 1) {
      // 多个模板，让用户选择
      let tplHtml = templates.map((t, i) =>
        `<label class="tpl-option"><input type="radio" name="tpl-select" value="${i}" ${i === 0 ? 'checked' : ''}> <span class="tpl-name">${escapeHtml(t.name)}</span> <span class="tpl-size">(${formatSize(parseInt(t.size))})</span></label>`
      ).join('')
      updateLastMsg(`请选择PPT模板：
        <div class="tpl-list">${tplHtml}</div>
        <div class="card-actions">
          <button class="btn btn-primary" onclick="document.dispatchEvent(new CustomEvent('ppt-generate'))">✅ 开始生成</button>
        </div>`)
      // 暂存模板列表供后续使用
      window.__pptTemplates = templates
      loading.value = false
      return
    } else if (templates.length === 1) {
      // 只有一个模板，直接用
      templatePath.value = templates[0].path
    }
    // 无模板或有唯一模板，直接生成
    addMsg('ai', '好的，正在为您生成PPT…⏳')
    await doGeneratePpt()
  } catch (err) {
    console.error('获取模板列表失败:', err)
    addMsg('ai', '好的，正在为您生成PPT…⏳')
    await doGeneratePpt()
  }
  loading.value = false
}

async function doGeneratePpt() {
  try {
    const payload = { outline: outlineStr.value }
    if (templatePath.value) payload.templatePath = templatePath.value
    const res = await api.post('/ppt/confirm', payload)
    const result = res.data
    if (result.success) {
      updateLastMsg(`✅ PPT 已生成完毕！
        <div style="margin-top:8px;padding:12px;background:#f0fff0;border-radius:8px;border-left:3px solid #2e7d32;">
          <b>📁 文件：</b>${result.fileName}<br>
          <b>📦 大小：</b>${formatSize(result.fileSize)}
        </div>
        <div class="card-actions">
          <a class="btn btn-primary" href="/api/ppt/download?file=${encodeURIComponent(result.fileName)}" download>📂 下载文件</a>
          <button class="btn btn-ghost" onclick="document.dispatchEvent(new CustomEvent('ppt-restart'))">🔄 重新做一个</button>
        </div>`)
      step.value = 2
    } else {
      updateLastMsg('生成 PPT 失败：' + (result.error || '未知错误'))
    }
  } catch (err) {
    console.error('生成 PPT 失败:', err, err?.response?.data)
    updateLastMsg('生成 PPT 失败，请稍后重试😅')
  }
}

function modifyOutline() {
  addMsg('ai', `好的，您想怎么调整？直接告诉我修改意见，我会调整大纲并直接生成PPT：
    <div style="margin-top:8px;padding:10px 14px;background:#f5f5f5;border-radius:8px;font-size:13px;color:#666;">
      · "第三点再展开一些，加上最新的指南推荐"<br>
      · "在最后加一页总结"<br>
      · "把第五点和第六点合并"
    </div>`)
}

function startOver() {
  step.value = 0
  outlineStr.value = ''
  messages.value = []
  inputText.value = ''
}

function updateLastMsg(html) {
  if (messages.value.length > 0) {
    messages.value[messages.value.length - 1].content = html
    scrollBottom()
  }
}

function buildOutlineHtml(outline) {
  const slides = outline.slides || []
  let itemsHtml = slides.map((s, i) =>
    `<div class="item"><span class="num">${i + 1}</span> ${escapeHtml(s.title || '')}</div>`
  ).join('')

  return `好的，已经整理了一份大纲，您看看是否满意：
    <div class="outline-card">${itemsHtml}</div>
    <div style="margin-top:8px;font-size:13px;color:#888;">
      📐 共 ${slides.length} 页 · ${escapeHtml(outline.title || '')}
    </div>
    <div class="card-actions">
      <button class="btn btn-primary" onclick="document.dispatchEvent(new CustomEvent('ppt-confirm'))">✅ 满意，生成PPT</button>
      <button class="btn btn-outline" onclick="document.dispatchEvent(new CustomEvent('ppt-modify'))">✏️ 调整一下</button>
    </div>`
}

function escapeHtml(str) {
  const div = document.createElement('div')
  div.textContent = str
  return div.innerHTML
}

function formatSize(bytes) {
  if (!bytes) return '未知'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

document.addEventListener('ppt-confirm', confirmOutline)
document.addEventListener('ppt-modify', modifyOutline)
document.addEventListener('ppt-restart', startOver)
document.addEventListener('ppt-generate', async () => {
  // 用户选完模板，开始生成
  const templates = window.__pptTemplates || []
  const selected = document.querySelector('input[name="tpl-select"]:checked')
  if (selected && templates[selected.value]) {
    templatePath.value = templates[selected.value].path
  }
  loading.value = true
  addMsg('ai', '好的，正在为您生成PPT…⏳')
  await doGeneratePpt()
  loading.value = false
})

function copyMsg(e, i) {
  const bubble = e.target.parentElement
  const text = bubble?.innerText || ''
  navigator.clipboard.writeText(text.replace('复制', '').trim()).then(() => {
    e.target.textContent = '已复制'
    setTimeout(() => e.target.textContent = '复制', 1500)
  })
}
</script>

<style>
/* 按钮样式不使用 scoped，因为 v-html 内的按钮需要穿透 */
.card-actions { display:flex; gap:8px; margin-top:12px; flex-wrap:wrap; }
.card-actions .btn { padding:7px 18px; border-radius:8px; border:none; font-size:13px; cursor:pointer; display:inline-flex; align-items:center; gap:6px; transition:all .15s; text-decoration:none; }
.card-actions .btn-primary { background:#c00000; color:#fff; }
.card-actions .btn-primary:hover { background:#a00000; }
.card-actions .btn-outline { background:transparent; color:#c00000; border:1px solid #c00000; }
.card-actions .btn-outline:hover { background:#fff5f5; }
.card-actions .btn-ghost { background:transparent; color:#666; border:1px solid #ddd; }
.card-actions .btn-ghost:hover { background:#f5f5f5; }
/* 模板选择列表 */
.tpl-list { margin-top:8px; display:flex; flex-direction:column; gap:6px; }
.tpl-option { display:flex; align-items:center; gap:8px; padding:8px 12px; background:#f9fafb; border:1px solid #e8e8ec; border-radius:8px; cursor:pointer; font-size:13px; transition:all .15s; }
.tpl-option:hover { background:#fff5f5; border-color:#c00000; }
.tpl-option input[type="radio"] { accent-color:#c00000; }
.tpl-name { font-weight:500; color:#333; }
.tpl-size { color:#999; font-size:12px; }
</style>

<style scoped>
.chat-panel { flex:1; display:flex; flex-direction:column; background:#f7f8fa; min-width:0; }
.chat-header { padding:16px 20px 12px; border-bottom:1px solid #e8e8ec; background:#fff; flex-shrink:0; }
.chat-header h2 { font-size:16px; font-weight:600; color:#1a1a2e; display:flex; align-items:center; gap:8px; }
.chat-header h2 .badge { font-size:11px; font-weight:400; background:#2e7d32; color:#fff; padding:1px 10px; border-radius:10px; }
.chat-header p { font-size:13px; color:#888; margin-top:2px; }
.messages { flex:1; overflow-y:auto; padding:16px 20px; display:flex; flex-direction:column; gap:12px; }
.msg { display:flex; gap:10px; max-width:85%; animation:fadeIn .25s ease; }
@keyframes fadeIn { from{opacity:0;transform:translateY(6px)} to{opacity:1;transform:translateY(0)} }
.msg.user { align-self:flex-end; flex-direction:row-reverse; }
.msg.ai { align-self:flex-start; }
.msg-avatar { width:34px;height:34px;border-radius:50%;flex-shrink:0;display:flex;align-items:center;justify-content:center;font-size:15px; }
.msg.user .msg-avatar { background:#c00000;color:#fff; }
.msg.ai .msg-avatar { background:#1e1e2d;color:#fff; }
.msg-bubble { padding:12px 16px; border-radius:14px; font-size:14px; line-height:1.65; }
.msg.user .msg-bubble { background:#c00000;color:#fff;border-bottom-right-radius:4px; }
.msg.ai .msg-bubble { background:#fff;color:#333;border-bottom-left-radius:4px;box-shadow:0 1px 4px rgba(0,0,0,.06);user-select:text;position:relative; }
.outline-card { background:#f9fafb;border:1px solid #e8e8ec;border-radius:10px;padding:12px 16px;margin-top:8px; }
.outline-card .item { display:flex;gap:8px;padding:5px 0;font-size:13.5px;color:#444; }
.outline-card .item .num { width:20px;height:20px;border-radius:50%;background:#c00000;color:#fff;display:flex;align-items:center;justify-content:center;flex-shrink:0;font-size:11px;font-weight:600;margin-top:2px; }
.input-area { padding:12px 16px 14px; background:#fff; border-top:1px solid #e8e8ec; flex-shrink:0; position:relative; }
.input-area.drag-over textarea { border-color:#c00000; background:#fff5f5; }
.drop-indicator { display:none; position:absolute; inset:0; background:rgba(192,0,0,0.04); border:2px dashed #c00000; border-radius:10px; align-items:center; justify-content:center; font-size:15px; color:#c00000; font-weight:500; pointer-events:none; z-index:10; }
.input-area.drag-over .drop-indicator { display:flex; }
.input-row { display:flex; gap:10px; align-items:flex-end; }
.input-row textarea { flex:1; border:1px solid #ddd; border-radius:10px; padding:10px 14px; font-size:14px; font-family:inherit; resize:none; height:44px; line-height:1.5; outline:none; }
.input-row textarea:focus { border-color:#c00000; }
.input-row textarea:disabled { background:#f5f5f5; }
.input-row .send-btn { width:44px;height:44px;border-radius:10px;border:none;background:#c00000;color:#fff;font-size:18px;cursor:pointer;display:flex;align-items:center;justify-content:center; }
.input-row .send-btn:hover { background:#a00000; }
.input-row .send-btn:disabled { background:#ccc;cursor:not-allowed; }
.input-hint { font-size:12px;color:#aaa;margin-top:6px;padding-left:4px;display:flex;gap:14px;align-items:center; }
.input-hint .file-btn { color:#c00000;cursor:pointer;font-size:13px;display:inline-flex;gap:4px; }
.input-hint .file-btn:hover { color:#a00000; }
.copy-btn { position:absolute;top:4px;right:4px;font-size:12px;color:#999;cursor:pointer;padding:2px 6px;border-radius:4px;transition:all .15s;opacity:0;background:rgba(255,255,255,0.8); }
.msg.ai .msg-bubble:hover .copy-btn { opacity:1; }
.copy-btn:hover { color:#c00000;background:#fff5f5; }
::-webkit-scrollbar { width:6px; }
::-webkit-scrollbar-thumb { background:#ddd;border-radius:3px; }
</style>
