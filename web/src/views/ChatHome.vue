<template>
  <div class="chat-panel">
    <div class="chat-header">
      <h2>
        <span>👋 你好！有什么需要帮忙的？</span>
        <span class="badge">体验版</span>
      </h2>
      <p>直接说出来，我来帮你做</p>
    </div>

    <div class="messages" ref="msgContainer">
      <div class="msg ai">
        <div class="msg-avatar">🤖</div>
        <div class="msg-bubble">
          你好！我是医语，可以帮你做这些事：
          <div class="suggestion-cards">
            <div class="suggestion-card" @click="$router.push('/ppt')">
              <div class="icon">📊</div>
              <h4>做PPT</h4>
              <p>输入主题，帮你生成科室汇报、教学课件</p>
              <span class="tag">试试 →</span>
            </div>
            <div class="suggestion-card" @click="$router.push('/research')">
              <div class="icon">🔬</div>
              <h4>课题调研</h4>
              <p>给个研究方向，帮你查文献、整理报告</p>
              <span class="tag">试试 →</span>
            </div>
            <div class="suggestion-card" @click="$router.push('/docqa')">
              <div class="icon">📄</div>
              <h4>读文件</h4>
              <p>上传PDF或Word，直接问文件里的内容</p>
              <span class="tag">试试 →</span>
            </div>
          </div>
          <div style="margin-top:10px;font-size:13px;color:#999;">
            或者直接在下面输入你的需求 ↘
          </div>
        </div>
      </div>

      <div v-for="(msg, i) in messages" :key="i" class="msg" :class="msg.role">
        <div class="msg-avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
        <div class="msg-bubble" v-html="msg.content"></div>
      </div>
    </div>

    <div class="input-area" @dragover.prevent="dragOver=true" @dragleave="dragOver=false" @drop.prevent="onDrop" :class="{ 'drag-over': dragOver }">
      <div class="drop-indicator">📄 松开以添加文件</div>
      <div class="input-row">
        <textarea
          v-model="inputText"
          placeholder="比如说：帮我做一个关于糖尿病足的PPT…"
          rows="1"
          @keydown.enter.exact="sendMessage"
          @input="autoResize"
        ></textarea>
        <button class="send-btn" :disabled="!inputText.trim()" @click="sendMessage">↵</button>
      </div>
      <div class="input-hint">
        <span>按 Enter 发送，Shift+Enter 换行</span>
        <span class="file-btn" @click="triggerFileInput">📎 拖拽文件到此处或点此上传</span>
      </div>
      <input type="file" ref="fileInput" style="display:none" @change="onFileSelected" />
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { chatSend } from '../api/index.js'

const messages = ref([])
const inputText = ref('')
const dragOver = ref(false)
const msgContainer = ref(null)
const fileInput = ref(null)

function autoResize(e) {
  const el = e.target
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}

function scrollBottom() {
  nextTick(() => {
    if (msgContainer.value) {
      msgContainer.value.scrollTop = msgContainer.value.scrollHeight
    }
  })
}

function addMsg(role, content) {
  messages.value.push({ role, content })
  scrollBottom()
}

async function sendMessage(e) {
  if (e) e.preventDefault()
  const text = inputText.value.trim()
  if (!text) return
  inputText.value = ''
  addMsg('user', text)

  try {
    const res = await chatSend(text)
    addMsg('ai', res.data.reply || '已收到您的消息，正在处理...')
  } catch {
    addMsg('ai', '没成功，换个说法试试？😅')
  }
}

function triggerFileInput() {
  fileInput.value?.click()
}

function onFileSelected(e) {
  const file = e.target.files[0]
  if (file) handleFile(file)
}

function onDrop(e) {
  dragOver.value = false
  const file = e.dataTransfer.files[0]
  if (file) handleFile(file)
}

function handleFile(file) {
  const iconMap = { pdf: '📄', docx: '📝', doc: '📝', pptx: '📊', ppt: '📊', txt: '📄', xlsx: '📊' }
  const ext = file.name.split('.').pop().toLowerCase()
  const icon = iconMap[ext] || '📎'
  addMsg('user', `${icon} <b>${file.name}</b> 已上传`)
  addMsg('ai', `已收到 <b>${file.name}</b> ✅ 请告诉我您想做什么？`)
}
</script>

