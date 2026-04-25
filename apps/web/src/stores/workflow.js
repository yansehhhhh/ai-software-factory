import { defineStore } from "pinia";
import { defaultPipeline } from "@/workflow/defaultPipeline";

export const useWorkflowStore = defineStore("workflow", {
  state: () => ({
    pipeline: defaultPipeline
  }),
  getters: {
    readyCount: (state) => state.pipeline.filter((item) => item.status === "ready").length
  }
});
