package com.ai.agent_tool.tools;

import com.ai.feign.UserErrorQuestionFeignClient;
import com.domain.dto.QuestionDto;
import com.domain.record.agent.UserErrorQuestionRequest;
import com.domain.restful.RestResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * description 为agent定义工具
 * author zzq
 * date 2026/1/28 20:23
*/
@Component
public class UserErrorQuestionFunction implements Function<UserErrorQuestionRequest, String> {
    private final UserErrorQuestionFeignClient userErrorQuestionFeignClient;
    private final ObjectMapper objectMapper;
    public UserErrorQuestionFunction(UserErrorQuestionFeignClient userErrorQuestionFeignClient, ObjectMapper objectMapper) {
        this.userErrorQuestionFeignClient = userErrorQuestionFeignClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String apply(UserErrorQuestionRequest userIdRecord) {
        // 调用 Feign 接口
        RestResponse<List<QuestionDto>> r = userErrorQuestionFeignClient.getErrorQuestionsByUserId(userIdRecord.userId());

        // 安全性检查：防止空指针
        List<QuestionDto> questionDtoList = (r != null && r.getData() != null) ? r.getData() : Collections.emptyList();

        try {
            // 3. 将 List 对象转换为 JSON 字符串
            return objectMapper.writeValueAsString(questionDtoList);
        } catch (JsonProcessingException e) {
            // 记录日志并返回错误提示给 AI，防止程序崩溃
            e.printStackTrace();
            return "获取错题列表时发生数据转换错误";
        }
    }
}
