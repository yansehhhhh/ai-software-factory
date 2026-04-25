export function createWorkflowContext(input = {}) {
  return {
    requestId: input.requestId || "local",
    artifacts: input.artifacts || [],
    metadata: input.metadata || {}
  };
}
