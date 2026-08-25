package com.auth.exception;

import com.domain.restful.RestResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获所有未知的异常（兜底）
     */
    @ExceptionHandler(Exception.class)
    public RestResponse<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统内部异常，请求路径: {}，错误信息: ", request.getRequestURI(), e);
        // 生产环境建议返回 "系统繁忙"，开发环境可以返回 e.getMessage() 方便调试
        return RestResponse.fail("系统内部异常: " + e.getMessage());
    }

//    /**
//     * 捕获你自己定义的业务异常（如果有的话）
//     */
//    @ExceptionHandler(MyBusinessException.class)
//    public R<Void> handleBusinessException(MyBusinessException e) {
//        log.warn("业务异常：{}", e.getMessage());
//        return R.fail(e.getMessage());
//    }
}