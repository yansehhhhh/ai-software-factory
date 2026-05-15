<script setup>
import { computed, nextTick, ref, watch } from "vue";
import MarkdownIt from "markdown-it";
import DOMPurify from "dompurify";
import { artifactUrl, fetchArtifactContent } from "@/api/artifact";
import MarkdownMessage from "@/components/MarkdownMessage.vue";

const props = defineProps({
  review: {
    type: Object,
    default: null
  },
  loading: {
    type: Boolean,
    default: false
  },
  revisionConversationOpen: {
    type: Boolean,
    default: false
  },
  status: {
    type: Object,
    default: null
  },
  result: {
    type: Object,
    default: () => ({ artifacts: [] })
  },
  discussionId: {
    type: String,
    default: ""
  },
  discussionMessages: {
    type: Array,
    default: () => []
  },
  discussionComplete: {
    type: Boolean,
    default: false
  },
  discussionLoading: {
    type: Boolean,
    default: false
  },
  logs: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(["approve", "revise", "openspec", "openArtifact", "discussionSend", "discussionConfirm"]);

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true
});

const feedback = ref("");
const revisionType = ref("");
const revisionOpen = ref(false);
const addingRevisionFeedback = ref(false);
const activeArtifactStage = ref("");
const discussionScroll = ref(null);

const artifactStageLabels = {
  requirement: "需求阶段",
  "ui-prototype": "UI原型",
  architecture: "架构设计",
  database: "数据库设计",
  "test-docs": "测试",
  misc: "其他文档",
  code: "代码与交付"
};

const artifactStageOrder = ["requirement", "ui-prototype", "architecture", "database", "test-docs", "misc", "code"];

function normalizeArtifactStage(stage) {
  const stageMap = {
    requirements: "requirement",
    requirement: "requirement",
    brainstorming: "requirement",
    "product-design-artifacts": "requirement",
    "writing-plans": "ui-prototype",
    "ui-design": "ui-prototype",
    "ui-prototype": "ui-prototype",
    architecture: "architecture",
    "architecture-design": "architecture",
    "api-design": "architecture",
    database: "database",
    "database-design": "database",
    "test-docs": "test-docs",
    "test-case-generation": "test-docs",
    "development-integration": "test-docs",
    "e2e-acceptance-testing": "test-docs",
    "verification-before-completion": "test-docs",
    misc: "misc",
    code: "code",
    "frontend-development": "code",
    "backend-development": "code",
    delivery: "code"
  };
  return stageMap[stage] || "code";
}

