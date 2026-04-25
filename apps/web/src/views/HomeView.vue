<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { fetchHealth } from "@/api/health";
import { clearLogs, fetchLogs, fetchResult, fetchWorkflowStatus, startWorkflow } from "@/api/workflow";

const requestText = ref("");
const health = ref(null);
const status = ref(null);
const logs = ref([]);
const result = ref({ available: false });
const loading = ref(false);
const pageError = ref("");
const logPanel = ref(null);
let pollTimer = null;

const statusText = {
  pending: "未开始",
  running: "执行中",
  success: "已完成",
  error: "失败"
};

const statusClass = computed(() => status.value?.status || "pending");
const examples = computed(() => status.value?.examples || []);
const steps = computed(() => status.value?.steps || []);
const agents = computed(() => status.value?.agents || []);
const currentStage = computed(() => status.value?.currentStage || "--");
const currentArtifactType = computed(() => status.value?.currentArtifactType || "--");
const designProgressMessage = computed(() => status.value?.designProgressMessage || "等待任务启动");
const remaining = computed(() => status.value?.estimatedCompletion || "--");
const testPassRate = computed(() => status.value?.testPassRate || "--");
const canRun = computed(() => requestText.value.trim().length > 0 && !loading.value);
const actionsEnabled = computed(() => result.value?.available === true);
const designAvailable = computed(() => result.value?.designAvailable === true);
const systemStatus = computed(() => health.value?.status === "UP" ? "在线" : "离线");

function statusLabel(value) {
  return statusText[value] || value || "未开始";
}

function pickExample(example) {
  requestText.value = `做一个${example}，支持核心页面、后端接口和自动化测试报告。`;
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
    await nextTick();
    scrollLogsToBottom();
  } catch (error) {
    pageError.value = "无法连接后端编排接口，请先启动后端服务。";
  }
}

async function runWorkflow() {
  if (!canRun.value) {
    return;
  }

  loading.value = true;
  pageError.value = "";
  try {
    status.value = await startWorkflow(requestText.value);
    await refreshAll();
  } catch (error) {
    pageError.value = error.response?.data?.message || "任务创建失败，请稍后重试。";
  } finally {
    loading.value = false;
  }
}

async function retryWorkflow() {
  await runWorkflow();
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
  if (url && actionsEnabled.value) {
    window.open(url, "_blank", "noopener,noreferrer");
  }
}

function scrollLogsToBottom() {
  if (logPanel.value) {
    logPanel.value.scrollTop = logPanel.value.scrollHeight;
  }
}

onMounted(async () => {
  try {
    health.value = await fetchHealth();
  } catch (error) {
    health.value = null;
  }

  await refreshAll();
  pollTimer = window.setInterval(refreshAll, 2000);
});

onBeforeUnmount(() => {
  if (pollTimer) {
    window.clearInterval(pollTimer);
  }
});
</script>

