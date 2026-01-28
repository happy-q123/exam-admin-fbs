package com.domain.record.agent;

import java.util.List;

// 最终返回给 AI 的响应对象（或者直接返回 List<ErrorQuestionItem> 也可以）
// todo 先不弄这个类了 直接在toll中设置String过度
public record UserErrorQuestionResponse(
    List<ErrorQuestionItem> errorQuestions
) {}