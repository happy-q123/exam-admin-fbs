package com.ai.feign;

import com.domain.dto.QuestionDto;
import com.domain.restful.RestResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "question-service")
public interface UserErrorQuestionFeignClient {



    //根据用户id获取错误题目列表
    @GetMapping("/question/getErrorQuestionsByUserId")
    RestResponse<List<QuestionDto>> getErrorQuestionsByUserId(@RequestParam("id") Long id);

//    //根据用户id获取错误题目列表
//    @GetMapping("/question/getErrorQuestionsByUserId")
//    RestResponse<List<QuestionDto>> getListByIds(@RequestParam("ids") List<Long> ids);
}
