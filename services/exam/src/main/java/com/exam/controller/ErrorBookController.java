package com.exam.controller;

import com.domain.entity.ErrorBook;
import com.domain.entity.Question;
import com.domain.restful.RestResponse;
import com.domain.vo.ErrorBookVo;
import com.exam.service.ErrorBookService;
import com.exam.service.QuestionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/errorBook")
public class ErrorBookController {

    @Autowired
    private ErrorBookService errorBookService;

    @Autowired
    private QuestionService questionService;

    @GetMapping({"/list", "/list/{userId}"})
    public RestResponse<List<ErrorBookVo>> listUserErrors(@AuthenticationPrincipal Jwt jwt,
                                                          @PathVariable(required = false) Long userId) {
        Long currentUserId = currentUserId(jwt);
        if (currentUserId == null) {
            return RestResponse.fail("token中无userId");
        }
        if (userId != null && !currentUserId.equals(userId)) {
            return RestResponse.fail(403, "只能查看自己的错题");
        }
        List<ErrorBook> errors = errorBookService.lambdaQuery()
                .eq(ErrorBook::getUserId, currentUserId)
                .orderByDesc(ErrorBook::getCreateTime)
                .list();
        Map<Long, Question> questions = errors.stream()
                .map(ErrorBook::getQuestionId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toMap(id -> id, questionService::getById, (first, second) -> first));
        List<ErrorBookVo> result = errors.stream().map(error -> {
            Question question = questions.get(error.getQuestionId());
            return ErrorBookVo.builder()
                    .errorId(error.getErrorId())
                    .userId(error.getUserId())
                    .questionId(error.getQuestionId())
                    .examId(error.getExamId())
                    .type(question == null ? null : question.getType())
                    .difficulty(question == null ? null : question.getDifficulty())
                    .body(question == null ? null : question.getBody())
                    .createTime(error.getCreateTime())
                    .updateTime(error.getUpdateTime())
                    .build();
        }).toList();
        return RestResponse.success(result);
    }

    @DeleteMapping("/remove/{errorId}")
    public RestResponse<Boolean> removeError(@AuthenticationPrincipal Jwt jwt, @PathVariable Long errorId) {
        Long currentUserId = currentUserId(jwt);
        ErrorBook errorBook = errorBookService.getById(errorId);
        if (currentUserId == null || errorBook == null) {
            return RestResponse.fail("错题不存在");
        }
        if (!currentUserId.equals(errorBook.getUserId())) {
            return RestResponse.fail(403, "只能删除自己的错题");
        }
        return RestResponse.success(errorBookService.removeById(errorId));
    }

    private Long currentUserId(Jwt jwt) {
        if (jwt == null || jwt.getClaim("userId") == null) {
            return null;
        }
        Object value = jwt.getClaim("userId");
        return value instanceof Number ? ((Number) value).longValue() : Long.valueOf(String.valueOf(value));
    }
}
