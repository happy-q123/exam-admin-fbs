package com.question.controller;

import com.domain.dto.QuestionDto;
import com.domain.entity.Question;
import com.domain.restful.RestResponse;
import com.question.service.QuestionOptionService;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * description 问题操作controller
 * author zzq
 * date 2025/12/20 12:12
 */
@RestController
public class QuestionOptionController {
    private final QuestionOptionService questionOptionService;

    public QuestionOptionController(QuestionOptionService questionOptionService) {
        this.questionOptionService = questionOptionService;
    }

    /**
     * description 添加一个问题
     * author zzq
     * date 2025/12/20 14:18
     * param
     * return 成功返回插入记录的时生成的id
     */
    @PostMapping("/insertOne")
    public RestResponse<Map<String,String>> insertOne(@AuthenticationPrincipal Jwt jwt, @RequestBody QuestionDto dto) {
        Long userId = jwt.getClaim("userId");
        if(userId==null)
            return RestResponse.fail("token中无userId");
        dto.setCreatorId(userId);
        Long result=questionOptionService.insert(dto);
        String message=result==null?"添加失败":"添加成功";
        return RestResponse.success(message, Map.of("id",String.valueOf(result)));
    }

    /**
     * description 根据问题id列表获取问题列表
     * author zzq
     * date 2025/12/20 17:10
     * param
     * return
     */
    @GetMapping("/getListByIds")
    public RestResponse<List<QuestionDto>> getListByIds(@RequestParam("ids") List<Long> ids) {
        return RestResponse.success("查询成功", questionOptionService.getListByIds(ids));
    }
}
