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
        <h2>输入需求</h2>
      </div>
      <p>描述您想要构建的应用或功能</p>
    </div>

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

    <div class="quick-examples" aria-label="快速示例">
      <span>快速示例：</span>
      <button v-for="example in examples" :key="example" type="button" @click="pickExample(example)">
        {{ example }}
      </button>
    </div>
  </section>
</template>

<style scoped>
.input-panel {
  background: #ffffff;
  border: 1px solid #edf1f7;
  border-radius: 16px;
  padding: 18px 20px;
  box-shadow: 0 8px 30px rgba(15, 23, 42, 0.04);
}

.panel-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.panel-title-main {
  display: flex;
  align-items: center;
}

.panel-title-row h2 {
  margin: 0;
  color: #111827;
  font-size: 16px;
}

.panel-title-row p {
  margin: 0;
  color: #9aa4b2;
  font-size: 13px;
}

.input-main-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 18px;
  align-items: center;
}

.textarea-shell {
  position: relative;
}

.textarea-shell textarea {
  width: 100%;
  min-height: 120px;
  resize: none;
  border: 1px solid #dfe7f3;
  border-radius: 12px;
  background: #ffffff;
  padding: 16px 18px 28px;
  font-size: 15px;
  line-height: 1.8;
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
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.primary-action {
  width: 100%;
  min-height: 56px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(180deg, #2f7cff 0%, #165dff 100%);
  color: #fff;
  font-size: 16px;
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

.quick-examples {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.quick-examples span {
  color: #7c8798;
  font-size: 13px;
}

.quick-examples button {
  border: 1px solid #eef2f7;
  border-radius: 999px;
  background: #f8fafc;
  color: #475569;
  font-size: 13px;
  padding: 8px 14px;
  cursor: pointer;
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
