<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { fetchHealth } from "@/api/health";
import { approveCurrentStage, clearLogs, fetchLogs, fetchProjects, fetchResult, fetchWorkflowStatus, recoverProject, retryFailedStage, runOpenSpecAction, submitStageRevision } from "@/api/workflow";
import { startDiscussion, chatDiscussion, confirmDiscussion } from "@/api/discussion";
import { artifactPath } from "@/api/artifact";
import { previewGeneratedProject } from "@/api/generatedProject";

import RequirementInput from "@/components/RequirementInput.vue";
import AgentTable from "@/components/AgentTable.vue";
import DesignResult from "@/components/DesignResult.vue";
import StageReviewPanel from "@/components/StageReviewPanel.vue";

const requestText = ref("");
const health = ref(null);
const status = ref(null);
const logs = ref([]);
const result = ref({ available: false });
const pageError = ref("");
const projects = ref([]);
const selectedProject = ref("");
const projectLoading = ref(false);
const isNewProjectMode = ref(false);

const discussionId = ref("");
const discussionMessages = ref([]);
const discussionComplete = ref(false);
const discussionLoading = ref(false);
const activeSideTab = ref("discussion");

const currentPage = ref("home");
let pollTimer = null;

const examples = computed(() => status.value?.examples || []);
const stepIconMap = {
  "需求讨论": "需",
  "需求产物生成": "文",
  "UI 设计": "UI",
  "架构设计": "架",
  "接口设计": "API",
  "数据库设计": "DB",
  "前端开发": "FE",
  "后端开发": "BE",
  "开发联调": "联",
  "E2E验收测试": "E2E",
  "测试验证": "QA"
};
const agents = computed(() => status.value?.agents || []);
const projectLabel = computed(() => result.value?.projectName || selectedProject.value || status.value?.requirement || "未选择项目");
const projectOptions = computed(() => {
  const seen = new Set();
  return (projects.value || [])
    .filter((project) => project?.name)
    .filter((project) => {
      if (seen.has(project.name)) return false;
      seen.add(project.name);
      return true;
    });
});
const recognizedInfoCount = computed(() => discussionMessages.value.filter((message) => message.role === "user").length);
const workflowSteps = computed(() => {
  const backendSteps = status.value?.steps || [];
  return backendSteps.map((step) => ({
    index: step.index,
    title: step.title,
    status: step.status,
    description: step.detail || stepDescription(step.status),
    icon: stepIconMap[step.title] || "○",
    isCurrent: step.key && stageReview.value?.stageKey === step.key && step.status === "awaiting_review"
  }));
});
const runningAgent = computed(() => agents.value.find((agent) => agent.status === "running"));
const stageReview = computed(() => status.value?.review || null);
const revisionState = computed(() => stageReview.value?.revision || null);
const retryingStepIndex = ref(null);
const reviewLoading = ref(false);
const revisionConversationOpen = ref(false);

function canRetryStep(step) {
  return status.value?.status === "error" && step?.status === "error" && step?.title === status.value?.currentStage;
}

function retryButtonLabel(step) {
  return retryingStepIndex.value === step?.index ? "重试中..." : "重试";
}

function isRetryingStep(step) {
  return retryingStepIndex.value === step?.index;
}

function stepDescription(stepStatus) {
  if (stepStatus === "success") return "已完成";
  if (stepStatus === "running") return "进行中";
  if (stepStatus === "error") return "失败";
  if (stepStatus === "awaiting_review") return "待确认";
  return "等待中";
}

async function loadWorkflowSnapshot() {
  const [nextStatus, nextLogs, nextResult] = await Promise.all([
    fetchWorkflowStatus(),
    fetchLogs(),
    fetchResult()
  ]);

  status.value = nextStatus;
  logs.value = nextLogs;
  result.value = nextResult;
  if (nextResult?.projectName) {
    selectedProject.value = nextResult.projectName;
  }
}

