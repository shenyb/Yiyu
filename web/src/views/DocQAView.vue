<template>
  <div class="chat-panel">
    <div class="chat-header">
      <h2>📄 读文件 <span class="badge" style="background:#2e7d32;">问答模式</span></h2>
      <p>上传PDF或Word，直接问文件里的内容</p>
    </div>
    <div class="messages" ref="msgContainer">
      <div class="msg ai">
        <div class="msg-avatar">🤖</div>
        <div class="msg-bubble">
          您好！上传一份文件，我来帮您阅读和解答问题。
          <div style="margin-top:10px;padding:12px;background:#f0f7f0;border-radius:8px;border-left:3px solid #2e7d32;font-size:13.5px;color:#555;">
            <b>支持的格式：</b>PDF、DOCX、DOC、TXT<br>
            <b>您可以：</b>拖拽文件到输入区，或点击"选择文件"上传
          </div>
        </div>
      </div>
      <div v-for="(msg, i) in messages" :key="i" class="msg" :class="msg.role">
        <div class="msg-avatar">{{ msg.role==='user'?'👤':'🤖' }}</div>
        <div class="msg-bubble" v-html="msg.content"></div>
      </div>
    </div>
    <div class="input-area" @dragover.prevent="dragOver=true" @dragleave="dragOver=false" @drop.prevent="onDrop" :class="{'drag-over':dragOver}">
      <div class="drop-indicator">📄 松开以添加文件</div>
      <div class="input-row">
        <textarea v-model="inputText" placeholder="上传文件后，输入问题…" rows="1" @keydown.enter.exact="sendMessage" @input="autoResize" :disabled="!hasFile||loading"></textarea>
        <button class="send-btn" :disabled="!inputText.trim()||!hasFile||loading" @click="sendMessage">↵</button>
      </div>
      <div class="input-hint">
        <span class="file-btn" @click="triggerFileInput">📎 选择文件或拖拽到此</span>
        <span v-if="fileName" style="color:#2e7d32;">已加载：{{ fileName }}</span>
      </div>
      <input type="file" ref="fileInput" style="display:none" accept=".pdf,.docx,.doc,.txt" @change="onFileSelected" />
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import axios from 'axios'

const sessionId = 'doc-' + Date.now()
const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const dragOver = ref(false)
const hasFile = ref(false)
const fileName = ref('')
const msgContainer = ref(null)
const fileInput = ref(null)

function autoResize(e) { e.target.style.height='auto'; e.target.style.height=Math.min(e.target.scrollHeight,120)+'px' }
function addMsg(role, content) {
  messages.value.push({role, content})
  nextTick(() => { if(msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight })
}
function triggerFileInput() { fileInput.value?.click() }
function onFileSelected(e) {
  const f=e.target.files[0]; if(f) {
    fileName.value = f.name; hasFile.value = true
    addMsg('user', `📄 <b>${f.name}</b> 已选择`)
    addMsg('ai', `已选择文件，正在读取...⏳`)
    loadFile(f)
  }
}
function onDrop(e) { dragOver.value=false; const f=e.dataTransfer.files[0]; if(f) handleFile(f) }

async function loadFile(file) {
  try {
    // 使用浏览器 File API 读取文件内容
    const text = await file.text()
    const res = await axios.post('/api/docqa/load', {
      sessionId: sessionId,
      filePath: file.name,
      fileContent: text.substring(0, 50000)
    })
    if (res.data.success) {
      fileName.value = res.data.fileName
      updateLastMsg(`已收到 <b>${file.name}</b> ✅<br>文件摘要：${res.data.summary}<br><br>您可以问我关于这份文件的问题了。`)
    } else {
      updateLastMsg('文件读取失败：' + (res.data.error || '未知错误'))
    }
  } catch {
    updateLastMsg('文件处理失败😅')
  }
}

function handleFile(file) {
  fileName.value = file.name; hasFile.value = true
  addMsg('user', `📄 <b>${file.name}</b> 已选择`)
  loadFile(file)
}

async function sendMessage() {
  const text = inputText.value.trim(); if(!text||!hasFile.value||loading.value) return
  inputText.value=''; loading.value=true
  addMsg('user', text)
  addMsg('ai', '正在查阅文件内容...⏳')
  try {
    const res = await axios.post('/api/docqa/ask', {
      sessionId: sessionId,
      question: text
    })
    if (res.data.success) {
      updateLastMsg(res.data.reply)
    } else {
      updateLastMsg('回答失败：' + (res.data.error || '未知错误'))
    }
  } catch {
    updateLastMsg('请求失败，请稍后重试😅')
  }
  loading.value = false
}

function updateLastMsg(html) { if(messages.value.length>0) { messages.value[messages.value.length-1].content=html; scrollBottom() } }
function scrollBottom() { nextTick(()=>{ if(msgContainer.value) msgContainer.value.scrollTop=msgContainer.value.scrollHeight }) }
</script>

<style scoped>
.chat-panel{flex:1;display:flex;flex-direction:column;background:#f7f8fa;min-width:0}
.chat-header{padding:16px 20px 12px;border-bottom:1px solid #e8e8ec;background:#fff;flex-shrink:0}
.chat-header h2{font-size:16px;font-weight:600;color:#1a1a2e;display:flex;align-items:center;gap:8px}
.chat-header h2 .badge{font-size:11px;font-weight:400;background:#2e7d32;color:#fff;padding:1px 10px;border-radius:10px}
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
.input-area{padding:12px 16px 14px;background:#fff;border-top:1px solid #e8e8ec;flex-shrink:0;position:relative}
.input-area.drag-over textarea{border-color:#2e7d32;background:#f0faf0}
.drop-indicator{display:none;position:absolute;inset:0;background:rgba(46,125,50,0.04);border:2px dashed #2e7d32;border-radius:10px;align-items:center;justify-content:center;font-size:15px;color:#2e7d32;font-weight:500;pointer-events:none;z-index:10}
.input-area.drag-over .drop-indicator{display:flex}
.input-row{display:flex;gap:10px;align-items:flex-end}
.input-row textarea{flex:1;border:1px solid #ddd;border-radius:10px;padding:10px 14px;font-size:14px;font-family:inherit;resize:none;height:44px;line-height:1.5;outline:none}
.input-row textarea:focus{border-color:#c00000}
.input-row textarea:disabled{background:#f5f5f5}
.input-row .send-btn{width:44px;height:44px;border-radius:10px;border:none;background:#c00000;color:#fff;font-size:18px;cursor:pointer;display:flex;align-items:center;justify-content:center}
.input-row .send-btn:disabled{background:#ccc;cursor:not-allowed}
.input-hint{font-size:12px;color:#aaa;margin-top:6px;padding-left:4px;display:flex;gap:14px;align-items:center}
.input-hint .file-btn{color:#2e7d32;cursor:pointer;font-size:13px;display:inline-flex;gap:4px}
.input-hint .file-btn:hover{color:#1b5e20}
::-webkit-scrollbar{width:6px}
::-webkit-scrollbar-thumb{background:#ddd;border-radius:3px}
</style>
