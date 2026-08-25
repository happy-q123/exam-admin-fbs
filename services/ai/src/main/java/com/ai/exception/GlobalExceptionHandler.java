package com.ai.exception;

import com.domain.restful.RestResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public RestResponse<Void> handleBusinessException(IllegalArgumentException exception) {
        return RestResponse.fail(exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public RestResponse<Void> handleException(Exception exception) {
        log.error("AI 服务请求失败", exception);
        return RestResponse.fail("AI 服务暂时不可用，请稍后重试");
    }
}
