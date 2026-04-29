<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";

const props = defineProps({
  initialActiveItem: {
    type: String,
    default: "home"
  }
});

const router = useRouter();
const activeItem = ref(props.initialActiveItem);
const collapsed = ref(false);

const navItems = [
  { id: "home", icon: "⌂", label: "首页", route: "/" },
  { id: "model", icon: "⚙", label: "模型配置", route: "/model-config" },
  { id: "history", icon: "◷", label: "历史记录" }
];

const emit = defineEmits(["navigate"]);

function selectItem(item) {
  activeItem.value = item.id;
  emit("navigate", item.id);
  if (item.route) {
    router.push(item.route);
  }
}

function toggleSidebar() {
  collapsed.value = !collapsed.value;
}
</script>

<template>
  <aside :class="['sidebar', { collapsed }]">
    <div class="sidebar-header">
      <div class="brand-icon">AI</div>
      <div v-if="!collapsed" class="brand-copy">
        <strong>AI SOFTWARE FACTORY</strong>
        <h1 class="brand-title">AI 编排平台</h1>
      </div>
    </div>

    <nav class="sidebar-nav">
      <button
        v-for="item in navItems"
        :key="item.id"
        :class="['nav-item', { active: activeItem === item.id }]"
        :title="item.label"
        @click="selectItem(item)"
      >
        <span class="nav-rail"></span>
        <span class="nav-icon">{{ item.icon }}</span>
        <span v-if="!collapsed" class="nav-label">{{ item.label }}</span>
      </button>
    </nav>

    <button type="button" class="sidebar-footer" @click="toggleSidebar">
      <span :class="['footer-icon', { collapsed }]">≪</span>
      <span v-if="!collapsed">收起菜单</span>
    </button>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 220px;
  min-width: 220px;
  height: 100vh;
  position: sticky;
  top: 0;
  z-index: 10;
  background: #ffffff;
  color: #0f172a;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e8edf5;
  transition: width 0.2s ease, min-width 0.2s ease;
}

.sidebar.collapsed {
  width: 76px;
  min-width: 76px;
}

.sidebar-header {
  padding: 16px 16px 14px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.sidebar.collapsed .sidebar-header {
  justify-content: center;
  padding-inline: 10px;
}

.brand-icon {
  width: 38px;
  height: 38px;
  background: linear-gradient(180deg, #2f7cff 0%, #1f62f4 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 15px;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.18);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.brand-copy strong {
  color: #2f7cff;
  font-size: 13px;
  line-height: 1;
}

.brand-copy span {
  color: #111827;
  font-size: 15px;
  font-weight: 700;
}

.brand-title {
  color: #111827;
  font-size: 15px;
  font-weight: 700;
  margin: 0;
  line-height: 1;
}

.sidebar-nav {
  flex: 1;
  padding: 14px 8px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  background: transparent;
  border: none;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
}

.sidebar.collapsed .nav-item {
  justify-content: center;
  padding-inline: 10px;
}

.nav-item:hover {
  background: #f8fbff;
}

.nav-item.active {
  background: #eef5ff;
  color: #165dff;
}

.nav-rail {
  position: absolute;
  left: 0;
  top: 10px;
  bottom: 10px;
  width: 3px;
  border-radius: 999px;
  background: transparent;
}

.nav-item.active .nav-rail {
  background: #165dff;
}

.nav-icon {
  width: 20px;
  font-size: 18px;
  text-align: center;
  flex-shrink: 0;
}

.nav-label {
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
}

.sidebar-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 18px 22px;
  border: none;
  background: transparent;
  color: #64748b;
  font-size: 14px;
  cursor: pointer;
}

.sidebar.collapsed .sidebar-footer {
  justify-content: center;
  padding-inline: 10px;
}

.footer-icon {
  display: inline-flex;
  transition: transform 0.2s ease;
}

.footer-icon.collapsed {
  transform: rotate(180deg);
}
</style>
