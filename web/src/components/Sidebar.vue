<template>
  <div class="sidebar">
    <div
      class="sidebar-icon"
      :class="{ active: current === 'chat' }"
      @click="$router.push('/')"
      title="对话首页"
    >
      <svg viewBox="0 0 24 24"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z"/></svg>
    </div>
    <div
      class="sidebar-icon"
      :class="{ active: current === 'ppt' }"
      @click="$router.push('/ppt')"
      title="做PPT"
    >
      <svg viewBox="0 0 24 24"><path d="M6 2h8l6 6v12c0 1.1-.9 2-2 2H6c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2zm7 1.5V9h5.5L13 3.5zM8 13h8v-2H8v2zm0 4h5v-2H8v2z"/></svg>
    </div>
    <div
      class="sidebar-icon"
      :class="{ active: current === 'research' }"
      @click="$router.push('/research')"
      title="课题调研"
    >
      <svg viewBox="0 0 24 24"><path d="M9 5v13l7-6.5L9 5z"/></svg>
    </div>
    <div
      class="sidebar-icon"
      :class="{ active: current === 'docqa' }"
      @click="$router.push('/docqa')"
      title="读文件"
    >
      <svg viewBox="0 0 24 24"><path d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm-1 7V3.5L18.5 9H13z"/></svg>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
const route = useRoute()
const current = computed(() => route.name || 'chat')
</script>

<style scoped>
.sidebar {
  width: 56px;
  background: #1e1e2d;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 16px;
  gap: 4px;
  flex-shrink: 0;
}
.sidebar-icon {
  width: 40px; height: 40px;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #6b6b8d; font-size: 18px;
  cursor: pointer; transition: all 0.15s;
  position: relative;
}
.sidebar-icon:hover { background: rgba(255,255,255,0.06); color: #fff; }
.sidebar-icon.active {
  background: rgba(255,255,255,0.1);
  color: #fff;
}
.sidebar-icon.active::before {
  content: '';
  position: absolute; left: 0; top: 8px; bottom: 8px;
  width: 3px; border-radius: 0 3px 3px 0;
  background: #c00000;
}
.sidebar-icon svg { width: 22px; height: 22px; fill: currentColor; }
</style>
