<script setup>
import { computed } from "vue";
import MarkdownIt from "markdown-it";
import DOMPurify from "dompurify";

const props = defineProps({
  content: {
    type: String,
    default: ""
  }
});

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true
});

const sanitizedHtml = computed(() => DOMPurify.sanitize(markdown.render(props.content || "")));
</script>

<template>
  <div class="markdown-message" v-html="sanitizedHtml"></div>
</template>

<style scoped>
.markdown-message {
  color: inherit;
  line-height: 1.7;
  overflow-wrap: anywhere;
}

.markdown-message :deep(p) {
  margin: 0 0 10px;
}

.markdown-message :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-message :deep(ul),
.markdown-message :deep(ol) {
  margin: 8px 0 10px 18px;
  padding: 0;
}

.markdown-message :deep(li + li) {
  margin-top: 4px;
}

.markdown-message :deep(pre) {
  margin: 10px 0;
  padding: 12px;
  overflow-x: auto;
  border-radius: 10px;
  background: #0f172a;
  color: #e2e8f0;
  font-size: 12px;
}

.markdown-message :deep(code) {
  padding: 2px 5px;
  border-radius: 5px;
  background: rgba(15, 23, 42, 0.08);
  font-family: "SFMono-Regular", Consolas, monospace;
  font-size: 0.92em;
}

.markdown-message :deep(pre code) {
  padding: 0;
  background: transparent;
  color: inherit;
}

.markdown-message :deep(table) {
  display: block;
  width: 100%;
  overflow-x: auto;
  border-collapse: collapse;
  margin: 10px 0;
  font-size: 12px;
}

.markdown-message :deep(th),
.markdown-message :deep(td) {
  border: 1px solid #dbe3ef;
  padding: 8px 10px;
  text-align: left;
  white-space: nowrap;
}

.markdown-message :deep(th) {
  background: #f8fafc;
  color: #111827;
  font-weight: 700;
}

.markdown-message :deep(blockquote) {
  margin: 10px 0;
  padding-left: 12px;
  border-left: 3px solid #bfdbfe;
  color: #475569;
}
</style>
