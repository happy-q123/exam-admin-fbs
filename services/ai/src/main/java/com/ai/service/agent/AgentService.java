package com.ai.service.agent;

import org.springframework.ai.chat.prompt.ChatOptions;

public interface AgentService {

    /**
     * description agent的实际行为
     * author zzq
     * date 2025/12/14 21:04
     * param
     * return
     */
    Object execute(String query);

    /**
     * description agent的实际行为，并允许临时设置options配置
     * author zzq
     * date 2025/12/14 21:04
     * param
     * return
     */
    Object execute(String query, ChatOptions chatOptions);
}