const hasReview = computed(() => Boolean(props.review));
const hasDiscussion = computed(() => Boolean(props.discussionId));
const discussionInputDisabled = computed(() => props.discussionLoading || props.discussionComplete);
const latestAiMessageIndex = computed(() => {
  for (let index = props.discussionMessages.length - 1; index >= 0; index -= 1) {
    if (props.discussionMessages[index]?.role === "ai") return index;
  }
  return -1;
});
const canConfirmDiscussion = computed(() => hasDiscussion.value && !hasReview.value && !props.discussionLoading && props.discussionMessages.length > 0);
const allArtifacts = computed(() => {
  const resultArtifacts = props.result?.artifacts || [];
  return resultArtifacts.length ? resultArtifacts : props.review?.artifacts || [];
});
const artifactGroups = computed(() => {
  const groups = Object.fromEntries(artifactStageOrder.map((stage) => [stage, []]));
  for (const artifact of allArtifacts.value) {
    const stage = normalizeArtifactStage(artifact?.stage);
    groups[stage].push(artifact);
  }
  return artifactStageOrder
    .map((stage) => ({
      stage,
      label: artifactStageLabels[stage],
      artifacts: groups[stage]
    }))
    .filter((group) => group.artifacts.length > 0);
});
const activeArtifactGroup = computed(() => artifactGroups.value.find((group) => group.stage === activeArtifactStage.value) || artifactGroups.value[0] || null);
const artifacts = computed(() => activeArtifactGroup.value?.artifacts || []);
const revision = computed(() => props.review?.revision || null);
const feedbackTypes = [
  {
    value: "缺陷修复",
    description: "页面报错、接口异常、交互不符合设计、数据展示错误"
  },
  {
    value: "验收/测试辅助",
    description: "只用于本地预览、验收或 E2E，例如 URL 参数切换角色"
  },
  {
    value: "需求变更",
    description: "新增或改变产品范围，例如新增功能、改业务规则"
  }
];
const revisionMessages = computed(() => props.review?.revisionMessages || []);
const hasChangeId = computed(() => Boolean(revision.value?.changeId));
const isAwaitingReview = computed(() => props.review?.status === "awaiting_review");
const runningRevisionStatuses = new Set(["revision_discussing", "revision_proposing", "revision_applying", "revision_archiving"]);
const waitingChoiceRevisionStatuses = new Set(["revision_waiting_choice"]);
const completedRevisionStatuses = new Set(["revision_proposed", "revision_applied", "revision_archived"]);
const isRevisionRunning = computed(() => props.loading || runningRevisionStatuses.has(revision.value?.status));
const isRevisionWaitingChoice = computed(() => waitingChoiceRevisionStatuses.has(revision.value?.status));
const isRevisionFeedbackSubmitted = computed(() => revision.value?.status === "feedback_submitted");
const hasSubmittedRevision = computed(() => Boolean(revision.value));
const hasExplorationResult = computed(() => revision.value?.status === "revision_explored" || (revision.value?.status === "feedback_submitted" && revisionMessages.value.some((message) => message.role === "ai" && message.content?.includes("/opsx:explore"))));
const hasOpenSpecResult = computed(() => hasExplorationResult.value || completedRevisionStatuses.has(revision.value?.status));
const hasRevisionConversation = computed(() => props.revisionConversationOpen || hasSubmittedRevision.value || revisionMessages.value.length > 0);
const showRevisionFeedbackInput = computed(() => hasReview.value && hasRevisionConversation.value && (!hasOpenSpecResult.value || addingRevisionFeedback.value));
const showExploreAdjustmentInput = computed(() => hasReview.value && hasRevisionConversation.value && hasExplorationResult.value && !addingRevisionFeedback.value);
const canTypeRevision = computed(() => showRevisionFeedbackInput.value && isAwaitingReview.value && !isRevisionRunning.value);
const canAdjustExploration = computed(() => showExploreAdjustmentInput.value && isAwaitingReview.value && !isRevisionRunning.value);
const canUseRevision = computed(() => hasReview.value && isAwaitingReview.value && !isRevisionRunning.value);
const canApplyOrArchive = computed(() => canUseRevision.value && hasChangeId.value);
const canApprove = computed(() => hasReview.value && isAwaitingReview.value && !isRevisionRunning.value && !hasSubmittedRevision.value);
const stageTitle = computed(() => props.review?.stageTitle || props.status?.currentStage || "等待阶段产物");
const nextStageTitle = computed(() => props.review?.nextStageTitle || "--");
const stageStatusLabel = computed(() => props.review?.statusLabel || props.status?.statusLabel || "等待工作流运行");
const discussionTitle = computed(() => hasDiscussion.value && !hasReview.value ? "需求讨论" : "阶段修订讨论");
const discussionSubtitle = computed(() => hasDiscussion.value && !hasReview.value
  ? "正在讨论：需求分析"
  : `正在讨论：${stageTitle.value} 的阶段产物`);
const discussionIntro = computed(() => hasReview.value
  ? props.revisionConversationOpen
    ? "提交修订反馈\n\n你在预览生成项目时发现了什么？请在下方选择反馈类型并填写反馈内容。缺陷修复和验收/测试辅助会作为当前阶段修复处理；需求变更建议创建 OpenSpec 变更。"
    : "当前阶段正在等待确认。你可以先查看阶段产物或预览生成项目；如果发现问题，请点击左侧当前阶段后的“修订”开始提交反馈。"
  : "当前暂无待确认阶段。工作流生成阶段产物后，可以在这里查看产物、提交修订意见，并通过 OpenSpec 完成探索、计划、执行和归档。");
const approveButtonText = computed(() => {
  if (hasDiscussion.value && !hasReview.value) return "结束讨论并确认需求";
  if (isRevisionRunning.value) return "修订中";
  if (isRevisionFeedbackSubmitted.value) return "等待修订完成";
  if (hasSubmittedRevision.value) return "重新确认";
  return "确认通过";
});
const approveButtonHint = computed(() => {
  if (hasDiscussion.value && !hasReview.value) return "进入下一阶段：需求产物生成";
  if (isRevisionRunning.value) return "正在修改生成产物并重新验证";
  if (isRevisionFeedbackSubmitted.value) return "修订完成后将重新验证";
  if (hasSubmittedRevision.value) return "查看修订结果后确认进入下一阶段";
  return `进入下一阶段：${nextStageTitle.value}`;
});

watch(() => props.review?.stageKey, () => {
  feedback.value = props.review?.userFeedback || "";
  addingRevisionFeedback.value = false;
});

