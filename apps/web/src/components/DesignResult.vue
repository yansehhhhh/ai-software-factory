<script setup>
const props = defineProps({
  result: {
    type: Object,
    default: () => ({ available: false })
  }
});

const emit = defineEmits(["openLink"]);

function openLink(url) {
  if (url && props.result?.available === true) {
    emit("openLink", url);
  }
}
</script>

<template>
  <section class="result-panel">
    <div class="panel-head">
      <h2>结果与操作</h2>
      <p>任务完成后可进行以下操作</p>
    </div>

    <div class="result-actions">
      <button type="button" :disabled="!result?.available" @click="openLink(result.projectUrl)">
        <span class="action-icon blue">🖥</span>
        <strong>打开生成的项目</strong>
        <span>启动前端应用</span>
      </button>
      <button type="button" :disabled="!result?.available" @click="openLink(result.reportUrl)">
        <span class="action-icon green">📄</span>
        <strong>查看测试报告</strong>
        <span>Playwright 报告</span>
      </button>
      <button type="button" :disabled="!result?.available" @click="openLink(result.zipUrl)">
        <span class="action-icon purple">⬇</span>
        <strong>下载代码包</strong>
        <span>ZIP 压缩包</span>
      </button>
    </div>
  </section>
</template>

<style scoped>
.result-panel {
  background: #ffffff;
  border: 1px solid #edf1f7;
  border-radius: 16px;
  padding: 18px;
  box-shadow: 0 8px 30px rgba(15, 23, 42, 0.04);
}

.panel-head h2 {
  margin: 0;
  color: #111827;
  font-size: 16px;
}

.panel-head p {
  margin: 6px 0 0;
  color: #7c8798;
  font-size: 13px;
}

.result-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.result-actions button {
  border: 1px solid #edf1f7;
  border-radius: 14px;
  background: #f8fbff;
  min-height: 132px;
  padding: 18px 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  text-align: center;
  cursor: pointer;
}

.result-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.action-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-style: normal;
}

.action-icon.blue {
  background: #eaf2ff;
  color: #2563eb;
}

.action-icon.green {
  background: #eafbf1;
  color: #16a34a;
}

.action-icon.purple {
  background: #f4ecff;
  color: #9333ea;
}

.result-actions strong {
  color: #111827;
  font-size: 16px;
}

.result-actions span:last-child {
  color: #7c8798;
  font-size: 13px;
}

@media (max-width: 1180px) {
  .result-actions {
    grid-template-columns: 1fr;
  }
}
</style>
