<script setup>
import { computed, onMounted, ref } from "vue";
import { fetchClaudeEnvironment } from "@/api/claude";
import Sidebar from "@/components/Sidebar.vue";
import Topbar from "@/components/Topbar.vue";

const environment = ref(null);
const loading = ref(true);
const pageError = ref("");

const permissionEntries = computed(() => Object.entries(environment.value?.permissions || {}));
const skillList = computed(() => environment.value?.availableSkills || []);

async function loadEnvironment() {
  loading.value = true;
  pageError.value = "";
  try {
    environment.value = await fetchClaudeEnvironment();
  } catch (error) {
    pageError.value = error.response?.data?.message || "无法获取 Claude Code 运行环境，请先启动 Runner 和后端服务。";
  } finally {
    loading.value = false;
  }
}

onMounted(loadEnvironment);
</script>

<template>
  <div class="app-layout">
    <Sidebar initial-active-item="claude" />

    <div class="main-shell">
      <Topbar />

      <main class="page-body">
        <section class="hero-card">
          <div>
            <p class="eyebrow">Claude Code 环境检查</p>
            <h1>运行环境状态</h1>
            <span>检查 Claude CLI、登录状态、工作目录、可用 skills 与执行权限。</span>
          </div>
          <button type="button" class="refresh-button" :disabled="loading" @click="loadEnvironment">
            {{ loading ? '检查中...' : '重新检查' }}
          </button>
        </section>

        <p v-if="pageError" class="error-line">{{ pageError }}</p>

        <section v-if="environment" class="status-grid">
          <article class="status-card">
            <span>Runner 状态</span>
            <strong :data-ok="environment.runnerOnline">{{ environment.runnerOnline ? '在线' : '离线' }}</strong>
          </article>
          <article class="status-card">
            <span>Claude CLI</span>
            <strong :data-ok="environment.cliInstalled">{{ environment.cliInstalled ? '已安装' : '未安装' }}</strong>
          </article>
          <article class="status-card">
            <span>登录状态</span>
            <strong :data-ok="environment.loggedIn">{{ environment.loggedIn ? '已登录' : '未登录' }}</strong>
          </article>
          <article class="status-card">
            <span>Workspace Root</span>
            <strong data-ok="true">{{ environment.workspaceRoot || '--' }}</strong>
          </article>
        </section>

        <section class="detail-grid">
          <article class="detail-card">
            <h2>当前工作目录</h2>
            <p>{{ environment?.workingDirectory || '--' }}</p>
          </article>

          <article class="detail-card">
            <h2>可用 Skills</h2>
            <div v-if="skillList.length > 0" class="chip-list">
              <span v-for="skill in skillList" :key="skill" class="chip">{{ skill }}</span>
            </div>
            <p v-else class="empty-tip">未发现可用 skills</p>
          </article>

          <article class="detail-card permissions-card">
            <h2>执行权限</h2>
            <div v-if="permissionEntries.length > 0" class="permission-list">
              <div v-for="([key, value]) in permissionEntries" :key="key" class="permission-row">
                <span>{{ key }}</span>
                <strong :data-ok="value">{{ value ? '允许' : '受限' }}</strong>
              </div>
            </div>
            <p v-else class="empty-tip">暂无权限信息</p>
          </article>
        </section>
      </main>
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
  padding: 0 18px 18px;
}

.page-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-card,
.status-card,
.detail-card {
  background: #ffffff;
  border: 1px solid #edf1f7;
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(15, 23, 42, 0.04);
}

.hero-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 24px;
}

.eyebrow {
  margin: 0 0 10px;
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
}

.hero-card h1 {
  margin: 0;
  color: #111827;
  font-size: 28px;
}

.hero-card span {
  display: block;
  margin-top: 10px;
  color: #64748b;
  font-size: 14px;
}

.refresh-button {
  border: none;
  border-radius: 12px;
  background: #2563eb;
  color: #ffffff;
  padding: 12px 18px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.refresh-button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.status-card {
  padding: 18px;
}

.status-card span,
.detail-card h2 {
  color: #64748b;
}

.status-card span {
  display: block;
  margin-bottom: 12px;
  font-size: 13px;
}

.status-card strong,
.permission-row strong {
  font-size: 20px;
  color: #0f172a;
}

.status-card strong[data-ok="true"],
.permission-row strong[data-ok="true"] {
  color: #16a34a;
}

.status-card strong[data-ok="false"],
.permission-row strong[data-ok="false"] {
  color: #dc2626;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1fr;
  gap: 16px;
}

.detail-card {
  padding: 20px;
}

.detail-card h2 {
  margin: 0 0 14px;
  font-size: 16px;
}

.detail-card p {
  margin: 0;
  color: #111827;
  line-height: 1.6;
  word-break: break-all;
}

.chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.chip {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: #eef5ff;
  color: #165dff;
  font-size: 13px;
  font-weight: 600;
}

.permission-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.permission-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #edf1f7;
}

.permission-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.permission-row span {
  color: #334155;
  font-size: 14px;
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

@media (max-width: 1180px) {
  .status-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .hero-card {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
