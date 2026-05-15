<script setup>
import { computed } from "vue";

const props = defineProps({
  requestText: {
    type: String,
    required: true
  },
  examples: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(["update:requestText", "start"]);

const canRun = computed(() => props.requestText.trim().length > 0 && !props.loading && !props.disabled);
const textLength = computed(() => props.requestText.length);

function pickExample(example) {
  emit("update:requestText", `做一个${example}，支持核心页面、后端接口和自动化测试报告。`);
}

function startDiscussion() {
  if (canRun.value) {
    emit("start");
  }
}
</script>

<template>
  <section class="input-panel">
    <div class="panel-title-row">
      <div class="panel-title-main">
        <h2>需求输入入口</h2>
        <span>起点</span>
      </div>
    </div>

    <div class="step-title-line">
      <strong>1</strong>
      <span>输入需求</span>
    </div>
    <p class="input-helper">用自然语言描述您想构建的系统或功能</p>

    <div class="input-main-row">
      <div class="textarea-shell">
        <textarea
          :value="requestText"
          @input="$emit('update:requestText', $event.target.value)"
          placeholder="做一个AI质检助手"
          rows="5"
        ></textarea>
        <span class="text-count">{{ textLength }}/1000</span>
      </div>

      <div class="action-column">
        <button class="primary-action" :disabled="!canRun" @click="startDiscussion">
          <span class="action-icon">💬</span>
          <span v-if="loading">开始中...</span>
          <span v-else>开始需求讨论</span>
        </button>
        <span class="action-hint">预计耗时 3 ~ 5 分钟</span>
      </div>
    </div>

    <p class="input-tip">提示：输入越详细，AI 生成的产物越准确</p>
  </section>
</template>

<style scoped>
.input-panel {
  background: #ffffff;
  border: 1px solid #edf1f7;
  border-radius: 14px;
  padding: 14px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}

.panel-title-row {
  margin-bottom: 12px;
}

.panel-title-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.panel-title-row h2 {
  margin: 0;
  color: #111827;
  font-size: 16px;
}

.panel-title-main span {
  border-radius: 999px;
  background: #dcfce7;
  color: #16a34a;
  padding: 4px 9px;
  font-size: 12px;
  font-weight: 700;
}

.step-title-line {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.step-title-line strong {
  width: 24px;
  height: 24px;
  border-radius: 999px;
  background: #2563eb;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
}

.step-title-line span {
  color: #111827;
  font-size: 14px;
  font-weight: 700;
}

.input-helper,
.input-tip {
  margin: 0 0 10px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.input-main-row {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.textarea-shell {
  position: relative;
}

.textarea-shell textarea {
  width: 100%;
  min-height: 132px;
  resize: none;
  border: 1px solid #dfe7f3;
  border-radius: 10px;
  background: #fbfdff;
  padding: 14px 14px 28px;
  font-size: 13px;
  line-height: 1.7;
  color: #111827;
  box-sizing: border-box;
  outline: none;
}

.textarea-shell textarea::placeholder {
  color: #cbd5e1;
}

.text-count {
  position: absolute;
  right: 14px;
  bottom: 10px;
  color: #9aa4b2;
  font-size: 12px;
}

.action-column {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: center;
  gap: 10px;
}

.primary-action {
  width: 100%;
  min-height: 42px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #4f46e5 0%, #2563eb 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  cursor: pointer;
  box-shadow: 0 14px 28px rgba(37, 99, 235, 0.24);
}

.primary-action:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  box-shadow: none;
}

.action-icon {
  font-size: 15px;
}

.action-hint {
  color: #7c8798;
  font-size: 13px;
}


@media (max-width: 1180px) {
  .input-main-row {
    grid-template-columns: 1fr;
  }

  .action-column {
    align-items: flex-start;
  }

  .primary-action {
    max-width: 280px;
  }
}
</style>
