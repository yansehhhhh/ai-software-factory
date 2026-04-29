<script setup>
import { computed } from "vue";

const props = defineProps({
  result: {
    type: Object,
    default: () => ({ available: false })
  }
});

const emit = defineEmits(["openLink"]);

const stageLabels = {
  requirement: "需求阶段产物",
  "ui-prototype": "UI原型",
  architecture: "架构设计",
  database: "数据库设计",
  "test-docs": "测试",
  misc: "其他文档",
  code: "代码与测试"
};

const artifactNameMap = {
  "PRD.md": "prd",
  "PRD.docx": "需求文档",
  "UI-Design-Spec.md": "UI设计规范",
  "UI-Design.svg": "UI设计图",
  "业务流程图.puml": "业务流程图",
  "信息架构图.puml": "信息架构图",
  "页面流转图.puml": "页面流转图",
  "页面流转图.svg": "页面流转图预览",
  "术语表.md": "术语表",
  "组件清单.md": "组件清单",
  "交互说明.md": "交互说明",
  "响应式断点参考.md": "响应式断点参考",
  "系统架构设计.md": "系统架构设计",
  "系统架构图.puml": "系统架构图",
  "系统架构图.svg": "系统架构图预览",
  "部署架构.md": "部署架构",
  "技术选型.md": "技术选型",
  "接口清单.md": "接口清单",
  "openapi.yaml": "OpenAPI 定义",
  "OpenAPI.yaml": "OpenAPI 定义",
  "数据字典.md": "数据字典",
  "schema.sql": "建表脚本",
  "migration.sql": "数据迁移脚本",
  "版本说明.md": "版本说明"
};

const artifactTypeMap = {
  markdown: "文档",
  word: "Word",
  image: "图片",
  plantuml: "流程图",
  html: "HTML",
  yaml: "YAML",
  sql: "SQL",
  directory: "目录",
  zip: "压缩包"
};

const stageOrder = ["requirement", "ui-prototype", "architecture", "database", "test-docs", "misc", "code"];

const normalizedArtifacts = computed(() => {
  return (props.result?.artifacts || []).map((artifact) => {
    const normalizedStage = artifact.stage === "brainstorming"
      ? "requirement"
      : artifact.stage === "writing-plans"
        ? "ui-prototype"
        : artifact.stage;

    const normalizedName = artifactNameMap[artifact.name]
      || (artifact.name === "生成项目" && artifact.type === "directory"
        ? artifact.path.includes("/frontend")
          ? "前端代码"
          : artifact.path.includes("/backend")
            ? "后端代码"
            : "前端代码 / 后端代码"
        : artifact.name);

    return {
      ...artifact,
      stage: stageLabels[normalizedStage] ? normalizedStage : "code",
      name: normalizedName,
      type: artifactTypeMap[artifact.type] || artifact.type
    };
  });
});

const groupedArtifacts = computed(() => {
  const groups = Object.fromEntries(stageOrder.map((stage) => [stage, []]));
  for (const artifact of normalizedArtifacts.value) {
    const stage = groups[artifact.stage] ? artifact.stage : "code";
    groups[stage].push(artifact);
  }
  return stageOrder.map((stage) => ({
    stage,
    label: stageLabels[stage],
    artifacts: groups[stage]
  }));
});

const markdownSummary = computed(() => {
  const markdown = props.result?.prdMarkdown || "";
  return markdown.length > 1200 ? `${markdown.slice(0, 1200)}...` : markdown;
});

function openLink(url) {
  if (url) {
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

    <div class="artifact-groups">
      <div v-for="group in groupedArtifacts" :key="group.stage" class="artifact-group">
        <h3>{{ group.label }}</h3>
        <div v-if="group.artifacts.length" class="artifact-list">
          <button
            v-for="artifact in group.artifacts"
            :key="`${artifact.stage}-${artifact.path}`"
            type="button"
            class="artifact-item"
            @click="openLink(artifact.path)"
          >
            <span class="artifact-name">{{ artifact.name }}</span>
            <small class="artifact-type">{{ artifact.type }}</small>
          </button>
        </div>
        <p v-else class="empty-artifacts">等待生成</p>
      </div>
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

    <div v-if="markdownSummary" class="markdown-preview">
      <h3>PRD / UI 摘要</h3>
      <pre>{{ markdownSummary }}</pre>
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

.artifact-groups {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 16px;
}

.artifact-group {
  border: 1px solid #edf1f7;
  border-radius: 16px;
  background: #fbfdff;
  padding: 16px;
  min-width: 0;
}

.artifact-group h3,
.markdown-preview h3 {
  margin: 0 0 12px;
  color: #111827;
  font-size: 14px;
}

.artifact-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.artifact-item {
  border: 1px solid #e5eaf1;
  border-radius: 12px;
  background: #ffffff;
  padding: 12px 14px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  width: 100%;
  text-align: left;
  cursor: pointer;
}

.artifact-item:hover {
  border-color: #bfdbfe;
  background: #f8fbff;
}

.artifact-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #1f2937;
  font-size: 14px;
  font-weight: 700;
}

.artifact-type {
  border-radius: 999px;
  background: #eef5ff;
  color: #2563eb;
  font-size: 12px;
  padding: 4px 8px;
}

.empty-artifacts {
  margin: 0;
  color: #94a3b8;
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

.markdown-preview {
  margin-top: 16px;
  border: 1px solid #edf1f7;
  border-radius: 14px;
  background: #0f172a;
  padding: 14px;
}

.markdown-preview h3 {
  color: #e5e7eb;
}

.markdown-preview pre {
  margin: 0;
  color: #d1d5db;
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  max-height: 260px;
  overflow: auto;
}

@media (max-width: 1180px) {
  .artifact-groups,
  .result-actions {
    grid-template-columns: 1fr;
  }
}
</style>
