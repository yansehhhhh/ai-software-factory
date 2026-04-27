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
      <h2>参与 Agent 列表</h2>
    </div>

    <div class="agent-table">
      <div class="agent-row header">
        <span>Agent 名称</span>
        <span>角色</span>
        <span>状态</span>
      </div>
      <div v-for="agent in agents" :key="agent.name" class="agent-row">
        <span class="agent-name">{{ agent.name }}</span>
        <span class="agent-role">{{ agent.role }}</span>
        <span class="agent-status" :data-status="agent.status">
          <i></i>
          {{ statusLabel(agent.status) }}
        </span>
      </div>
    </div>
  </section>
</template>

<style scoped>
.agent-panel {
  background: #ffffff;
  border: 1px solid #edf1f7;
  border-radius: 16px;
  padding: 18px 18px 10px;
  box-shadow: 0 8px 30px rgba(15, 23, 42, 0.04);
}

.panel-head {
  margin-bottom: 12px;
}

.panel-head h2 {
  margin: 0;
  color: #111827;
  font-size: 16px;
}

.agent-table {
  display: flex;
  flex-direction: column;
}

.agent-row {
  display: grid;
  grid-template-columns: 1.2fr 1fr 90px;
  gap: 12px;
  align-items: center;
  min-height: 44px;
  border-top: 1px solid #f0f3f8;
  color: #475569;
  font-size: 13px;
}

.agent-row.header {
  border-top: none;
  color: #94a3b8;
  font-size: 12px;
  min-height: 34px;
}

.agent-name {
  color: #111827;
  font-weight: 500;
}

.agent-role {
  color: #64748b;
}

.agent-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}

.agent-status i {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #cbd5e1;
}

.agent-status[data-status="running"] {
  color: #16a34a;
}

.agent-status[data-status="running"] i {
  background: #22c55e;
}

.agent-status[data-status="success"] {
  color: #2563eb;
}

.agent-status[data-status="success"] i {
  background: #2563eb;
}

.agent-status[data-status="error"] {
  color: #ef4444;
}

.agent-status[data-status="error"] i {
  background: #ef4444;
}
</style>
