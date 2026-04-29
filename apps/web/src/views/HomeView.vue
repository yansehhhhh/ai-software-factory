<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { fetchHealth } from "@/api/health";
import { clearLogs, fetchLogs, fetchResult, fetchWorkflowStatus } from "@/api/workflow";
import { startDiscussion, chatDiscussion, confirmDiscussion } from "@/api/discussion";

import Sidebar from "@/components/Sidebar.vue";
import Topbar from "@/components/Topbar.vue";
import RequirementInput from "@/components/RequirementInput.vue";
import DiscussionPanel from "@/components/DiscussionPanel.vue";
import AgentTable from "@/components/AgentTable.vue";
import DesignResult from "@/components/DesignResult.vue";

const requestText = ref("");
const health = ref(null);
const status = ref(null);
const logs = ref([]);
const result = ref({ available: false });
const pageError = ref("");

const discussionId = ref("");
const discussionMessages = ref([]);
const discussionComplete = ref(false);
const discussionLoading = ref(false);
const activeSideTab = ref("discussion");

const currentPage = ref("home");
let pollTimer = null;

const examples = computed(() => status.value?.examples || []);
const agents = computed(() => status.value?.agents || []);
const progressPercent = computed(() => status.value?.progress || 0);
const recognizedInfoCount = computed(() => discussionMessages.value.filter((message) => message.role === "user").length);
const workflowSteps = computed(() => {
  const backendSteps = status.value?.steps || [];
  const inputStep = {
    index: 0,
    title: "需求输入",
    status: backendSteps.length > 0 ? "success" : "running",
    description: backendSteps.length > 0 ? "已完成" : "进行中"
  };

  return [
    inputStep,
    ...backendSteps.map((step) => ({
      index: step.index,
      title: step.title,
      status: step.status,
      description: stepDescription(step.status)
    }))
  ];
});
const runningAgent = computed(() => agents.value.find((agent) => agent.status === "running"));

function stepDescription(stepStatus) {
  if (stepStatus === "success") return "已完成";
  if (stepStatus === "running") return "进行中";
  if (stepStatus === "error") return "失败";
  return "等待中";
}

async function refreshAll() {
  try {
    const [nextStatus, nextLogs, nextResult] = await Promise.all([
      fetchWorkflowStatus(),
      fetchLogs(),
      fetchResult()
    ]);
    status.value = nextStatus;
    logs.value = nextLogs;
    result.value = nextResult;
    pageError.value = "";
  } catch (error) {
    pageError.value = "无法连接后端编排接口，请先启动后端服务。";
  }
}

async function handleStartDiscussion() {
  if (requestText.value.trim().length === 0) return;

  discussionLoading.value = true;
  pageError.value = "";
  try {
    const response = await startDiscussion(requestText.value);
    discussionId.value = response.discussionId;
    discussionMessages.value = response.history;
    discussionComplete.value = false;
    activeSideTab.value = "discussion";
  } catch (error) {
    pageError.value = error.response?.data?.message || "无法开始需求讨论，请稍后重试。";
  } finally {
    discussionLoading.value = false;
  }
}

async function handleSendMessage(message) {
  if (!discussionId.value || message.trim().length === 0) return;

  discussionLoading.value = true;
  try {
    const response = await chatDiscussion(discussionId.value, message);
    discussionMessages.value = response.history;
    discussionComplete.value = response.isComplete;
    activeSideTab.value = "discussion";
  } catch (error) {
    pageError.value = error.response?.data?.message || "发送消息失败。";
  } finally {
    discussionLoading.value = false;
  }
}

async function handleConfirm() {
  if (!discussionId.value) return;

  discussionLoading.value = true;
  try {
    const response = await confirmDiscussion(discussionId.value);
    status.value = response;
    discussionId.value = "";
    discussionMessages.value = [];
    discussionComplete.value = false;
    activeSideTab.value = "logs";
    await refreshAll();
  } catch (error) {
    pageError.value = error.response?.data?.message || "确认讨论失败。";
  } finally {
    discussionLoading.value = false;
  }
}

function handleNavigate(page) {
  currentPage.value = page;
}

function clearDiscussionPanel() {
  discussionId.value = "";
  discussionMessages.value = [];
  discussionComplete.value = false;
}

async function clearLogPanel() {
  try {
    await clearLogs();
    logs.value = [];
  } catch (error) {
    pageError.value = "清空日志失败。";
  }
}

function openLink(url) {
  if (url) {
    window.open(url, "_blank", "noopener,noreferrer");
  }
}

function historyRoleLabel(role) {
  return role === "ai" ? "[AI]" : "[用户]";
}

onMounted(async () => {
  try {
    health.value = await fetchHealth();
  } catch (error) {
    health.value = null;
  }

  await refreshAll();
  pollTimer = window.setInterval(refreshAll, 3000);
});