watch(() => revision.value?.status, (status) => {
  if (completedRevisionStatuses.has(status)) {
    addingRevisionFeedback.value = false;
    feedback.value = "";
  }
});

watch(
  () => props.discussionMessages.length,
  async () => {
    await nextTick();
    if (discussionScroll.value) {
      discussionScroll.value.scrollTop = discussionScroll.value.scrollHeight;
    }
  }
);

watch(
  [artifactGroups, () => props.review?.stageKey],
  ([groups, stageKey]) => {
    if (!groups.length) {
      activeArtifactStage.value = "";
      return;
    }
    const currentStage = normalizeArtifactStage(stageKey);
    const currentStageGroup = groups.find((group) => group.stage === currentStage);
    const activeStageExists = groups.some((group) => group.stage === activeArtifactStage.value);
    if (currentStageGroup && (!activeArtifactStage.value || !activeStageExists)) {
      activeArtifactStage.value = currentStageGroup.stage;
      return;
    }
    if (!activeStageExists) {
      activeArtifactStage.value = groups[0].stage;
    }
  },
  { immediate: true }
);

function artifactDisplayName(artifact) {
  return artifact?.name || artifact?.path?.split("/").pop() || "产物";
}

function openArtifactPreview(artifact) {
  if (!artifact?.path) return;
  const name = artifactDisplayName(artifact);
  const path = artifact.path;
  const lowerPath = path.toLowerCase();
  const type = artifact.type || "";
  if (normalizeArtifactStage(artifact.stage) === "code" && (type === "directory" || lowerPath.includes("/frontend") || lowerPath.includes("/generated/"))) {
    emit("openArtifact", { action: "previewGeneratedProject", path });
    return;
  }
  if (type === "plantuml" || lowerPath.endsWith(".puml")) {
    emit("openArtifact", artifactUrl(path));
    return;
  }
  if (type === "markdown" || type === "yaml" || type === "sql" || lowerPath.endsWith(".md") || lowerPath.endsWith(".yaml") || lowerPath.endsWith(".yml") || lowerPath.endsWith(".sql") || lowerPath.endsWith(".txt")) {
    openMarkdownPreview({ path, name });
    return;
  }
  emit("openArtifact", artifactUrl(path));
}

