<template>
  <div class="chat-panel">
    <div class="chat-header">
      <h2>🔬 AI 课题调研 <span class="badge" style="background:#0066cc;">引导模式</span></h2>
      <p>告诉我研究方向，帮你查文献、整理调研报告</p>
    </div>
    <div class="messages" ref="msgContainer">
      <div class="msg ai">
        <div class="msg-avatar">🤖</div>
        <div class="msg-bubble">
          好的！我来帮你做课题调研。请告诉我：
          <div style="margin-top:10px;padding:12px;background:#e8f4ff;border-radius:8px;border-left:3px solid #0066cc;font-size:13.5px;color:#555;">
            <b>例如：</b>"帮我查一下近三年mRNA疫苗在肿瘤治疗中的临床研究进展"
          </div>
          <div style="margin-top:10px;font-size:13px;color:#888;">
            📚 我会联网检索相关文献，整理成调研报告
          </div>
        </div>
      </div>
      <div v-for="(msg, i) in messages" :key="i" class="msg" :class="msg.role">
        <div class="msg-avatar">{{ msg.role==='user'?'👤':'🤖' }}</div>
        <div class="msg-bubble" v-html="msg.content"></div>
      </div>
    </div>
    <div class="input-area" @dragover.prevent="dragOver=true" @dragleave="dragOver=false" @drop.prevent="onDrop" :class="{'drag-over':dragOver}">
      <div class="drop-indicator">📄 松开以添加参考文献</div>
      <div class="input-row">
        <textarea v-model="inputText" placeholder="输入研究方向…" rows="1" @keydown.enter.prevent="sendMessage" @input="autoResize" :disabled="loading"></textarea>
        <button class="send-btn" :disabled="!inputText.trim()||loading" @click="sendMessage">↵</button>
      </div>
      <div class="input-hint">
        <span>💡 可指定：研究方向、时间范围、语言</span>
        <span style="color:#0066cc;cursor:pointer;" @click="inputText='帮我查一下近三年SGLT2抑制剂在心力衰竭治疗中的临床研究进展，包含中文和英文文献'">点此填入示例</span>
        <span class="file-btn" @click="triggerFileInput">📎 拖拽文献到此</span>
      </div>
      <input type="file" ref="fileInput" style="display:none" accept=".pdf,.docx,.doc,.txt" @change="onFileSelected" />
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import axios from 'axios'

const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const dragOver = ref(false)
const step = ref(0)
const outlineStr = ref('')
const msgContainer = ref(null)
const fileInput = ref(null)

function autoResize(e) { e.target.style.height='auto'; e.target.style.height=Math.min(e.target.scrollHeight,120)+'px' }
function addMsg(role, content) {
  messages.value.push({role, content})
  nextTick(() => { if(msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight })
}
function triggerFileInput() { fileInput.value?.click() }
function onFileSelected(e) { const f=e.target.files[0]; if(f) handleFile(f) }
function onDrop(e) { dragOver.value=false; const f=e.dataTransfer.files[0]; if(f) handleFile(f) }

function handleFile(file) {
  const iconMap = { pdf:'📄', docx:'📝', doc:'📝', txt:'📄' }
  const ext = file.name.split('.').pop().toLowerCase()
  addMsg('user', `${iconMap[ext]||'📎'} <b>${file.name}</b> 已上传`)
  addMsg('ai', `已收到 <b>${file.name}</b> ✅ 请告诉我研究方向👇`)
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || loading.value) return
  inputText.value = ''
  loading.value = true
  addMsg('user', text)

  if (step.value === 0) {
    addMsg('ai', '好的，我来检索相关文献，请稍等...⏳')
    try {
      const res = await axios.post('/api/research/generate-outline', { topic: text })
      if (res.data.success && res.data.outline) {
        outlineStr.value = res.data.outline
        const outline = JSON.parse(res.data.outline)
        const sections = outline.sections || []
        let itemsHtml = sections.map((s, i) =>
          `<div class="item"><span class="num">${i+1}</span> ${escapeHtml(s.title||'')}</div>`
        ).join('')
        updateLastMsg(`已为您整理了一份调研报告框架：
          <div class="outline-card">${itemsHtml}</div>
          <div style="margin-top:8px;font-size:13px;color:#888;">
            📚 主题：${escapeHtml(outline.title||'')}
          </div>
          <div class="card-actions">
            <button class="btn btn-primary" onclick="document.dispatchEvent(new CustomEvent('research-confirm'))">✅ 满意，生成报告</button>
            <button class="btn btn-outline" onclick="document.dispatchEvent(new CustomEvent('research-modify'))">✏️ 调整框架</button>
          </div>`)
        step.value = 1
      } else {
        updateLastMsg('没成功生成框架，换个说法试试？')
      }
    } catch {
      updateLastMsg('检索失败，请稍后重试😅')
    }
  } else {
    addMsg('ai', '好的，已记录。请在上方点击「满意，生成报告」或继续调整。')
  }
  loading.value = false
}

