<script setup>
const props = defineProps({
  agents: {
    type: Array,
    default: () => []
  }
});

function statusLabel(value) {
  if (value === "running") return "进行中";
  if (value === "success") return "已完成";
  if (value === "error") return "失败";
  return "等待中";
}
</script>

<template>
  <section class="agent-panel">
    <div class="panel-head">
      <h2>Agent 状态</h2>
    </div>

    <div class="agent-table">
      <div v-for="agent in agents" :key="agent.name" class="agent-row">
        <span class="agent-icon" :data-status="agent.status">{{ agent.name?.slice(0, 1) || 'A' }}</span>
        <span class="agent-copy">
          <strong>{{ agent.name }}</strong>
          <small>{{ agent.role }}</small>
        </span>
        <span class="agent-status" :data-status="agent.status">
          {{ statusLabel(agent.status) }}
        </span>
      </div>
    </div>
  </section>
</template>

<style scoped>
.agent-panel {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 300px);
  min-height: 360px;
  max-height: calc(100vh - 300px);
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #edf1f7;
  border-radius: 14px;
  padding: 14px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}

.panel-head {
  margin-bottom: 10px;
}

.panel-head h2 {
  margin: 0;
  color: #111827;
  font-size: 16px;
}

.agent-table {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 4px;
  scrollbar-width: none;
}

.agent-table::-webkit-scrollbar {
  display: none;
}

.agent-row {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  min-height: 58px;
  border-top: 1px solid #f0f3f8;
}

.agent-row:first-child {
  border-top: none;
}

.agent-icon {
  width: 34px;
  height: 34px;
  border-radius: 999px;
  background: #eef2ff;
  color: #4f46e5;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 800;
}

.agent-icon[data-status="success"] {
  background: #dcfce7;
  color: #16a34a;
}

.agent-icon[data-status="running"] {
  background: #fff7ed;
  color: #f97316;
}

.agent-copy {
  min-width: 0;
}

.agent-copy strong,
.agent-copy small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-copy strong {
  color: #111827;
  font-size: 13px;
  font-weight: 600;
}

.agent-copy small {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 11px;
}

.agent-status {
  border-radius: 999px;
  background: #f8fafc;
  color: #64748b;
  padding: 5px 9px;
  font-size: 12px;
  font-weight: 700;
}

.agent-status[data-status="running"] {
  background: #fff7ed;
  color: #f97316;
}

.agent-status[data-status="success"] {
  background: #dcfce7;
  color: #16a34a;
}

.agent-status[data-status="error"] {
  background: #fee2e2;
  color: #ef4444;
}
</style>
