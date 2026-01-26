package com.ai.service.agent;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AgentManager {
    private Map<String, AbstractAgentService> agentMap;

    public AgentManager(List<AbstractAgentService> agentServices) {
        agentMap = agentServices.stream().
                collect(Collectors.toMap(AbstractAgentService::getAgentName, agentService -> agentService));
    }

    public String doService(String query) {
        AbstractAgentService service = agentMap.get("CommonChatAgent");
        return service.execute(query).toString();
    }

    public String doService(String agentName,String query) {
        AbstractAgentService service = agentMap.get(agentName);
        return service.execute(query).toString();
    }
}
