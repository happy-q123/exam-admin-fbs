package com.ai.feign;

import com.domain.dto.QuestionDto;
import com.domain.restful.RestResponse;
import com.domain.vo.UserErrorQuestionsVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "exam-service")
public interface UserErrorQuestionFeignClient {



    //根据用户id获取错误题目列表
    @GetMapping("/exam/getExamErrorQuestions/{userId}")
    RestResponse<List<UserErrorQuestionsVo>> getErrorQuestionsByUserId(@PathVariable("userId") Long userId);

    /*
    下面两种写法都对
    1、参数风格
        //根据用户 id 获取错题列表
        @GetMapping("/exam/getExamErrorQuestions")
        RestResponse<List<UserErrorQuestionsVo>> getErrorQuestionsByUserId(@RequestParam("userId") Long userId);
    2、restful风格
        //根据用户id获取错误题目列表
        @GetMapping("/exam/getExamErrorQuestions/{userId}")
        RestResponse<List<QuestionDto>> getErrorQuestionsByUserId(@PathVariable("userId") Long userId);
    * */
}
