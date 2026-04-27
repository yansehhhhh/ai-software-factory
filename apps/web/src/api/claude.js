import { apiClient } from "./client";

export async function fetchClaudeEnvironment() {
  const { data } = await apiClient.get("/claude/environment");
  return data;
}