async function confirmReport() {
  if (!outlineStr.value) return
  loading.value = true
  addMsg('user', '框架没问题，生成报告吧！')
  addMsg('ai', '正在整理文献、撰写报告…⏳')

  try {
    const res = await axios.post('/api/research/generate-report', { outline: outlineStr.value })
    const result = res.data
    if (result.success) {
      updateLastMsg(`✅ 调研报告已生成完毕！
        <div style="margin-top:8px;padding:12px;background:#e8f4ff;border-radius:8px;border-left:3px solid #0066cc;">
          <b>📁 文件：</b>${result.fileName}<br>
          <b>📦 大小：</b>${formatSize(result.fileSize)}
        </div>
        <div class="card-actions">
          <button class="btn btn-primary" onclick="window.open('/api/research/download?file='+encodeURIComponent('${result.fileName}'))">📂 打开文件</button>
          <button class="btn btn-ghost" onclick="document.dispatchEvent(new CustomEvent('research-restart'))">🔄 重新调研</button>
        </div>`)
      step.value = 2
    } else {
      updateLastMsg('生成报告失败：' + (result.error || '未知错误'))
    }
  } catch {
    updateLastMsg('生成报告失败，请稍后重试😅')
  }
  loading.value = false
}

function modifyOutline() {
  addMsg('ai', `您想怎么调整框架？可以直接告诉我，比如：
    <div style="margin-top:8px;padding:10px 14px;background:#f5f5f5;border-radius:8px;font-size:13px;color:#666;">
      · "加一个章节写药物经济学数据"<br>
      · "把安全性和疗效合并"<br>
      · "多引用2025年以后的文献"
    </div>`)
}

function startOver() {
  step.value=0; outlineStr.value=''; messages.value=[]; inputText.value=''
}

function updateLastMsg(html) {
  if(messages.value.length>0) { messages.value[messages.value.length-1].content=html; scrollBottom() }
}
function escapeHtml(str) { const d=document.createElement('div'); d.textContent=str; return d.innerHTML }
function formatSize(bytes) { if(!bytes) return '未知'; if(bytes<1024) return bytes+' B'; if(bytes<1048576) return (bytes/1024).toFixed(1)+' KB'; return (bytes/1048576).toFixed(1)+' MB' }

document.addEventListener('research-confirm', confirmReport)
document.addEventListener('research-modify', modifyOutline)
document.addEventListener('research-restart', startOver)
</script>

