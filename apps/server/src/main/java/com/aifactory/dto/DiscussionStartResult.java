package com.aifactory.dto;

import java.util.List;

public record DiscussionStartResult(
        String discussionId,
        String firstQuestion,
        List<DiscussionMessage> history
) {}