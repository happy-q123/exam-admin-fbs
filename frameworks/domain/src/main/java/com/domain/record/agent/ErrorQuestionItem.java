package com.domain.record.agent;

import java.util.List;

// 单个错题的信息结构
public record ErrorQuestionItem(
    String questionContent, // 题目题干
    String userAnswer,      // 用户填写的错误答案
    String correctAnswer,   // 正确答案（可选，通常 AI 知道正确答案会解释得更好）
    String explanation      // 解析（可选）
) {}

