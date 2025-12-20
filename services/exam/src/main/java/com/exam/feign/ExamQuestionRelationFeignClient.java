package com.exam.feign;

import com.domain.dto.QuestionDto;
import com.domain.restful.RestResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "question-service")
public interface ExamQuestionRelationFeignClient {

    @GetMapping("/question/getListByIds")
    RestResponse<List<QuestionDto>> getListByIds(@RequestParam("ids") List<Long> ids);
}
