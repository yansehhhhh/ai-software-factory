import { apiClient } from "./client";

function isArtifactApiUrl(path) {
  return typeof path === "string" && path.startsWith("/api/artifacts?");
}

function rawArtifactPath(path) {
  if (!isArtifactApiUrl(path)) return path;

  const query = path.split("?")[1] || "";
  return new URLSearchParams(query).get("path") || path;
}

export function artifactUrl(path) {
  if (!path) return "";
  return isArtifactApiUrl(path) ? path : `/api/artifacts?path=${encodeURIComponent(path)}`;
}

export async function fetchArtifactContent(path) {
  const response = await apiClient.get("/artifacts", {
    params: { path: rawArtifactPath(path) },
    responseType: "text"
  });
  return response.data;
}

export function artifactPath(path) {
  return rawArtifactPath(path);
}