<style scoped>
.chat-panel{flex:1;display:flex;flex-direction:column;background:#f7f8fa;min-width:0}
.chat-header{padding:16px 20px 12px;border-bottom:1px solid #e8e8ec;background:#fff;flex-shrink:0}
.chat-header h2{font-size:16px;font-weight:600;color:#1a1a2e;display:flex;align-items:center;gap:8px}
.chat-header h2 .badge{font-size:11px;font-weight:400;background:#0066cc;color:#fff;padding:1px 10px;border-radius:10px}
.chat-header p{font-size:13px;color:#888;margin-top:2px}
.messages{flex:1;overflow-y:auto;padding:16px 20px;display:flex;flex-direction:column;gap:12px}
.msg{display:flex;gap:10px;max-width:85%;animation:fadeIn .25s ease}
@keyframes fadeIn{from{opacity:0;transform:translateY(6px)}to{opacity:1;transform:translateY(0)}}
.msg.user{align-self:flex-end;flex-direction:row-reverse}
.msg.ai{align-self:flex-start}
.msg-avatar{width:34px;height:34px;border-radius:50%;flex-shrink:0;display:flex;align-items:center;justify-content:center;font-size:15px}
.msg.user .msg-avatar{background:#c00000;color:#fff}
.msg.ai .msg-avatar{background:#1e1e2d;color:#fff}
.msg-bubble{padding:12px 16px;border-radius:14px;font-size:14px;line-height:1.65}
.msg.user .msg-bubble{background:#c00000;color:#fff;border-bottom-right-radius:4px}
.msg.ai .msg-bubble{background:#fff;color:#333;border-bottom-left-radius:4px;box-shadow:0 1px 4px rgba(0,0,0,.06)}
.outline-card{background:#f9fafb;border:1px solid #e8e8ec;border-radius:10px;padding:12px 16px;margin-top:8px}
.outline-card .item{display:flex;gap:8px;padding:5px 0;font-size:13.5px;color:#444}
.outline-card .item .num{width:20px;height:20px;border-radius:50%;background:#0066cc;color:#fff;display:flex;align-items:center;justify-content:center;flex-shrink:0;font-size:11px;font-weight:600;margin-top:2px}
.card-actions{display:flex;gap:8px;margin-top:12px;flex-wrap:wrap}
.card-actions .btn{padding:7px 18px;border-radius:8px;border:none;font-size:13px;cursor:pointer;font-family:inherit;display:inline-flex;align-items:center;gap:6px;transition:all .15s}
.card-actions .btn-primary{background:#0066cc;color:#fff}
.card-actions .btn-primary:hover{background:#0055aa}
.card-actions .btn-outline{background:transparent;color:#0066cc;border:1px solid #0066cc}
.card-actions .btn-outline:hover{background:#e8f4ff}
.card-actions .btn-ghost{background:transparent;color:#666;border:1px solid #ddd}
.card-actions .btn-ghost:hover{background:#f5f5f5}
.input-area{padding:12px 16px 14px;background:#fff;border-top:1px solid #e8e8ec;flex-shrink:0;position:relative}
.input-area.drag-over textarea{border-color:#0066cc;background:#e8f4ff}
.drop-indicator{display:none;position:absolute;inset:0;background:rgba(0,102,204,0.04);border:2px dashed #0066cc;border-radius:10px;align-items:center;justify-content:center;font-size:15px;color:#0066cc;font-weight:500;pointer-events:none;z-index:10}
.input-area.drag-over .drop-indicator{display:flex}
.input-row{display:flex;gap:10px;align-items:flex-end}
.input-row textarea{flex:1;border:1px solid #ddd;border-radius:10px;padding:10px 14px;font-size:14px;font-family:inherit;resize:none;height:44px;line-height:1.5;outline:none}
.input-row textarea:focus{border-color:#0066cc}
.input-row textarea:disabled{background:#f5f5f5}
.input-row .send-btn{width:44px;height:44px;border-radius:10px;border:none;background:#0066cc;color:#fff;font-size:18px;cursor:pointer;display:flex;align-items:center;justify-content:center}
.input-row .send-btn:disabled{background:#ccc;cursor:not-allowed}
.input-hint{font-size:12px;color:#aaa;margin-top:6px;padding-left:4px;display:flex;gap:14px;align-items:center}
.input-hint .file-btn{color:#0066cc;cursor:pointer;font-size:13px;display:inline-flex;gap:4px}
.input-hint .file-btn:hover{color:#004499}
::-webkit-scrollbar{width:6px}
::-webkit-scrollbar-thumb{background:#ddd;border-radius:3px}
</style>
