<script setup>
import { computed, nextTick, ref, watch } from "vue";

const props = defineProps({
  messages: {
    type: Array,
    default: () => []
  },
  discussionId: {
    type: String,
    default: ""
  },
  isComplete: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(["send", "confirm", "clear"]);

const userInput = ref("");
const messagePanel = ref(null);

const canSend = computed(() => userInput.value.trim().length > 0 && !props.loading);
const canConfirm = computed(() => props.messages.length > 2 && !props.loading);
const userMessages = computed(() => props.messages.filter((message) => message.role === "user"));
const completionPercent = computed(() => {
  const count = Math.min(userMessages.value.length * 14, 100);
  return count === 0 ? 8 : count;
});
const recognizedItems = computed(() => {
  const firstUserMessage = props.messages.find((message) => message.role === "user")?.content || "";
  const requirement = firstUserMessage.replace(/^我的需求是：/, "");
  return [
    { label: `系统名称：${requirement || "待补充"}`, done: requirement.length > 0 },
    { label: "核心功能：待进一步确认", done: userMessages.value.length >= 1 },
    { label: "报告内容：待进一步确认", done: userMessages.value.length >= 2 },
    { label: "输出格式：待进一步确认", done: userMessages.value.length >= 3 },
    { label: "用户登录：待补充", done: userMessages.value.length >= 4 },
    { label: "权限管理：待补充", done: userMessages.value.length >= 5 },
    { label: "日志来源：待补充", done: userMessages.value.length >= 6 },
    { label: "实时分析：待确认", done: userMessages.value.length >= 7 },
    { label: "通知方式：待确认", done: userMessages.value.length >= 8 },
    { label: "其他需求：待补充", done: userMessages.value.length >= 9 }
  ];
});

function sendMessage() {
  if (canSend.value) {
    emit("send", userInput.value);
    userInput.value = "";
  }
}

function confirmAndGenerate() {
  if (canConfirm.value) {
    emit("confirm");
  }
}

function clearDiscussion() {
  userInput.value = "";
  emit("clear");
}

function roleLabel(role) {
  return role === "ai" ? "🤖" : "🧑";
}

watch(() => props.messages.length, async () => {
  await nextTick();
  if (messagePanel.value) {
    messagePanel.value.scrollTop = messagePanel.value.scrollHeight;
  }
});
</script>

<template>
  <section class="discussion-panel">
    <div class="discussion-header">
      <div class="discussion-title-row">
        <div>
          <h2>需求讨论</h2>
          <p>AI 与您共同完善需求</p>
        </div>
      </div>
      <button type="button" class="clear-button" @click="clearDiscussion">🗑 清空对话</button>
    </div>

    <div class="discussion-body">
      <div class="chat-column">
        <div ref="messagePanel" class="message-list">
          <div v-if="messages.length === 0" class="empty-message">
            您好！我将帮助您梳理和完善需求，以确保最终生成的应用更符合您的期望。
          </div>

          <div
            v-for="(msg, index) in messages"
            :key="index"
            class="message-row"
            :data-role="msg.role"
          >
            <div class="message-avatar">{{ roleLabel(msg.role) }}</div>
            <div class="message-bubble">
              <div class="message-content">{{ msg.content }}</div>
              <span class="message-time">{{ 10 + Math.floor(index / 2) }}:{{ `${15 + (index % 5) * 7}`.padStart(2, '0') }}</span>
            </div>
          </div>
        </div>

        <div class="discussion-input-row">
          <textarea
            v-model="userInput"
            placeholder="输入您的回复..."
            rows="2"
            :disabled="loading || isComplete"
            @keydown.enter.exact.prevent="sendMessage"
          ></textarea>
          <button class="send-button" :disabled="!canSend" @click="sendMessage">发送</button>
        </div>
      </div>

      <aside class="discussion-progress-card">
        <div class="progress-head">
          <h3>讨论进度</h3>
          <strong>{{ userMessages.length }}/20 已识别</strong>
        </div>

        <div class="progress-bar">
          <span :style="{ width: `${completionPercent}%` }"></span>
        </div>

        <div class="progress-list">
          <h4>已识别关键信息</h4>
          <ul>
            <li v-for="item in recognizedItems" :key="item.label">
              <span class="list-arrow">▸</span>
              <span class="list-text">{{ item.label }}</span>
              <span v-if="item.done" class="check-mark">✓</span>
            </li>
          </ul>
        </div>

        <button class="confirm-button" :disabled="!canConfirm" @click="confirmAndGenerate">
          结束讨论并确认需求
        </button>
        <p class="confirm-hint">确认后将进入下一阶段：需求分析</p>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.discussion-panel {
  background: #ffffff;
  border: 1px solid #edf1f7;
  border-radius: 16px;
  padding: 18px 20px 20px;
  box-shadow: 0 8px 30px rgba(15, 23, 42, 0.04);
}

.discussion-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.discussion-title-row {
  display: flex;
  align-items: center;
}

.discussion-title-row h2 {
  margin: 0;
  color: #111827;
  font-size: 16px;
}

.discussion-title-row p {
  margin: 4px 0 0;
  color: #7c8798;
  font-size: 13px;
}

.clear-button {
  border: 1px solid #e5eaf1;
  border-radius: 10px;
  background: #ffffff;
  color: #64748b;
  font-size: 13px;
  padding: 8px 12px;
  cursor: pointer;
}

.discussion-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
  align-items: start;
}

.chat-column {
  min-width: 0;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  height: 350px;
  overflow: auto;
  padding-right: 6px;
  border: 1px solid #edf1f7;
  border-radius: 14px;
  padding: 18px;
  background: #ffffff;
}

.message-list::-webkit-scrollbar {
  width: 6px;
}

.message-list::-webkit-scrollbar-thumb {
  background: #d5deec;
  border-radius: 999px;
}

.empty-message {
  color: #64748b;
  font-size: 14px;
  line-height: 1.7;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.message-row[data-role="user"] {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 34px;
  height: 34px;
  border-radius: 999px;
  background: #eef5ff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.message-bubble {
  max-width: 76%;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.message-row[data-role="ai"] .message-content {
  background: #eef5ff;
  color: #0f172a;
}

.message-row[data-role="user"] .message-content {
  background: #dcfce7;
  color: #0f172a;
}

.message-content {
  border-radius: 14px;
  padding: 14px 16px;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.message-time {
  color: #94a3b8;
  font-size: 12px;
}

.message-row[data-role="user"] .message-time {
  text-align: right;
}

.discussion-input-row {
  margin-top: 16px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 110px;
  gap: 12px;
}

.discussion-input-row textarea {
  width: 100%;
  resize: none;
  border: 1px solid #dfe7f3;
  border-radius: 12px;
  padding: 16px 14px;
  font-size: 14px;
  line-height: 1.6;
  outline: none;
  box-sizing: border-box;
  min-height: 64px;
}

.send-button,
.confirm-button {
  border: none;
  border-radius: 12px;
  background: linear-gradient(180deg, #2f7cff 0%, #165dff 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.send-button:disabled,
.confirm-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.discussion-progress-card {
  border: 1px solid #edf1f7;
  border-radius: 14px;
  background: #ffffff;
  padding: 16px;
  display: flex;
  flex-direction: column;
  align-self: start;
}

.progress-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.progress-head h3,
.progress-list h4 {
  margin: 0;
  color: #111827;
  font-size: 16px;
}

.progress-head strong {
  color: #64748b;
  font-size: 13px;
}

.progress-bar {
  height: 6px;
  border-radius: 999px;
  background: #e5edf7;
  overflow: hidden;
  margin: 14px 0 18px;
}

.progress-bar span {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #2f7cff 0%, #165dff 100%);
}

.progress-list ul {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.progress-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  color: #374151;
  font-size: 13px;
  line-height: 1.5;
}

.list-arrow {
  color: #475569;
}

.list-text {
  flex: 1;
}

.check-mark {
  color: #22c55e;
  font-weight: 700;
}

.confirm-button {
  width: 100%;
  min-height: 46px;
  margin-top: 18px;
}

.confirm-hint {
  color: #7c8798;
  font-size: 12px;
  text-align: center;
  margin: 10px 0 0;
}

@media (max-width: 1280px) {
  .discussion-body {
    grid-template-columns: 1fr;
  }
}
</style>
