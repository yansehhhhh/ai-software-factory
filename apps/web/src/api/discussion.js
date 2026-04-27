import { apiClient } from "./client";

export async function startDiscussion(requirement) {
  const response = await apiClient.post("/discussion/start", { requirement });
  return response.data;
}

export async function chatDiscussion(discussionId, message) {
  const response = await apiClient.post("/discussion/chat", { discussionId, message });
  return response.data;
}

export async function confirmDiscussion(discussionId) {
  const response = await apiClient.post("/discussion/confirm", { discussionId });
  return response.data;
}

export async function fetchDiscussionHistory(discussionId) {
  const response = await apiClient.get(`/discussion/${discussionId}/history`);
  return response.data;
}