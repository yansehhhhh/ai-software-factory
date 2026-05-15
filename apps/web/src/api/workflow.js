import { apiClient } from "./client";

export async function fetchWorkflowStatus() {
  const response = await apiClient.get("/workflow/status");
  return response.data;
}

export async function startWorkflow(requirement) {
  const response = await apiClient.post("/workflow/start", { requirement });
  return response.data;
}

export async function approveCurrentStage() {
  const response = await apiClient.post("/workflow/review/approve");
  return response.data;
}

export async function submitStageRevision(feedback) {
  const response = await apiClient.post("/workflow/review/revise", { feedback });
  return response.data;
}

export async function runOpenSpecAction(action, feedback) {
  const response = await apiClient.post(`/workflow/review/openspec/${action}`, { feedback });
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

export async function fetchProjects() {
  const response = await apiClient.get("/workflow/projects");
  return response.data;
}

export async function recoverProject(projectName) {
  const response = await apiClient.post("/workflow/recover/project", { projectName });
  return response.data;
}

export async function retryFailedStage() {
  const response = await apiClient.post("/workflow/retry");
  return response.data;
}
