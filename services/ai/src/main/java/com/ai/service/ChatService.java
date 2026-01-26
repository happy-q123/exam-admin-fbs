package com.ai.service;

public interface ChatService {
    Object memoryChatWithJudge(String query);

    Object memoryChatFlow(String query);
}
