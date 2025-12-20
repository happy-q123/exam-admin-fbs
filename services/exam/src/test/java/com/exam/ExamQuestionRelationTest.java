package com.exam;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.domain.dto.ExamQuestionRelationDto;
import com.domain.vo.ExamQuestionRelationVo;
import com.exam.service.ExamQuestionRelationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class ExamQuestionRelationTest {
    @Resource
    ExamQuestionRelationService examQuestionRelationService;

    @Test
    void testGetExamQuestions() {
        ExamQuestionRelationDto dto = ExamQuestionRelationDto.builder()
                .examId(1L)
                .build();
        Page<ExamQuestionRelationVo>vo=examQuestionRelationService.getExamQuestionsByExamId(dto);
        log.warn("结果：{}",vo.getRecords());
    }
}