async function refreshAll({ force = false, preserveError = false } = {}) {
  if (isNewProjectMode.value && !force) return;

  try {
    await loadWorkflowSnapshot();
    if (!preserveError) {
      pageError.value = "";
    }
  } catch (error) {
    pageError.value = "无法连接后端编排接口，请先启动后端服务。";
  }
}

async function refreshProjects() {
  try {
    projects.value = await fetchProjects();
  } catch (error) {
    projects.value = [];
  }
}

async function handleProjectSelect(event) {
  const projectName = event.target.value;
  selectedProject.value = projectName;
  if (!projectName) return;

  isNewProjectMode.value = false;
  projectLoading.value = true;
  try {
    status.value = await recoverProject(projectName);
    clearDiscussionPanel();
    activeSideTab.value = "discussion";
    await refreshAll({ force: true });
  } catch (error) {
    pageError.value = error.response?.data?.message || "恢复历史项目失败。";
  } finally {
    projectLoading.value = false;
  }
}

async function handleStartDiscussion() {
  if (requestText.value.trim().length === 0) return;

  discussionLoading.value = true;
  pageError.value = "";
  try {
    isNewProjectMode.value = false;
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

async function handleApproveStage() {
  reviewLoading.value = true;
  try {
    status.value = await approveCurrentStage();
    revisionConversationOpen.value = false;
    activeSideTab.value = "logs";
    await refreshAll();
  } catch (error) {
    pageError.value = error.response?.data?.message || "确认阶段失败。";
  } finally {
    reviewLoading.value = false;
  }
}

async function handleStageRevision(feedback) {
  reviewLoading.value = true;
  try {
    status.value = await submitStageRevision(feedback);
    revisionConversationOpen.value = true;
    await refreshAll();
  } catch (error) {
    pageError.value = error.response?.data?.message || "提交修订反馈失败。";
    await refreshAll({ preserveError: true });
  } finally {
    reviewLoading.value = false;
  }
}

function handleStartStageRevision() {
  if (!stageReview.value) return;
  revisionConversationOpen.value = true;
  activeSideTab.value = "discussion";
}

async function handleOpenSpecAction({ action, feedback }) {
  reviewLoading.value = true;
  try {
    status.value = await runOpenSpecAction(action, feedback);
    await refreshAll();
  } catch (error) {
    pageError.value = error.response?.data?.message || "OpenSpec 操作失败。";
    await refreshAll({ preserveError: true });
  } finally {
    reviewLoading.value = false;
  }
}

async function handleRetryStep(step) {
  if (!canRetryStep(step) || isRetryingStep(step)) return;

  retryingStepIndex.value = step.index;
  pageError.value = "";
  try {
    status.value = await retryFailedStage();
    activeSideTab.value = "logs";
    await refreshAll();
  } catch (error) {
    pageError.value = error.response?.data?.message || "重试失败，请稍后再试。";
  } finally {
    retryingStepIndex.value = null;
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

function createEmptyResult() {
  return { available: false };
}

function createInitialStatusSnapshot() {
  const baseStatus = status.value || {};
  const steps = (baseStatus.steps || []).map((step, index) => ({
    ...step,
    status: index === 0 ? "running" : "pending",
    detail: index === 0 ? "进行中" : stepDescription("pending")
  }));
  const agents = (baseStatus.agents || []).map((agent, index) => ({
    ...agent,
    status: index === 0 ? "running" : "pending",
    duration: "--",
    progress: index === 0 ? agent.progress || 0 : 0
  }));

  return {
    ...baseStatus,
    status: "running",
    statusLabel: "进行中",
    currentStage: steps[0]?.title || "需求讨论",
    requirement: "",
    review: null,
    steps,
    agents
  };
}

function resetToNewProject() {
  isNewProjectMode.value = true;
  requestText.value = "";
  status.value = createInitialStatusSnapshot();
  logs.value = [];
  result.value = createEmptyResult();
  selectedProject.value = "";
  pageError.value = "";
  activeSideTab.value = "discussion";
  revisionConversationOpen.value = false;
  clearDiscussionPanel();
}

function handleStartNewProject() {
  resetToNewProject();
}

async function clearLogPanel() {
  try {
    await clearLogs();
    logs.value = [];
  } catch (error) {
    pageError.value = "清空日志失败。";
  }
}

async function openLink(target) {
  if (!target) return;
  if (typeof target === "object" && target.action === "previewGeneratedProject") {
    await openGeneratedProjectPreview(target.path);
    return;
  }
  window.open(target, "_blank", "noopener,noreferrer");
}

async function openGeneratedProjectPreview(path) {
  const normalizedPath = artifactPath(path);
  if (!normalizedPath) return;
  try {
    const preview = await previewGeneratedProject(normalizedPath, result.value?.projectName || selectedProject.value);
    if (preview?.url) {
      window.open(preview.url, "_blank", "noopener,noreferrer");
    }
  } catch (error) {
    pageError.value = error.response?.data?.message || "启动生成项目预览失败。";
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

  await refreshProjects();
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
  <div class="factory-page">
    <header class="factory-topbar">
      <div class="brand-block">
        <div class="brand-logo">A</div>
        <strong>AI Software Factory</strong>
      </div>
      <div class="topbar-divider"></div>
      <label class="project-selector">
        <span class="project-open-icon">↗</span>
        <span class="project-label">项目：</span>
        <select class="project-select" :value="selectedProject" :disabled="projectLoading" @change="handleProjectSelect">
          <option value="" disabled>{{ projectOptions.length ? '选择历史项目' : projectLabel }}</option>
          <option v-for="project in projectOptions" :key="project.id" :value="project.name">
            {{ project.name }}
          </option>
        </select>
        <span class="project-chevron"></span>
      </label>
      <div class="stage-pill">当前阶段：{{ stageReview?.stageTitle || status?.currentStage || '未开始' }}</div>
      <div class="review-pill"><span></span>{{ status?.statusLabel || '未开始' }}</div>
      <div class="next-stage-pill">下一阶段：{{ stageReview?.nextStageTitle || '待工作流推进' }}</div>
      <button type="button" class="start-button" @click="handleStartNewProject">
        Start
      </button>
    </header>

    <div class="main-shell">
      <main class="dashboard-grid">
        <aside class="left-workbench-column">
          <RequirementInput
            v-model:request-text="requestText"
            :examples="examples"
            :loading="discussionLoading"
            :disabled="false"
            @start="handleStartDiscussion"
          />

          <section class="workflow-panel compact-workflow-panel">
            <div class="section-title-row">
              <h2>主流程进度</h2>
            </div>

                <div class="workflow-stepper">
              <div
                v-for="step in workflowSteps"
                :key="step.title"
                class="step-item"
                :data-status="step.status"
              >
                <div class="step-rail-node">
                  <span class="step-index">{{ step.index }}</span>
                </div>
                <div class="step-icon">{{ step.icon }}</div>
                <div class="step-copy">
                  <strong>{{ step.title }}</strong>
                  <span>{{ step.description }}</span>
                </div>
                <button
                  v-if="step.isCurrent"
                  type="button"
                  class="step-revise-button"
                  @click="handleStartStageRevision"
                >
                  修订
                </button>
                <button
                  v-else-if="canRetryStep(step)"
                  type="button"
                  class="step-retry-button"
                  :disabled="isRetryingStep(step)"
                  @click="handleRetryStep(step)"
                >
                  {{ retryButtonLabel(step) }}
                </button>
              </div>
            </div>
          </section>
        </aside>

        <section class="center-workbench-column">
          <p v-if="pageError" class="error-line">{{ pageError }}</p>

          <StageReviewPanel
            :review="stageReview"
            :revision-conversation-open="revisionConversationOpen"
            :status="status"
            :result="result"
            :loading="reviewLoading"
            :discussion-id="discussionId"
            :discussion-messages="discussionMessages"
            :discussion-complete="discussionComplete"
            :discussion-loading="discussionLoading"
            :logs="logs"
            @approve="handleApproveStage"
            @revise="handleStageRevision"
            @openspec="handleOpenSpecAction"
            @open-artifact="openLink"
            @discussion-send="handleSendMessage"
            @discussion-confirm="handleConfirm"
          />
        </section>

        <aside class="sidebar-column">
          <AgentTable :agents="agents" />

          <section class="side-card revision-status-card">
            <div class="side-card-head">
              <div>
                <h2>OpenSpec 修订状态</h2>
              </div>
            </div>
            <div v-if="revisionState" class="revision-status-list">
              <div>
                <span>Change ID</span>
                <strong>{{ revisionState.changeId || '未创建' }}</strong>
              </div>
              <div>
                <span>Proposal</span>
                <strong>{{ revisionState.proposalStatus }}</strong>
              </div>
              <div>
                <span>Apply</span>
                <strong>{{ revisionState.applyStatus }}</strong>
              </div>
              <div>
                <span>Archive</span>
                <strong>{{ revisionState.archiveStatus }}</strong>
              </div>
              <small>更新时间：{{ revisionState.updatedAt }}</small>
            </div>
            <p v-else class="empty-tip">暂无阶段修订 change。</p>
          </section>

          <section class="side-card log-card fixed-log-card">
            <div class="side-card-head">
              <div>
                <h2>执行日志</h2>
              </div>
              <button
                type="button"
                class="small-ghost"
                @click="clearLogPanel"
              >
                查看全部日志
              </button>
            </div>

            <div class="log-feed compact-log-feed">
              <div v-for="entry in logs.slice(-5)" :key="`${entry.time}-${entry.agent || 'log'}-${entry.message}`" class="log-line">
                <span>{{ entry.time }}</span>
                <em>{{ entry.agent ? `${entry.agent} · ${entry.message}` : entry.message }}</em>
              </div>
              <p v-if="logs.length === 0" class="empty-tip">暂无执行日志。</p>
            </div>
          </section>

          <DesignResult :result="result" @open-link="openLink" @preview-project="openGeneratedProjectPreview" />
        </aside>
      </main>

    </div>
  </div>
</template>

<style scoped>
.factory-page {
  min-height: 100vh;
  background: #f6f8fc;
  color: #111827;
}

.factory-topbar {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 18px;
  min-width: 0;
  min-height: 52px;
  max-width: 100vw;
  overflow: hidden;
  padding: 0 16px;
  background: rgba(255, 255, 255, 0.98);
  border-bottom: 1px solid #edf1f7;
  box-shadow: 0 4px 18px rgba(15, 23, 42, 0.03);
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 238px;
}

.brand-logo {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: linear-gradient(135deg, #5b8cff, #6d5dfc);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 800;
}

.brand-block strong {
  font-size: 16px;
}

.topbar-divider {
  width: 1px;
  height: 26px;
  background: #edf1f7;
}

.project-selector,
.stage-pill,
.next-stage-pill,
.review-pill {
  position: relative;
  min-width: 0;
  overflow: hidden;
  color: #334155;
  padding: 0 0 0 2px;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-selector {
  display: inline-flex;
  align-items: center;
  flex: 1 1 320px;
  max-width: 560px;
  height: 34px;
  border: 1px solid #e5eaf3;
  border-radius: 999px;
  background: #f8fafc;
  padding: 0 34px 0 12px;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.project-selector:hover,
.project-selector:focus-within {
  border-color: #bfdbfe;
  background: #ffffff;
  box-shadow: 0 8px 22px rgba(37, 99, 235, 0.10);
}

.project-label {
  flex: 0 0 auto;
  color: #64748b;
  font-weight: 700;
}

.project-select {
  min-width: 0;
  flex: 1;
  border: 0;
  outline: 0;
  appearance: none;
  background: transparent;
  color: #1e293b;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
  text-overflow: ellipsis;
}

.project-select:disabled {
  color: #94a3b8;
  cursor: wait;
}

.stage-pill,
.next-stage-pill {
  flex: 0 1 220px;
}

.review-pill {
  flex: 0 0 auto;
}

.project-open-icon {
  color: #2563eb;
  margin-right: 5px;
}

.project-chevron {
  position: absolute;
  right: 14px;
  top: 50%;
  display: inline-block;
  width: 7px;
  height: 7px;
  border-right: 2px solid #64748b;
  border-bottom: 2px solid #64748b;
  pointer-events: none;
  transform: translateY(-65%) rotate(45deg);
}

.review-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #f97316;
  font-weight: 700;
}

.review-pill span {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: #f97316;
}

.stage-pill,
.next-stage-pill {
  padding-left: 17px;
}

.stage-pill::before,
.next-stage-pill::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  width: 1px;
  height: 22px;
  background: #edf1f7;
  transform: translateY(-50%);
}

.start-button {
  margin-left: auto;
  border: 0;
  border-radius: 999px;
  background: linear-gradient(135deg, #2563eb, #4f46e5);
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  padding: 0 16px;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.22);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.start-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.28);
}

.start-button:active {
  transform: translateY(0);
}

.main-shell {
  padding: 8px 10px 12px;
  min-width: 0;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 304px minmax(672px, 1fr) 312px;
  gap: 10px;
  align-items: start;
}

.left-workbench-column,
.center-workbench-column,
.sidebar-column {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}

.left-workbench-column,
.sidebar-column {
  align-self: start;
  position: sticky;
  top: 62px;
  max-height: calc(100vh - 72px);
  overflow-y: auto;
}

.center-workbench-column {
  min-height: calc(100vh - 120px);
}

.sidebar-column {
  align-self: start;
}

.left-workbench-column::-webkit-scrollbar,
.sidebar-column::-webkit-scrollbar {
  display: none;
}

.left-workbench-column,
.sidebar-column {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.column-header-card,
.review-hero-card,
.empty-review-card,
.workflow-panel,
.side-card,
.summary-card {
  background: #ffffff;
  border: 1px solid #edf1f7;
  border-radius: 18px;
  box-shadow: 0 12px 34px rgba(15, 23, 42, 0.06);
}

.column-header-card {
  padding: 18px;
  background: linear-gradient(135deg, #eff6ff 0%, #ffffff 56%, #eef2ff 100%);
}

.column-header-card span,
.review-hero-card span {
  display: inline-flex;
  margin-bottom: 8px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.column-header-card strong {
  display: block;
  color: #0f172a;
  font-size: 18px;
  line-height: 1.4;
}

.column-header-card small {
  display: block;
  margin-top: 8px;
  color: #64748b;
  line-height: 1.6;
}

.review-hero-card {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 24px;
  background: radial-gradient(circle at top left, rgba(37, 99, 235, 0.14), transparent 34%), #ffffff;
}

.review-hero-card h1 {
  margin: 0;
  color: #0f172a;
  font-size: 26px;
  line-height: 1.3;
}

.review-hero-card p {
  margin: 10px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.review-hero-card > strong {
  align-self: flex-start;
  white-space: nowrap;
  border-radius: 999px;
  padding: 8px 14px;
  background: #eef2ff;
  color: #4f46e5;
  font-size: 13px;
}

.review-hero-card[data-reviewing="true"] > strong {
  background: #ffedd5;
  color: #c2410c;
}

.empty-review-card {
  padding: 28px;
  border: 1px dashed #bfdbfe;
  background: #f8fbff;
}

.empty-review-card h2 {
  margin: 0 0 10px;
  color: #111827;
  font-size: 18px;
}

.empty-review-card p {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

.workflow-panel {
  padding: 14px;
}

.compact-workflow-panel {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 300px);
  min-height: 360px;
  max-height: calc(100vh - 300px);
  overflow: hidden;
}

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-title-row h2 {
  margin: 0;
  color: #111827;
  font-size: 16px;
}

.section-title-row span {
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
}

.workflow-stepper {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 14px;
  padding-bottom: 8px;
  border-bottom: 0;
}

.compact-workflow-panel .workflow-stepper {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 4px;
  scrollbar-width: none;
}

.compact-workflow-panel .workflow-stepper::-webkit-scrollbar {
  display: none;
}

.step-item {
  position: relative;
  display: grid;
  grid-template-columns: 28px 32px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  min-width: 0;
  min-height: 56px;
  border-radius: 12px;
  padding: 6px 6px;
  background: transparent;
}

.step-rail-node {
  position: relative;
  display: inline-flex;
  justify-content: center;
}

.step-rail-node::after {
  content: "";
  position: absolute;
  left: 50%;
  top: 24px;
  bottom: -30px;
  width: 2px;
  background: #dbe3ef;
  transform: translateX(-50%);
}

.step-item:last-child .step-rail-node::after {
  display: none;
}

.step-item[data-status="success"] .step-rail-node::after {
  background: #22c55e;
}

.step-item[data-status="awaiting_review"] {
  background: #f4f7ff;
}

.step-item[data-status="error"] {
  background: #fff7f7;
}

.step-revise-button,
.step-retry-button {
  border: 1px solid #fed7aa;
  border-radius: 999px;
  background: #fff7ed;
  color: #ea580c;
  padding: 5px 10px;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.step-revise-button:hover {
  border-color: #fb923c;
  background: #ffedd5;
}

.step-retry-button {
  border-color: #fecaca;
  background: #fef2f2;
  color: #dc2626;
}

.step-retry-button:hover:not(:disabled) {
  border-color: #f87171;
  background: #fee2e2;
}

.step-retry-button:disabled {
  cursor: wait;
  opacity: 0.7;
}

.step-index {
  width: 22px;
  height: 22px;
  border-radius: 999px;
  background: #94a3b8;
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

.step-icon {
  width: 30px;
  height: 30px;
  border-radius: 999px;
  background: #f8fafc;
  color: #475569;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 800;
}

.step-item[data-status="success"] .step-index {
  background: #22c55e;
  color: #ffffff;
}

.step-item[data-status="success"] .step-icon {
  background: #dcfce7;
  color: #16a34a;
}

.step-item[data-status="running"] .step-index {
  background: #2563eb;
  color: #fff;
}

.step-item[data-status="awaiting_review"] .step-index {
  background: #f97316;
  color: #ffffff;
}

.step-item[data-status="awaiting_review"] .step-icon {
  background: #fff7ed;
  color: #f97316;
}

.step-item[data-status="error"] .step-index {
  background: #dc2626;
  color: #ffffff;
}

.step-item[data-status="error"] .step-icon {
  background: #fee2e2;
  color: #dc2626;
}

.step-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.step-copy strong {
  color: #111827;
  font-size: 14px;
  white-space: nowrap;
}

.step-copy span {
  color: #7c8798;
  font-size: 12px;
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
  padding: 14px;
}

.fixed-log-card {
  display: flex;
  flex-direction: column;
  min-height: 260px;
  max-height: 260px;
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
  gap: 10px;
  margin-top: 12px;
  min-height: 0;
  height: 196px;
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
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 8px;
  border-left: 2px solid #dbeafe;
  padding-left: 10px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
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

.revision-status-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.revision-status-list div {
  display: grid;
  grid-template-columns: 82px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #edf1f7;
  padding-bottom: 8px;
}

.revision-status-list span,
.revision-status-list small {
  color: #64748b;
  font-size: 12px;
}

.revision-status-list strong {
  color: #111827;
  font-size: 12px;
  text-align: right;
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

@media (max-width: 1500px) {
  .dashboard-grid {
    grid-template-columns: 280px minmax(520px, 1fr) 300px;
  }

  .factory-topbar {
    gap: 12px;
  }
}

@media (max-width: 1280px) {
  .dashboard-grid {
    grid-template-columns: 280px minmax(0, 1fr);
  }

  .factory-topbar {
    overflow: hidden;
  }

  .sidebar-column {
    grid-column: 1 / -1;
    position: static;
    max-height: none;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .sidebar-column > * {
    min-width: 0;
  }
}

@media (max-width: 900px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .left-workbench-column,
  .sidebar-column {
    position: static;
    max-height: none;
  }

  .sidebar-column {
    display: flex;
  }

  .review-hero-card {
    flex-direction: column;
  }
}
</style>
