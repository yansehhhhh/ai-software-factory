package com.aifactory.skill;

import com.aifactory.llm.LlmClient;
import com.aifactory.llm.LlmRequest;
import com.aifactory.llm.LlmResponse;
import com.aifactory.llm.ModelRouter;
import com.aifactory.service.DesignArtifactFactory;
import com.aifactory.service.PromptTemplateService;
import org.springframework.stereotype.Component;

@Component
public class UiGenerateSkillAdapter implements Skill {

    private final LlmClient llmClient;
    private final ModelRouter modelRouter;
    private final PromptTemplateService promptTemplateService;
    private final DesignArtifactFactory artifactFactory;

    public UiGenerateSkillAdapter(
            LlmClient llmClient,
            ModelRouter modelRouter,
            PromptTemplateService promptTemplateService,
            DesignArtifactFactory artifactFactory
    ) {
        this.llmClient = llmClient;
        this.modelRouter = modelRouter;
        this.promptTemplateService = promptTemplateService;
        this.artifactFactory = artifactFactory;
    }

    @Override
    public String id() {
        return "ui-generate-skill";
    }

    @Override
    public String description() {
        return "Generate UI guidance, pages, and component specs for the design stage.";
    }

    @Override
    public SkillExecution execute(SkillRequest request) {
        String requirement = String.valueOf(request.context().getOrDefault("requirement", request.prompt()));
        String model = modelRouter.resolveModel(request.taskType(), request.modelHint());
        String prompt = promptTemplateService.load("prompts/design-agent.md");
        LlmResponse response = llmClient.complete(new LlmRequest(request.taskType(), model, prompt, requirement, request.context()));
        SkillOutput output = artifactFactory.createUiOutput(requirement, response.content());
        return new SkillExecution(id(), "success", "UI generate skill completed successfully.", output);
    }
}
