<template>
  <div class="app-window">
    <div class="titlebar">
      <div class="titlebar-left">
        <span class="titlebar-dot red"></span>
        <span class="titlebar-dot yellow"></span>
        <span class="titlebar-dot green"></span>
        <span class="titlebar-title">医语 v0.1</span>
      </div>
      <div class="titlebar-right">— □ ×</div>
    </div>
    <div class="main-body">
      <Sidebar />
      <router-view />
    </div>
  </div>
</template>

<script setup>
import Sidebar from './components/Sidebar.vue'
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body {
  font-family: -apple-system, '微软雅黑', 'Segoe UI', sans-serif;
  background: #f0f2f5;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  user-select: none;
}
#app { width: 100%; display: flex; justify-content: center; }
.app-window {
  width: 92vw;
  max-width: 1400px;
  height: 88vh;
  max-height: 900px;
  min-width: 800px;
  min-height: 500px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3), 0 0 0 1px rgba(0,0,0,0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
}
.titlebar {
  height: 36px;
  background: #2b2b2b;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  flex-shrink: 0;
}
.titlebar-left { display: flex; align-items: center; gap: 8px; }
.titlebar-dot {
  width: 12px; height: 12px; border-radius: 50%;
  display: inline-block;
}
.titlebar-dot.red { background: #ff5f56; }
.titlebar-dot.yellow { background: #ffbd2e; }
.titlebar-dot.green { background: #27c93f; }
.titlebar-title { color: #ccc; font-size: 13px; margin-left: 8px; letter-spacing: 0.3px; }
.titlebar-right { color: #888; font-size: 13px; }
.main-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}
@media (max-width: 820px) {
  .app-window { width: 100vw; max-width: none; height: 100vh; max-height: none; min-width: 0; border-radius: 0; }
}
</style>
