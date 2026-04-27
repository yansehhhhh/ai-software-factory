<script setup>
import { ref, watch, nextTick } from "vue";

const props = defineProps({
  logs: {
    type: Array,
    default: () => []
  },
  visible: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(["close", "clear"]);

const logPanel = ref(null);

function closeModal() {
  emit("close");
}

function clearLogs() {
  emit("clear");
}

// 自动滚动到底部
watch(() => props.logs.length, async () => {
  await nextTick();
  if (logPanel.value) {
    logPanel.value.scrollTop = logPanel.value.scrollHeight;
  }
});
</script>

<template>
  <div v-if="visible" class="modal-overlay" @click.self="closeModal">
    <div class="modal-content log-modal">
      <div class="modal-header">
        <h2>实时日志</h2>
        <button class="modal-close" @click="closeModal">×</button>
      </div>
      <div class="modal-toolbar">
        <span class="live-indicator"><span class="live-dot"></span> 实时</span>
        <button class="ghost-button" @click="clearLogs">清空</button>
      </div>
      <div ref="logPanel" class="log-panel">
        <p v-if="logs.length === 0" class="empty-log">暂无日志</p>
        <div
          v-for="entry in logs"
          :key="`${entry.time}-${entry.agent}-${entry.message}`"
          class="log-line"
          :data-level="entry.level"
        >
          <span>{{ entry.time }}</span>
          <strong>[{{ entry.agent }}]</strong>
          <em>{{ entry.message }}</em>
        </div>
      </div>
    </div>
  </div>
</template>