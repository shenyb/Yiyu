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
    <div class="input-area">
      <div class="input-row">
        <textarea v-model="inputText" placeholder="输入研究方向…" rows="1" @keydown.enter.exact="sendMessage" @input="autoResize"></textarea>
        <button class="send-btn" :disabled="!inputText.trim()" @click="sendMessage">↵</button>
      </div>
      <div class="input-hint">
        <span>💡 可指定：研究方向、时间范围、语言</span>
        <span style="color:#0066cc;cursor:pointer;" @click="inputText='帮我查一下近三年SGLT2抑制剂在心力衰竭治疗中的临床研究进展，包含中文和英文文献'">点此填入示例</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
const messages = ref([])
const inputText = ref('')
const msgContainer = ref(null)
function autoResize(e) { e.target.style.height='auto'; e.target.style.height=Math.min(e.target.scrollHeight,120)+'px' }
function addMsg(role, content) {
  messages.value.push({role, content})
  nextTick(() => { if(msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight })
}
function sendMessage() {
  const text = inputText.value.trim(); if(!text) return
  inputText.value = ''; addMsg('user', text)
  addMsg('ai', '好的，我来检索相关文献，请稍等...⏳')
  setTimeout(() => {
    const last = messages.value[messages.value.length-1]
    last.content = `已为您整理了一份调研报告框架：
      <div class="outline-card">
        <div class="item"><span class="num">1</span> 研究背景与临床意义</div>
        <div class="item"><span class="num">2</span> 国内外研究现状</div>
        <div class="item"><span class="num">3</span> 关键机制与作用靶点</div>
        <div class="item"><span class="num">4</span> 临床疗效与安全性数据</div>
        <div class="item"><span class="num">5</span> 未来研究方向</div>
      </div>
      <div class="card-actions">
        <button class="btn btn-primary" @click="confirmReport">✅ 满意，生成报告</button>
        <button class="btn btn-outline">✏️ 调整框架</button>
      </div>`
  }, 2000)
}
function confirmReport() {
  addMsg('user', '框架没问题，生成报告吧！')
  addMsg('ai', '正在撰写报告…⏳')
  setTimeout(() => {
    const last = messages.value[messages.value.length-1]
    last.content = `✅ 调研报告已生成完毕！
      <div style="margin-top:8px;padding:12px;background:#e8f4ff;border-radius:8px;border-left:3px solid #0066cc;">
        <b>📁 文件：</b>SGLT2抑制剂心衰研究进展调研报告.docx<br>
        <b>📚 引用：</b>15 篇文献
      </div>
      <div class="card-actions">
        <button class="btn btn-primary">📂 打开文件</button>
        <button class="btn btn-ghost">🔄 重新调研</button>
      </div>`
  }, 3000)
}
</script>

<style scoped>
.chat-panel{flex:1;display:flex;flex-direction:column;background:#f7f8fa;min-width:0}
.chat-header{padding:16px 20px 12px;border-bottom:1px solid #e8e8ec;background:#fff;flex-shrink:0}
.chat-header h2{font-size:16px;font-weight:600;color:#1a1a2e;display:flex;align-items:center;gap:8px}
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
.input-area{padding:12px 16px 14px;background:#fff;border-top:1px solid #e8e8ec;flex-shrink:0}
.input-row{display:flex;gap:10px;align-items:flex-end}
.input-row textarea{flex:1;border:1px solid #ddd;border-radius:10px;padding:10px 14px;font-size:14px;font-family:inherit;resize:none;height:44px;line-height:1.5;outline:none}
.input-row textarea:focus{border-color:#0066cc}
.input-row .send-btn{width:44px;height:44px;border-radius:10px;border:none;background:#0066cc;color:#fff;font-size:18px;cursor:pointer;display:flex;align-items:center;justify-content:center}
.input-row .send-btn:disabled{background:#ccc;cursor:not-allowed}
.input-hint{font-size:12px;color:#aaa;margin-top:6px;padding-left:4px;display:flex;gap:14px;align-items:center}
::-webkit-scrollbar{width:6px}
::-webkit-scrollbar-thumb{background:#ddd;border-radius:3px}
</style>
