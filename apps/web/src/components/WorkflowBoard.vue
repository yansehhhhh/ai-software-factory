<script setup>
defineProps({
  stages: {
    type: Array,
    required: true
  }
});

const statusLabel = {
  ready: "就绪",
  queued: "排队中",
  running: "运行中",
  done: "完成"
};
</script>

<template>
  <section class="workflow-board" aria-label="AI factory workflow">
    <article v-for="stage in stages" :key="stage.id" class="workflow-step">
      <div class="step-status" :data-status="stage.status">
        {{ statusLabel[stage.status] || stage.status }}
      </div>
      <h2>{{ stage.title }}</h2>
      <p>{{ stage.owner }}</p>
    </article>
  </section>
</template>
