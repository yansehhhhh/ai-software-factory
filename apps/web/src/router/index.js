import { createRouter, createWebHistory } from "vue-router";
import HomeView from "@/views/HomeView.vue";
import ClaudeEnvironmentView from "@/views/ClaudeEnvironmentView.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      name: "home",
      component: HomeView
    },
    {
      path: "/claude-environment",
      name: "claude-environment",
      component: ClaudeEnvironmentView
    }
  ]
});

export default router;
