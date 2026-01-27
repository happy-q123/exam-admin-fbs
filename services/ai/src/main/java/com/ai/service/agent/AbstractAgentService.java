package com.ai.service.agent;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAgentService implements AgentService{
    protected ChatClient chatClient;

    protected List<Advisor> advisors=new ArrayList<>();

    @Getter
    protected String agentName;

    @Getter
    protected String agentDescription;

    @Getter
    protected String systemPrompt;

    protected ChatClient.Builder chatClientBuilder;

    public AbstractAgentService(ChatClient.Builder chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    @PostConstruct
    protected void initAll(){
        initProperties();
        initAdvisors();
        initChatClient(chatClientBuilder);
    }

    /**
     * description 初始化各种属性
     * author zzq
     * date 2025/12/14 21:51
     * param
     * return
     */
    protected void initProperties(){
        this.agentDescription="";
        this.agentName="";
        this.systemPrompt="";
    }

    /**
     * description 初始化chatClient
     * author zzq
     * date 2025/12/14 21:42
     * param
     * return
     */
    protected void initChatClient(ChatClient.Builder chatClientBuilder){
        if(chatClient!=null)
            throw new RuntimeException("chatClient已经不为null");

        ChatClient.Builder builder = chatClientBuilder
                .defaultSystem(this.systemPrompt);

        if (this.advisors != null && !this.advisors.isEmpty()) {
            builder.defaultAdvisors(this.advisors);
        }
        this.chatClient = builder.build();
    };

    /**
     * description 初始化Advisor
     * author zzq
     * date 2025/12/14 22:52
     * param
     * return
     */
    protected void initAdvisors() {}

    /**
     * description 重新设置chatClient
     * author zzq
     * date 2025/12/14 21:45
     * param
     * return
     */
    public void resetChatClient(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * description 重设默认系统提示
     * author zzq
     * date 2025/12/14 21:47
     * param prompt：新的系统提示词
     * return
     */
    public void resetDefaultSystemPrompt(String prompt) {
        this.systemPrompt = prompt;
        flushChatClient();
    }

    /**
     * description 重设agent的描述
     * author zzq
     * date 2025/12/14 21:47
     * param
     * return
     */
    public void resetAgentDescription(String description) {
        this.agentDescription=description;
    }

    /**
     * description 刷新advisor
     * author zzq
     * date 2025/12/14 23:01
     * param
     * return
     */
    public void flushChatClient(){
        ChatClient.Builder builder = this.chatClient.mutate()
                .defaultSystem(this.systemPrompt);
        if (this.advisors != null && !this.advisors.isEmpty()) {
            builder.defaultAdvisors(this.advisors);
        }
        this.chatClient = builder.build();
    }

    public void addAdvisor(Advisor advisor){
        advisors.add(advisor);
        flushChatClient();
    }

    public void removeAdvisor(Advisor advisor){
        if (advisors.isEmpty())
            return;
        advisors.remove(advisor);
        flushChatClient();
    }

    public void removeAllAdvisor(){
        if (advisors.isEmpty())
            return;
        advisors=new ArrayList<>();
        flushChatClient();
    }

    /**
     * description 返回不可变的、线程安全的Advisor列表
     * author zzq
     * date 2025/12/15 14:24
     * param
     * return
     */
    public List<Advisor> getAdvisors(){
        return new ArrayList<>(advisors);
    }

    // 假设你在接口里定义了这个方法，或者直接作为新方法添加
    public abstract Object execute(String query, String userId);
}