onBeforeUnmount(() => {
  if (pollTimer) {
    window.clearInterval(pollTimer);
  }
});
</script>

<template>
  <div class="app-layout">
    <Sidebar initial-active-item="home" @navigate="handleNavigate" />

    <div class="main-shell">
      <Topbar />

      <main class="dashboard-grid">
        <section class="workspace-column">
          <RequirementInput
            v-model:request-text="requestText"
            :examples="examples"
            :loading="discussionLoading"
            :disabled="false"
            @start="handleStartDiscussion"
          />

          <p v-if="pageError" class="error-line">{{ pageError }}</p>

          <div class="discussion-panel-shell">
            <DiscussionPanel
              :messages="discussionMessages"
              :discussion-id="discussionId"
              :is-complete="discussionComplete"
              :loading="discussionLoading"
              @send="handleSendMessage"
              @confirm="handleConfirm"
              @clear="clearDiscussionPanel"
            />
          </div>

          <section class="workflow-panel">
            <div class="section-title-row">
              <h2>执行流程</h2>
            </div>

            <div class="workflow-stepper">
              <div
                v-for="step in workflowSteps"
                :key="step.title"
                class="step-item"
                :data-status="step.status"
              >
                <div class="step-index">{{ step.index }}</div>
                <div class="step-copy">
                  <strong>{{ step.title }}</strong>
                  <span>{{ step.description }}</span>
                </div>
              </div>
            </div>

            <div class="summary-grid">
              <article class="summary-card status-box">
                <div class="summary-icon blue">〰</div>
                <div>
                  <span>当前任务状态</span>
                  <strong>{{ status?.status === 'success' ? '已完成' : status?.status === 'error' ? '执行失败' : '需求讨论中' }}</strong>
                  <small>{{ status?.designProgressMessage || 'AI 正在与您确认需求细节' }}</small>
                </div>
              </article>

              <article class="summary-card">
                <div class="summary-icon indigo">▦</div>
                <div>
                  <span>当前阶段</span>
                  <strong>{{ status?.currentStage || '需求讨论' }}</strong>
                  <small>{{ runningAgent ? `${runningAgent.name} 执行中` : '等待阶段启动' }}</small>
                </div>
              </article>

              <article class="summary-card">
                <div class="summary-icon light">◷</div>
                <div>
                  <span>预计完成时间</span>
                  <strong>{{ status?.estimatedCompletion || '--:--' }}</strong>
                  <small>预计剩余时间</small>
                </div>
              </article>

              <article class="summary-card progress-box">
                <div class="progress-circle">
                  <span>{{ progressPercent || 72 }}%</span>
                </div>
                <div>
                  <span>需求完整度</span>
                  <strong>{{ progressPercent || 72 }}%</strong>
                  <small>已识别 {{ recognizedInfoCount }}/20 项关键信息</small>
                </div>
              </article>
            </div>
          </section>
        </section>

        <aside class="sidebar-column">
          <section class="side-card log-card fixed-log-card">
            <div class="side-card-head">
              <div>
                <h2>实时日志</h2>
              </div>
              <button
                v-if="activeSideTab === 'logs'"
                type="button"
                class="small-ghost"
                @click="clearLogPanel"
              >
                清空
              </button>
            </div>

            <div class="log-tabs">
              <button :data-active="activeSideTab === 'discussion'" @click="activeSideTab = 'discussion'">需求讨论</button>
              <button :data-active="activeSideTab === 'logs'" @click="activeSideTab = 'logs'">系统日志</button>
            </div>

            <div v-if="activeSideTab === 'discussion'" class="log-feed">
              <div v-for="(message, index) in discussionMessages" :key="`history-${index}`" class="log-line discussion-line">
                <span>{{ `10:${`${15 + index * 2}`.padStart(2, '0')}` }}</span>
                <strong>{{ historyRoleLabel(message.role) }}</strong>
                <em>{{ message.content }}</em>
              </div>
              <p v-if="discussionMessages.length === 0" class="empty-tip">讨论进行中...</p>
            </div>

            <div v-else class="log-feed">
              <div v-for="entry in logs" :key="`${entry.time}-${entry.agent}-${entry.message}`" class="log-line">
                <span>{{ entry.time }}</span>
                <strong>[{{ entry.agent }}]</strong>
                <em>{{ entry.message }}</em>
              </div>
              <p v-if="logs.length === 0" class="empty-tip">暂无日志</p>
            </div>
          </section>

          <AgentTable :agents="agents" />
          <DesignResult :result="result" @open-link="openLink" />
        </aside>
      </main>

      <footer class="page-footer">© 2025 AI Software Factory. All rights reserved.</footer>
    </div>
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
  background: #f5f7fb;
}

.main-shell {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0 18px 14px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
  align-items: start;
}

.workspace-column,
.sidebar-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.sidebar-column {
  align-self: start;
  position: sticky;
  top: 14px;
  max-height: calc(100vh - 14px);
  overflow-y: auto;
}

