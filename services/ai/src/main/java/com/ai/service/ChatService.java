package com.ai.service;

public interface ChatService {
    //带结果判断的聊天
    Object memoryChatWithJudge(String query);

    //该方法作为临时测试，因为默认使用的用户id为0
    Object memoryChatFlow(String query);

    //带结果判断，且能自我修正的方法。
    // 使用用户id进行搜索内容区分
    Object memoryChatFlow(Long userId,String query);
}