function openSpecFiles(content) {
  if (!content) return [];
  const archiveMatch = content.match(/openspec\/changes\/archive\/([\w.-]+)\//);
  const changeMatch = content.match(/openspec\/changes\/([\w.-]+)\//);
  const changeRoot = archiveMatch
    ? `openspec/changes/archive/${archiveMatch[1]}`
    : changeMatch
      ? `openspec/changes/${changeMatch[1]}`
      : "";
  if (!changeRoot) return [];
  const filePaths = [
    `${changeRoot}/proposal.md`,
    `${changeRoot}/design.md`,
    `${changeRoot}/tasks.md`,
    ...Array.from(content.matchAll(/specs\/[^\s`：:，。)]+\/spec\.md/g)).map((match) => `${changeRoot}/${match[0]}`)
  ];
  return Array.from(new Set(filePaths)).map((path) => ({
    path,
    name: path.split("/").slice(-2).join("/")
  }));
}

function markdownPreviewHtml(title, markdownContent) {
  const body = DOMPurify.sanitize(markdown.render(markdownContent || ""));
  const safeTitle = DOMPurify.sanitize(title || "Markdown 预览");
  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>${safeTitle}</title>
  <style>
    body { margin: 0; background: #f6f8fc; color: #0f172a; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }
    main { max-width: 980px; margin: 32px auto; padding: 32px 40px; background: #fff; border: 1px solid #e5eaf3; border-radius: 18px; box-shadow: 0 18px 48px rgba(15, 23, 42, 0.08); }
    h1, h2, h3 { color: #111827; line-height: 1.35; }
    h1 { font-size: 30px; border-bottom: 1px solid #e5eaf3; padding-bottom: 14px; }
    h2 { margin-top: 30px; font-size: 23px; }
    h3 { margin-top: 22px; font-size: 18px; }
    p, li { line-height: 1.8; }
    a { color: #2563eb; }
    code { border-radius: 6px; background: #eef2f7; padding: 2px 6px; font-family: SFMono-Regular, Consolas, monospace; }
    pre { overflow: auto; border-radius: 12px; background: #0f172a; color: #e2e8f0; padding: 16px; }
    pre code { background: transparent; padding: 0; color: inherit; }
    blockquote { margin: 18px 0; border-left: 4px solid #93c5fd; background: #eff6ff; padding: 10px 16px; color: #334155; }
    table { width: 100%; border-collapse: collapse; margin: 18px 0; overflow: auto; display: block; }
    th, td { border: 1px solid #dbe3ef; padding: 10px 12px; text-align: left; white-space: nowrap; }
    th { background: #f8fafc; }
  </style>
</head>
<body><main>${body}</main></body>
</html>`;
}

async function openMarkdownPreview(file) {
  const content = await fetchArtifactContent(file.path);
  const blob = new Blob([markdownPreviewHtml(file.name, content)], { type: "text/html;charset=UTF-8" });
  window.open(URL.createObjectURL(blob), "_blank", "noopener,noreferrer");
}

function submitRevision() {
  if (hasDiscussion.value && !hasReview.value) {
    if (feedback.value.trim().length === 0 || discussionInputDisabled.value) return;
    emit("discussionSend", feedback.value);
    feedback.value = "";
    return;
  }
  if (!canTypeRevision.value || feedback.value.trim().length === 0) return;
  const nextFeedback = revisionType.value
    ? [
        `反馈类型：${revisionType.value}`,
        "反馈内容：",
        feedback.value.trim()
      ].join("\n")
    : feedback.value.trim();
  emit("revise", nextFeedback);
  feedback.value = "";
  addingRevisionFeedback.value = false;
}

function startAddingRevisionFeedback() {
  if (!canUseRevision.value) return;
  addingRevisionFeedback.value = true;
  feedback.value = "";
}

function cancelAddingRevisionFeedback() {
  addingRevisionFeedback.value = false;
  feedback.value = "";
}

function adjustExploration() {
  if (!canAdjustExploration.value || feedback.value.trim().length === 0) return;
  emit("openspec", { action: "explore", feedback: feedback.value.trim() });
  feedback.value = "";
}

function selectRevisionType(type) {
  if (!canTypeRevision.value) return;
  revisionType.value = type;
}

function sendDiscussionOption(option) {
  if (!props.discussionLoading && !props.discussionComplete) {
    emit("discussionSend", option);
  }
}

function confirmDiscussion() {
  if (canConfirmDiscussion.value) {
    emit("discussionConfirm");
  }
}

function runAction(action) {
  emit("openspec", { action, feedback: feedback.value });
}

function selectOpenSpecOption(option) {
  const status = revision.value?.status;
  const action = status === "revision_waiting_choice" ? "archive" : "explore";
  emit("openspec", { action, feedback: option });
}
</script>

<template>
  <section class="stage-review-panel" :data-empty="!hasReview">
    <div class="revision-card">
      <div class="discussion-context">
        <h2>{{ discussionTitle }}</h2>
        <p>{{ discussionSubtitle }}</p>
      </div>
      <div ref="discussionScroll" class="stage-discussion-scroll">
        <template v-if="hasDiscussion && !hasReview">
          <div
            v-for="(message, index) in discussionMessages"
            :key="`${message.role}-${index}`"
            class="stage-message-row"
            :class="{ 'user-message-row': message.role === 'user' }"
          >
            <div class="avatar" :class="{ 'user-avatar': message.role === 'user' }">{{ message.role === 'ai' ? 'AI' : '我' }}</div>
            <div class="stage-message-bubble">
              <MarkdownMessage :content="message.content" />
              <div v-if="message.role === 'ai' && index === latestAiMessageIndex && message.options?.length" class="discussion-option-list">
                <button
                  v-for="option in message.options"
                  :key="option"
                  type="button"
                  :disabled="discussionLoading || discussionComplete"
                  @click="sendDiscussionOption(option)"
                >
                  {{ option }}
                </button>
              </div>
            </div>
          </div>
          <div v-if="discussionMessages.length === 0" class="stage-message-row">
            <div class="avatar">AI</div>
            <div class="stage-message-bubble"><MarkdownMessage content="请先在左侧输入需求并开始讨论，我会在这里继续梳理需求。" /></div>
          </div>
        </template>
        <template v-else>
          <div v-if="revisionMessages.length === 0" class="stage-message-row">
            <div class="avatar">AI</div>
            <div class="stage-message-bubble">
              <MarkdownMessage :content="discussionIntro" />
              <div v-if="hasRevisionConversation && !hasSubmittedRevision" class="revision-type-options">
                <button
                  v-for="type in feedbackTypes"
                  :key="type.value"
                  type="button"
                  :data-active="revisionType === type.value"
                  :disabled="!canTypeRevision"
                  @click="selectRevisionType(type.value)"
                >
                  <strong>{{ type.value }}</strong>
                  <small>{{ type.description }}</small>
                </button>
              </div>
            </div>
          </div>
          <div
            v-for="(message, index) in revisionMessages"
            :key="`${message.role}-${index}`"
            class="stage-message-row"
            :class="{ 'user-message-row': message.role === 'user' }"
          >
            <div class="avatar" :class="{ 'user-avatar': message.role === 'user' }">{{ message.role === 'ai' ? 'AI' : '我' }}</div>
            <div class="stage-message-bubble">
              <div v-if="message.time" class="stage-message-time">{{ message.time }}</div>
              <MarkdownMessage :content="message.content" />
              <div v-if="message.role === 'ai' && message.options?.length" class="openspec-choice-list">
                <button
                  v-for="option in message.options"
                  :key="option"
                  type="button"
                  :disabled="!isRevisionWaitingChoice || isRevisionRunning"
                  @click="selectOpenSpecOption(option)"
                >
                  {{ option }}
                </button>
              </div>
              <div v-if="message.role === 'ai' && openSpecFiles(message.content).length" class="openspec-file-list">
                <button
                  v-for="file in openSpecFiles(message.content)"
                  :key="file.path"
                  type="button"
                  @click="openMarkdownPreview(file)"
                >
                  <span>预览</span>
                  <strong>{{ file.name }}</strong>
                </button>
              </div>
            </div>
          </div>
        </template>
      </div>
      <div v-if="showExploreAdjustmentInput" class="feedback-input-row revision-chat-input-row explore-adjustment-row">
        <input
          :value="feedback"
          :disabled="!canAdjustExploration"
          placeholder="基于上方探索结论补充调整，例如：第 1 点不做，只保留 token 验收辅助，并重新探索"
          @input="feedback = $event.target.value"
          @keydown.enter.prevent="adjustExploration"
        />
        <button type="button" class="send-feedback-button" :disabled="!canAdjustExploration || feedback.trim().length === 0" aria-label="调整探索结论并重新探索" @click="adjustExploration">➤</button>
      </div>
      <div v-else-if="showRevisionFeedbackInput" class="feedback-input-row revision-chat-input-row">
        <input
          :value="feedback"
          :disabled="!canTypeRevision"
          placeholder="补充新的修订反馈，会重新进入反馈评估；如要继续当前 OpenSpec 结果，请使用下方操作按钮"
          @input="feedback = $event.target.value"
          @keydown.enter.prevent="submitRevision"
        />
        <button type="button" class="send-feedback-button" :disabled="!canTypeRevision || feedback.trim().length === 0" aria-label="发送修订反馈" @click="submitRevision">➤</button>
        <button v-if="hasOpenSpecResult" type="button" class="cancel-feedback-button" @click="cancelAddingRevisionFeedback">取消</button>
      </div>
      <div v-else-if="hasReview && hasRevisionConversation && hasOpenSpecResult" class="revision-next-step-panel">
        <div>
          <strong>{{ hasExplorationResult ? '可以继续调整探索结论' : '当前展示的是 OpenSpec 阶段结果' }}</strong>
          <span>{{ hasExplorationResult ? '在下方输入补充约束会重新探索；如果结论已符合预期，可直接点击“制定计划”。' : '继续请使用“执行计划 / 归档”等操作；只有需要改变反馈内容时，才补充新的修订反馈。' }}</span>
        </div>
        <button type="button" :disabled="!canUseRevision" @click="startAddingRevisionFeedback">补充新反馈</button>
      </div>
      <div v-else-if="!hasReview" class="feedback-input-row">
        <input
          :value="feedback"
          :disabled="discussionInputDisabled"
          placeholder="输入您的回复..."
          @input="feedback = $event.target.value"
          @keydown.enter.prevent="submitRevision"
        />
        <button type="button" class="send-feedback-button" :disabled="feedback.trim().length === 0 || discussionInputDisabled" aria-label="发送修订反馈" @click="submitRevision">➤</button>
      </div>
      <div class="revision-toolbar">
        <button type="button" class="collapse-button" @click="revisionOpen = !revisionOpen">
          <span class="collapse-chevron" :data-open="revisionOpen"></span>
          {{ revisionOpen ? '收起修订流程' : '展开修订流程' }}
          <small>（仅在需要修改时使用）</small>
        </button>
        <button v-if="hasDiscussion && !hasReview" type="button" class="approve-button" :disabled="!canConfirmDiscussion" @click="confirmDiscussion">
          {{ approveButtonText }}
          <span>{{ approveButtonHint }}</span>
        </button>
        <button v-else type="button" class="approve-button" :disabled="!canApprove" @click="emit('approve')">
          {{ approveButtonText }}
          <span>{{ approveButtonHint }}</span>
        </button>
      </div>
      <div v-if="revisionOpen" class="revision-actions">
        <button type="button" :disabled="!canUseRevision" @click="runAction('explore')"><span class="revision-action-icon chat">●</span><strong>探索修订</strong><small>OpenSpec Explore</small></button>
        <button type="button" :disabled="!canUseRevision" @click="runAction('propose')"><span class="revision-action-icon plan">⌾</span><strong>制定计划</strong><small>OpenSpec Propose</small></button>
        <button type="button" :disabled="!canApplyOrArchive" @click="runAction('apply')"><span class="revision-action-icon apply">➜</span><strong>执行计划</strong><small>OpenSpec Apply</small></button>
        <button type="button" :disabled="!canApplyOrArchive" @click="runAction('archive')"><span class="revision-action-icon archive">▣</span><strong>归档</strong><small>OpenSpec Archive</small></button>
      </div>
    </div>

    <section class="artifact-list-card compact-artifact-card">
      <div class="card-title-row artifact-title-row">
        <div>
          <h2>阶段产物</h2>
          <p>可切换查看各阶段已生成的文档、设计、代码与测试产物；确认当前阶段无误后点击“确认通过，进入下一阶段”。</p>
          <div v-if="artifactGroups.length" class="artifact-stage-tabs">
            <button
              v-for="group in artifactGroups"
              :key="group.stage"
              type="button"
              class="artifact-stage-tab"
              :data-active="activeArtifactStage === group.stage"
              @click="activeArtifactStage = group.stage"
            >
              <span>{{ group.label }}</span>
              <small>{{ group.artifacts.length }}</small>
            </button>
          </div>
        </div>
        <span class="review-badge">{{ stageStatusLabel }}</span>
      </div>
      <div v-if="artifacts.length" class="artifact-list horizontal-artifact-list">
        <button
          v-for="artifact in artifacts"
          :key="`${artifact.name}-${artifact.path}`"
          type="button"
          class="artifact-item"
          @click="openArtifactPreview(artifact)"
        >
          <span class="artifact-icon">{{ artifact.type === 'markdown' ? 'MD' : artifact.type === 'html' ? 'HTML' : artifact.type === 'image' ? 'IMG' : 'FILE' }}</span>
          <span class="artifact-copy">
            <strong>{{ artifact.name }}</strong>
            <small>{{ artifact.type }}</small>
          </span>
          <em>预览</em>
        </button>
      </div>
      <p v-else class="empty-state">{{ hasReview ? '当前阶段暂无可展示产物，仍可确认通过或提交修订反馈。' : '暂无阶段产物，等待工作流生成后自动展示在这里。' }}</p>
    </section>
  </section>
</template>

<style scoped>
.stage-review-panel {
  background: transparent;
  border: 0;
  border-radius: 0;
  padding: 0;
  box-shadow: none;
}

.review-head,
.card-title-row,
.revision-toolbar,
.revision-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.review-head {
  margin-bottom: 12px;
}

.review-head h2 {
  margin: 0 0 6px;
  color: #111827;
  font-size: 22px;
  line-height: 1.2;
}

.review-head p,
.empty-state {
  margin: 0;
  color: #5f6b7a;
  font-size: 13px;
  line-height: 1.7;
}

.review-badge {
  white-space: nowrap;
  border-radius: 999px;
  background: #fff7ed;
  color: #f97316;
  padding: 7px 12px;
  font-size: 12px;
  font-weight: 700;
}

.artifact-list-card,
.revision-card {
  border: 1px solid #edf1f7;
  border-radius: 14px;
  background: #fff;
}

.artifact-list-card {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 12px;
}

.compact-artifact-card {
  margin-top: 12px;
}

.card-title-row h2{
  font-size: 16px;
}

.card-title-row h3 {
  margin: 0;
  color: #111827;
  font-size: 15px;
}

.card-title-row p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.card-title-row span {
  color: #94a3b8;
  font-size: 12px;
}

.artifact-stage-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.artifact-stage-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #e5eaf1;
  border-radius: 999px;
  background: #ffffff;
  color: #64748b;
  padding: 7px 10px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.artifact-stage-tab[data-active="true"] {
  border-color: #2563eb;
  background: #eef5ff;
  color: #2563eb;
}

.artifact-stage-tab small {
  min-width: 18px;
  height: 18px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #64748b;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
}

.artifact-stage-tab[data-active="true"] small {
  background: #2563eb;
  color: #ffffff;
}

.artifact-list {
  display: flex;
  flex: 1;
  min-height: 0;
  gap: 8px;
  margin-top: 10px;
}

.horizontal-artifact-list {
  flex-direction: row;
  flex-wrap: wrap;
  overflow: visible;
}

.artifact-item {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  flex: 0 0 260px;
  width: 260px;
  min-height: 58px;
  border: 1px solid #edf1f7;
  border-radius: 10px;
  background: #ffffff;
  padding: 9px;
  text-align: left;
  cursor: pointer;
}

.artifact-item[data-active="true"] {
  border-color: #dbeafe;
  background: #f8fbff;
}

.artifact-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 800;
}

.artifact-copy {
  min-width: 0;
}

.artifact-copy strong,
.artifact-copy small {
  display: block;
}

.artifact-copy strong {
  overflow: hidden;
  color: #111827;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.artifact-copy small {
  color: #64748b;
  margin-top: 3px;
}

.artifact-item em {
  border: 1px solid #e5eaf1;
  border-radius: 999px;
  padding: 5px 8px;
  color: #64748b;
  font-size: 11px;
  font-style: normal;
}

.collapse-button {
  border: 1px solid #dbe3ef;
  border-radius: 10px;
  background: #fff;
  color: #2563eb;
  padding: 8px 12px;
  cursor: pointer;
}

.revision-card {
  padding: 22px 28px 24px;
}

.discussion-context {
  margin-bottom: 14px;
}

.discussion-context h2 {
  margin: 0;
  color: #111827;
  font-size: 16px;
  line-height: 1.35;
}

.discussion-context p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  font-weight: 400;
}

.stage-discussion-scroll {
  height: min(620px, calc(100vh - 300px));
  min-height: 460px;
  overflow: auto;
  border: 1px solid #edf1f7;
  border-radius: 14px;
  background: #ffffff;
  padding: 14px;
}

.stage-discussion-scroll::-webkit-scrollbar {
  width: 6px;
}

.stage-discussion-scroll::-webkit-scrollbar-thumb {
  background: #d5deec;
  border-radius: 999px;
}

.stage-message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 10px;
}

.stage-message-row:last-child {
  margin-bottom: 0;
}

.user-message-row {
  justify-content: flex-end;
}

.avatar {
  width: 30px;
  height: 30px;
  border-radius: 999px;
  background: #4f46e5;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;
}

.user-avatar {
  background: #2563eb;
}

.stage-message-bubble {
  max-width: 82%;
  border-radius: 12px;
  background: #f4f7ff;
  padding: 10px 12px;
  color: #0f172a;
  font-size: 13px;
}

.stage-message-row:not(.user-message-row) .stage-message-bubble {
  max-width: min(940px, 92%);
}

.stage-message-time {
  margin-bottom: 6px;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.openspec-choice-list,
.openspec-file-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 8px;
  margin-top: 12px;
}

.openspec-choice-list button {
  border: 1px solid #bfdbfe;
  border-radius: 10px;
  background: #eff6ff;
  color: #1d4ed8;
  padding: 9px 10px;
  cursor: pointer;
  text-align: left;
  font-size: 13px;
  font-weight: 700;
}

.openspec-choice-list button:hover:not(:disabled) {
  border-color: #60a5fa;
  background: #dbeafe;
}

.openspec-choice-list button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.openspec-file-list button {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  border: 1px solid #dbeafe;
  border-radius: 10px;
  background: #ffffff;
  padding: 9px 10px;
  color: #1e3a8a;
  cursor: pointer;
  text-align: left;
}

.openspec-file-list button:hover {
  border-color: #93c5fd;
  background: #eff6ff;
}

.openspec-file-list span {
  flex-shrink: 0;
  border-radius: 999px;
  background: #dbeafe;
  padding: 2px 7px;
  font-size: 12px;
  font-weight: 700;
}

.openspec-file-list strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.discussion-option-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.discussion-option-list button {
  border: 1px solid #dbeafe;
  border-radius: 999px;
  background: #ffffff;
  color: #2563eb;
  padding: 7px 11px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.discussion-option-list button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.feedback-input-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 44px;
  align-items: stretch;
  gap: 8px;
  margin-top: 8px;
}

.feedback-input-row:has(.cancel-feedback-button) {
  grid-template-columns: minmax(0, 1fr) 44px auto;
}

.revision-card input {
  width: 100%;
  height: 42px;
  box-sizing: border-box;
  border: 1px solid #dfe7f3;
  border-radius: 10px;
  padding: 0 12px;
  font-size: 13px;
  outline: none;
}

.revision-type-options {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.revision-type-options button {
  display: block;
  width: 100%;
  border: 1px solid #e5eaf1;
  border-radius: 12px;
  background: #fff;
  color: #334155;
  padding: 10px 12px;
  text-align: left;
  cursor: pointer;
}

.revision-type-options button[data-active="true"] {
  border-color: #2563eb;
  background: #eef5ff;
  color: #1d4ed8;
}

.revision-type-options strong,
.revision-type-options small {
  display: block;
}

.revision-type-options strong {
  font-size: 13px;
}

.revision-type-options small {
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.send-feedback-button {
  border: none;
  border-radius: 12px;
  background: #eef5ff;
  color: #2563eb;
  font-weight: 800;
  cursor: pointer;
}

.send-feedback-button {
  min-width: 44px;
  min-height: 42px;
  padding: 0 14px;
  font-size: 13px;
}

.feedback-input-row .send-feedback-button {
  width: 44px;
  padding: 0;
  font-size: 18px;
}

.cancel-feedback-button,
.revision-next-step-panel button {
  border: 1px solid #dbe3ef;
  border-radius: 12px;
  background: #ffffff;
  color: #475569;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.revision-next-step-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
  border: 1px solid #dbeafe;
  border-radius: 12px;
  background: #f8fbff;
  padding: 10px 12px;
}

.revision-next-step-panel div {
  min-width: 0;
}

.revision-next-step-panel strong,
.revision-next-step-panel span {
  display: block;
}

.revision-next-step-panel strong {
  color: #1e293b;
  font-size: 13px;
}

.revision-next-step-panel span {
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.revision-next-step-panel button {
  flex: 0 0 auto;
  min-height: 36px;
}

.revision-next-step-panel button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.revision-toolbar {
  margin-top: 8px;
  align-items: stretch;
  gap: 10px;
}

.collapse-button {
  flex: 1;
  min-height: 42px;
  text-align: left;
  color: #2563eb;
  font-weight: 800;
  background: #ffffff;
}

.collapse-button small {
  color: #64748b;
  font-weight: 600;
}

.collapse-chevron {
  display: inline-block;
  width: 7px;
  height: 7px;
  margin: 0 8px 2px 2px;
  border-right: 2px solid #2563eb;
  border-bottom: 2px solid #2563eb;
  transform: rotate(45deg);
}

.collapse-chevron[data-open="true"] {
  margin-bottom: -2px;
  transform: rotate(-135deg);
}

.revision-actions {
  flex-wrap: nowrap;
  justify-content: flex-start;
  gap: 10px;
  margin-top: 10px;
}

.revision-actions button {
  min-width: 166px;
  min-height: 58px;
  border: 1px solid #e5eaf1;
  border-radius: 10px;
  background: #ffffff;
  color: #111827;
  padding: 10px 12px 10px 46px;
  cursor: pointer;
  text-align: left;
  font-size: 13px;
  font-weight: 700;
  position: relative;
  box-shadow: 0 6px 16px rgba(15, 23, 42, 0.04);
}

.revision-actions button strong {
  display: block;
  color: #111827;
  font-size: 14px;
  line-height: 1.2;
}

.revision-action-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  width: 26px;
  height: 26px;
  border-radius: 9px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transform: translateY(-50%);
  font-size: 13px;
  font-weight: 900;
}

.revision-action-icon.chat {
  background: #eaf2ff;
  color: #2563eb;
}

.revision-action-icon.plan {
  background: #f4ecff;
  color: #7c3aed;
}

.revision-action-icon.apply {
  background: #eef5ff;
  color: #2563eb;
}

.revision-action-icon.archive {
  background: #eafbf1;
  color: #16a34a;
}

.revision-actions button:disabled,
.approve-button:disabled,
.send-feedback-button:disabled,
.revision-type-options button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.revision-actions small {
  color: #64748b;
  font-size: 11px;
  font-weight: 500;
}

.approve-button {
  width: 300px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #2563eb 0%, #5b5ff8 100%);
  color: #fff;
  min-height: 58px;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.22);
}

.approve-button span {
  display: block;
  font-size: 12px;
  font-weight: 500;
  margin-top: 5px;
  opacity: 0.9;
}

@media (max-width: 900px) {
  .artifact-item {
    flex-basis: 220px;
    width: 220px;
  }

  .revision-toolbar,
  .revision-actions,
  .feedback-input-row {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .approve-button {
    width: 100%;
  }
}
</style>
