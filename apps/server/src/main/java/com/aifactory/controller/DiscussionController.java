package com.aifactory.controller;

import com.aifactory.dto.DiscussionChatResult;
import com.aifactory.dto.DiscussionMessage;
import com.aifactory.dto.DiscussionStartResult;
import com.aifactory.dto.StartWorkflowRequest;
import com.aifactory.dto.WorkflowStatus;
import com.aifactory.service.DiscussionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/discussion")
public class DiscussionController {

    private final DiscussionService discussionService;

    public DiscussionController(DiscussionService discussionService) {
        this.discussionService = discussionService;
    }

    @PostMapping("/start")
    public DiscussionStartResult start(@Valid @RequestBody StartWorkflowRequest request) {
        return discussionService.start(request.requirement());
    }

    @PostMapping("/chat")
    public DiscussionChatResult chat(@RequestBody Map<String, String> body) {
        String discussionId = body.get("discussionId");
        String message = body.get("message");
        if (discussionId == null || message == null) {
            throw new IllegalArgumentException("discussionId and message are required");
        }
        return discussionService.chat(discussionId, message);
    }

    @PostMapping("/confirm")
    public WorkflowStatus confirm(@RequestBody Map<String, String> body) {
        String discussionId = body.get("discussionId");
        if (discussionId == null) {
            throw new IllegalArgumentException("discussionId is required");
        }
        return discussionService.confirm(discussionId);
    }

    @GetMapping("/{id}/history")
    public List<DiscussionMessage> getHistory(@PathVariable String id) {
        return discussionService.getHistory(id);
    }
}