<style scoped>
.chat-panel {
  flex: 1; display: flex; flex-direction: column;
  background: #f7f8fa; min-width: 0;
}
.chat-header {
  padding: 16px 20px 12px; border-bottom: 1px solid #e8e8ec;
  background: #fff; flex-shrink: 0;
}
.chat-header h2 {
  font-size: 16px; font-weight: 600; color: #1a1a2e;
  display: flex; align-items: center; gap: 8px;
}
.chat-header h2 .badge {
  font-size: 11px; font-weight: 400;
  background: #c00000; color: #fff; padding: 1px 10px;
  border-radius: 10px;
}
.chat-header p { font-size: 13px; color: #888; margin-top: 2px; }
.messages {
  flex: 1; overflow-y: auto; padding: 16px 20px;
  display: flex; flex-direction: column; gap: 12px;
}
.msg { display: flex; gap: 10px; max-width: 85%; animation: fadeIn 0.25s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: translateY(0); } }
.msg.user { align-self: flex-end; flex-direction: row-reverse; }
.msg.ai { align-self: flex-start; }
.msg-avatar {
  width: 34px; height: 34px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; font-size: 15px;
}
.msg.user .msg-avatar { background: #c00000; color: #fff; }
.msg.ai .msg-avatar { background: #1e1e2d; color: #fff; }
.msg-bubble {
  padding: 12px 16px; border-radius: 14px; font-size: 14px; line-height: 1.65;
}
.msg.user .msg-bubble { background: #c00000; color: #fff; border-bottom-right-radius: 4px; }
.msg.ai .msg-bubble {
  background: #fff; color: #333; border-bottom-left-radius: 4px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.suggestion-cards { display: flex; gap: 10px; margin-top: 10px; flex-wrap: wrap; }
.suggestion-card {
  flex: 1; min-width: 160px; background: #fff; border: 1px solid #e8e8ec;
  border-radius: 12px; padding: 16px; cursor: pointer; transition: all 0.2s;
}
.suggestion-card:hover {
  border-color: #c00000; box-shadow: 0 4px 16px rgba(192,0,0,0.08);
  transform: translateY(-1px);
}
.suggestion-card .icon { font-size: 28px; margin-bottom: 8px; }
.suggestion-card h4 { font-size: 14px; color: #1a1a2e; }
.suggestion-card p { font-size: 12px; color: #999; margin-top: 4px; line-height: 1.5; }
.suggestion-card .tag {
  display: inline-block; font-size: 11px; background: #fff0f0; color: #c00000;
  padding: 1px 8px; border-radius: 6px; margin-top: 8px;
}
.input-area {
  padding: 12px 16px 14px; background: #fff; border-top: 1px solid #e8e8ec;
  flex-shrink: 0; position: relative;
}
.input-area.drag-over textarea { border-color: #c00000; background: #fff5f5; }
.drop-indicator {
  display: none; position: absolute; inset: 0;
  background: rgba(192,0,0,0.04); border: 2px dashed #c00000; border-radius: 10px;
  align-items: center; justify-content: center;
  font-size: 15px; color: #c00000; font-weight: 500; pointer-events: none; z-index: 10;
}
.input-area.drag-over .drop-indicator { display: flex; }
.input-row { display: flex; gap: 10px; align-items: flex-end; }
.input-row textarea {
  flex: 1; border: 1px solid #ddd; border-radius: 10px; padding: 10px 14px;
  font-size: 14px; font-family: inherit; resize: none; height: 44px;
  line-height: 1.5; outline: none; transition: border-color 0.15s;
}
.input-row textarea:focus { border-color: #c00000; }
.input-row .send-btn {
  width: 44px; height: 44px; border-radius: 10px; border: none;
  background: #c00000; color: #fff; font-size: 18px; cursor: pointer;
  display: flex; align-items: center; justify-content: center; transition: background 0.15s;
}
.input-row .send-btn:hover { background: #a00000; }
.input-row .send-btn:disabled { background: #ccc; cursor: not-allowed; }
.input-hint {
  font-size: 12px; color: #aaa; margin-top: 6px; padding-left: 4px;
  display: flex; gap: 14px; align-items: center;
}
.input-hint .file-btn { color: #c00000; cursor: pointer; font-size: 13px; display: inline-flex; gap: 4px; }
.input-hint .file-btn:hover { color: #a00000; }
::-webkit-scrollbar { width: 6px; }
::-webkit-scrollbar-thumb { background: #ddd; border-radius: 3px; }
</style>
