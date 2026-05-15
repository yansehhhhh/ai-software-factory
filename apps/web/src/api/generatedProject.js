import { apiClient } from "./client";

export async function previewGeneratedProject(path, projectName) {
  const response = await apiClient.post("/generated-projects/preview", { path, projectName }, { timeout: 360000 });
  return response.data;
}
