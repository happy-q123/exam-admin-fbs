package com.question.service;

import com.domain.dto.QuestionDto;
import com.domain.entity.attribute.QuestionBody;
import com.domain.enums.QuestionDifficultyEnum;
import com.domain.enums.QuestionTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
public class QuestionOptionTest {
    @Resource
    private QuestionOptionService questionOptionService;

    @Test
    public void testInsert() {
        List<QuestionBody.Option> options=List.of(
                QuestionBody.Option.builder()
                .key("A")
                .val("Paris")
                .build(),
                QuestionBody.Option.builder()
                        .key("B")
                        .val("Paris")
                        .build(),
                QuestionBody.Option.builder()
                        .key("C")
                        .val("Paris")
                        .build(),
                QuestionBody.Option.builder()
                        .key("D")
                        .val("Paris")
                        .build());

        QuestionBody body = new QuestionBody("What is the capital of France?",
                null,options,"A","666");

        QuestionDto questionDto = QuestionDto.builder()
                .type(QuestionTypeEnum.SingleOption)
                .tags(List.of("History", "Geography"))
                .difficulty(QuestionDifficultyEnum.Difficult)
                .status(false)
                .latestUpdateId(111L)
                .creatorId(111L)
                .latestUpdateTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .body(body)
                .build();
       questionOptionService.insert(questionDto);
    }
}
