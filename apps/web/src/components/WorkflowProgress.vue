<script setup>
import { computed } from "vue";

const props = defineProps({
  status: {
    type: Object,
    default: null
  }
});

const steps = computed(() => props.status?.steps || []);
const currentStage = computed(() => props.status?.currentStage || "--");
const currentArtifactType = computed(() => props.status?.currentArtifactType || "--");
const designProgressMessage = computed(() => props.status?.designProgressMessage || "等待任务启动");
</script>

<template>
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
      <article
        v-for="step in steps"
        :key="step.key"
        class="step-item"
        :data-status="step.status"
      >
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
  </section>
</template>