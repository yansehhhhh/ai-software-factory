import { apiClient } from "./client";

export async function fetchWorkflowStatus() {
  const response = await apiClient.get("/workflow/status");
  return response.data;
}

export async function startWorkflow(requirement) {
  const response = await apiClient.post("/workflow/start", { requirement });
  return response.data;
}

export async function fetchLogs() {
  const response = await apiClient.get("/logs");
  return response.data;
}

export async function clearLogs() {
  await apiClient.delete("/logs");
}

export async function fetchResult() {
  const response = await apiClient.get("/result");
  return response.data;
}
