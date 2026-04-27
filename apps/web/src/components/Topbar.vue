<template>
  <header class="topbar">
    <span class="status-dot"></span>
    <span class="status-text">在线</span>
  </header>
</template>

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  height: 34px;
  padding: 8px 6px 0;
  color: #16a34a;
  font-size: 13px;
  font-weight: 600;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: #22c55e;
  margin-right: 8px;
}

.status-text {
  color: #16a34a;
}
</style>
