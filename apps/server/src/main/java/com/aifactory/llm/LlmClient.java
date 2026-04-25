package com.aifactory.llm;

public interface LlmClient {

    LlmResponse complete(LlmRequest request);
}