.sidebar-column::-webkit-scrollbar {
  display: none;
}
.sidebar-column {
  -ms-overflow-style: none;  /* IE and Edge */
  scrollbar-width: none;  /* Firefox */
}



.workflow-panel,
.side-card,
.summary-card {
  background: #ffffff;
  border: 1px solid #edf1f7;
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(15, 23, 42, 0.04);
}

.workflow-panel {
  padding: 18px 20px;
}

.section-title-row h2 {
  margin: 0;
  color: #111827;
  font-size: 16px;
}

.workflow-stepper {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 8px;
  margin-top: 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid #edf1f7;
}

.step-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-width: 0;
}

.step-item::after {
  content: "";
  flex: 1;
  height: 2px;
  background: #e5eaf1;
  margin-top: 14px;
}

.step-item:last-child::after {
  display: none;
}

.step-index {
  width: 30px;
  height: 30px;
  border-radius: 999px;
  background: #eef2f7;
  color: #111827;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}

.step-item[data-status="success"] .step-index {
  background: #dcfce7;
  color: #16a34a;
}

.step-item[data-status="running"] .step-index {
  background: #2563eb;
  color: #fff;
}

.step-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.step-copy strong {
  color: #111827;
  font-size: 15px;
  white-space: nowrap;
}

.step-copy span {
  color: #7c8798;
  font-size: 13px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.summary-card {
  padding: 18px 16px;
  display: flex;
  align-items: center;
  gap: 14px;
}

.summary-card span {
  display: block;
  color: #7c8798;
  font-size: 13px;
  margin-bottom: 8px;
}

.summary-card strong {
  display: block;
  color: #165dff;
  font-size: 16px;
  margin-bottom: 6px;
}

.summary-card small {
  color: #7c8798;
  font-size: 12px;
}

.summary-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.summary-icon.blue {
  background: #eef5ff;
  color: #2563eb;
}

.summary-icon.indigo {
  background: #eef2ff;
  color: #4f46e5;
}

.summary-icon.light {
  background: #f8fafc;
  color: #64748b;
}

.progress-box {
  justify-content: flex-start;
}

.progress-circle {
  width: 72px;
  height: 72px;
  border-radius: 999px;
  background: conic-gradient(#2563eb 0deg, #2563eb 259deg, #dbeafe 259deg, #dbeafe 360deg);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.progress-circle span {
  width: 54px;
  height: 54px;
  border-radius: 999px;
  background: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #2563eb;
  font-size: 14px;
  font-weight: 700;
  margin: 0;
}

.side-card {
  padding: 18px;
}

.fixed-log-card {
  display: flex;
  flex-direction: column;
  min-height: 320px;
  max-height: 320px;
}

.side-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.side-card-head h2 {
  margin: 0;
  color: #111827;
  font-size: 16px;
}

.small-ghost {
  border: 1px solid #e5eaf1;
  border-radius: 10px;
  background: #fff;
  color: #64748b;
  font-size: 12px;
  padding: 6px 12px;
  cursor: pointer;
}

.log-tabs {
  display: flex;
  gap: 22px;
  margin-top: 14px;
  border-bottom: 1px solid #edf1f7;
}

.log-tabs button {
  border: none;
  background: transparent;
  color: #7c8798;
  font-size: 13px;
  font-weight: 600;
  padding: 8px 0 10px;
  position: relative;
  cursor: pointer;
}

.log-tabs button[data-active="true"] {
  color: #165dff;
}

.log-tabs button[data-active="true"]::after {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 2px;
  background: #165dff;
}

.log-feed {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 14px;
  min-height: 0;
  height: 230px;
  overflow-y: auto;
  padding-right: 4px;
}

.log-feed::-webkit-scrollbar {
  width: 6px;
}

.log-feed::-webkit-scrollbar-thumb {
  background: #d5deec;
  border-radius: 999px;
}

.log-feed::-webkit-scrollbar-track {
  background: transparent;
}

.log-line {
  display: grid;
  grid-template-columns: 56px 90px minmax(0, 1fr);
  gap: 8px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.log-line strong {
  color: #2563eb;
  font-weight: 600;
}

.log-line em {
  color: #374151;
  font-style: normal;
}

.discussion-line strong {
  color: #16a34a;
}

.empty-tip {
  color: #94a3b8;
  font-size: 13px;
}

.error-line {
  color: #dc2626;
  font-weight: 600;
  padding: 12px 14px;
  background: #fef2f2;
  border-radius: 12px;
}

.page-footer {
  text-align: center;
  color: #94a3b8;
  font-size: 12px;
  padding: 14px 0 0;
}

@media (max-width: 1400px) {
  .workflow-stepper {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    row-gap: 14px;
  }

  .step-item::after {
    display: none;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1180px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