<template>
  <main class="app-shell">
    <header class="topbar">
      <div class="brand-mark" aria-hidden="true">AI</div>
      <div>
        <p class="eyebrow">AI Software Factory</p>
        <h1>AI 编排平台</h1>
      </div>
      <div class="system-pill" :data-status="systemStatus">
        <span></span>
        {{ systemStatus }}
      </div>
    </header>

    <section class="workspace-grid">
      <div class="main-column">
        <section class="panel input-panel">
          <div class="section-heading">
            <div>
              <h2>输入需求</h2>
              <p>支持自然语言描述你的应用需求</p>
            </div>
            <span>预计耗时 1 ~ 3 分钟</span>
          </div>

          <div class="prompt-row">
            <textarea
              v-model="requestText"
              placeholder="做一个AI质检助手，支持上传日志文件，分析问题并生成报告"
              rows="5"
            ></textarea>
            <button class="primary-action" :disabled="!canRun" @click="runWorkflow">
              <span v-if="loading" class="spinner"></span>
              一键生成并运行
            </button>
          </div>

          <div class="quick-examples" aria-label="快速示例">
            <span>快速示例：</span>
            <button v-for="example in examples" :key="example" type="button" @click="pickExample(example)">
              {{ example }}
            </button>
          </div>
          <p v-if="pageError" class="error-line">{{ pageError }}</p>
        </section>

        <section class="kpi-grid">
          <article class="kpi-card">
            <span>当前任务状态</span>
            <strong :data-status="statusClass">{{ statusLabel(status?.status) }}</strong>
            <small>{{ status?.status === "running" ? "任务正常进行中" : "等待执行指令" }}</small>
          </article>
          <article class="kpi-card">
            <span>当前阶段</span>
            <strong>{{ currentStage }}</strong>
            <small>由后端状态接口同步</small>
          </article>
          <article class="kpi-card">
            <span>预计完成时间</span>
            <strong>{{ remaining }}</strong>
            <small>预计剩余时间</small>
          </article>
          <article class="kpi-card">
            <span>测试通过率</span>
            <strong>{{ testPassRate }}</strong>
            <small>完成后显示</small>
          </article>
        </section>

        <section class="panel">
          <div class="section-heading compact">
            <h2>执行流程</h2>
            <span>当前进度 {{ status?.progress || 0 }}%</span>
          </div>
          <div class="flow-meta">
            <div>
              <span>当前产物</span>
              <strong>{{ currentArtifactType }}</strong>
            </div>
            <div>
              <span>进度说明</span>
              <strong>{{ designProgressMessage }}</strong>
            </div>
            <div>
              <span>预计剩余</span>
              <strong>{{ status?.estimatedRemaining || "--" }}</strong>
            </div>
          </div>
          <div class="stepper">
            <article v-for="step in steps" :key="step.key" class="step-item" :data-status="step.status">
              <div class="step-index">
                <span v-if="step.status === 'success'">✓</span>
                <span v-else>{{ step.index }}</span>
              </div>
              <div class="step-content">
                <h3>{{ step.title }}</h3>
                <p>{{ step.detail }}</p>
                <small v-if="step.status === 'success'">耗时 {{ step.duration }}</small>
                <small v-else-if="step.error">{{ step.error }}</small>
                <div v-if="step.status === 'running'" class="progress-track">
                  <span :style="{ width: `${step.progress}%` }"></span>
                </div>
              </div>
            </article>
          </div>
          <div v-if="status?.status === 'error'" class="failure-box">
            <strong>{{ status.error }}</strong>
            <button type="button" @click="retryWorkflow">重试</button>
          </div>
        </section>
      </div>

      <aside class="panel log-card">
        <div class="section-heading compact">
          <div>
            <h2>实时日志</h2>
            <p><span class="live-dot"></span> 实时</p>
          </div>
          <button type="button" class="ghost-button" @click="clearLogPanel">清空</button>
        </div>
        <div ref="logPanel" class="log-panel" aria-label="实时日志">
          <p v-if="logs.length === 0" class="empty-log">暂无日志</p>
          <div v-for="entry in logs" :key="`${entry.time}-${entry.agent}-${entry.message}`" class="log-line" :data-level="entry.level">
            <span>{{ entry.time }}</span>
            <strong>[{{ entry.agent }}]</strong>
            <em>{{ entry.message }}</em>
          </div>
        </div>
      </aside>
    </section>

    <section class="bottom-grid">
      <section class="panel agent-panel">
        <div class="section-heading compact">
          <h2>参与 Agent 列表</h2>
        </div>
        <div class="agent-table">
          <div class="agent-row header">
            <span>Agent名称</span>
            <span>角色</span>
            <span>状态</span>
            <span>使用模型</span>
            <span>执行耗时</span>
          </div>
          <div v-for="agent in agents" :key="agent.name" class="agent-row">
            <span class="agent-name">{{ agent.name }}</span>
            <span>{{ agent.role }}</span>
            <span class="status-badge" :data-status="agent.status">
              {{ statusLabel(agent.status) }}
              <i v-if="agent.status === 'running'">{{ agent.progress }}%</i>
            </span>
            <span>{{ agent.model }}</span>
            <span>{{ agent.duration }}</span>
          </div>
        </div>
      </section>

      <section class="panel result-panel">
        <div class="section-heading compact">
          <div>
            <h2>结果与操作</h2>
            <p>任务完成后可进行以下操作</p>
          </div>
        </div>
        <section class="design-result" :data-empty="!designAvailable">
          <div class="section-heading compact design-heading">
            <div>
              <h3>产品设计结果</h3>
              <p>PRD 与 UI 规范由后端设计阶段实时产出</p>
            </div>
          </div>
          <div v-if="designAvailable" class="design-grid">
            <article class="design-card markdown-card">
              <h3>PRD 摘要</h3>
              <pre>{{ result.prdMarkdown }}</pre>
            </article>
            <article class="design-card">
              <h3>页面清单</h3>
              <ul>
                <li v-for="pageSpec in result.pageSpecs" :key="pageSpec.name">
                  <strong>{{ pageSpec.name }}</strong>
                  <p>{{ pageSpec.description }}</p>
                  <span>{{ pageSpec.sections.join(" / ") }}</span>
                </li>
              </ul>
            </article>
            <article class="design-card">
              <h3>用户流程</h3>
              <ul>
                <li v-for="flow in result.userFlowSpecs" :key="flow.name">
                  <strong>{{ flow.name }}</strong>
                  <span>{{ flow.steps.join(" -> ") }}</span>
                </li>
              </ul>
            </article>
            <article class="design-card">
              <h3>组件建议</h3>
              <ul>
                <li v-for="component in result.componentSpecs" :key="component.name">
                  <strong>{{ component.name }}</strong>
                  <p>{{ component.description }}</p>
                  <span>{{ component.capabilities.join(" / ") }}</span>
                </li>
              </ul>
            </article>
            <article class="design-card full-width">
              <h3>UI 规范</h3>
              <ul class="guideline-list">
                <li v-for="guideline in result.uiGuidelines" :key="guideline">{{ guideline }}</li>
              </ul>
            </article>
          </div>
          <div v-else class="design-empty">
            <strong>设计结果尚未生成</strong>
            <p>运行 Product Agent 和 Design Agent 后，这里会显示 PRD、页面清单、用户流程和组件建议。</p>
          </div>
        </section>
        <div class="result-actions">
          <button type="button" :disabled="!actionsEnabled" @click="openLink(result.projectUrl)">
            <strong>打开生成的项目</strong>
            <span>启动前端应用</span>
          </button>
          <button type="button" :disabled="!actionsEnabled" @click="openLink(result.reportUrl)">
            <strong>查看测试报告</strong>
            <span>Playwright 报告</span>
          </button>
          <button type="button" :disabled="!actionsEnabled" @click="openLink(result.zipUrl)">
            <strong>下载代码包</strong>
            <span>ZIP 压缩包</span>
          </button>
        </div>
        <p class="result-note">设计结果生成后会先开放结构化查看；项目、报告和代码包操作仍在完整流程完成后启用。</p>
      </section>
    </section>

    <footer>© 2025 AI Software Factory. All rights reserved.</footer>
  </main>
</template>
