package com.ai.test;

import com.ai.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.ServletRequestBindingException;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsInvalidArgumentsToBadRequest() {
        var response = handler.handleBusinessException(new IllegalArgumentException("请求不能为空"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getCode());
    }

    @Test
    void mapsOwnershipFailuresToForbidden() {
        var response = handler.handleAccessDenied(new AccessDeniedException("无权访问该会话"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().getCode());
    }

    @Test
    void mapsMissingResourcesToNotFound() {
        var response = handler.handleNotFound(new NoSuchElementException("运行记录不存在"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getCode());
    }

    @Test
    void mapsUnreadableRequestBodiesToBadRequest() {
        var response = handler.handleUnreadableRequest(new HttpMessageNotReadableException("invalid body", (org.springframework.http.HttpInputMessage) null));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getCode());
    }

    @Test
    void mapsInvalidRequestParametersToBadRequest() {
        var response = handler.handleInvalidRequestParameter(new ServletRequestBindingException("missing parameter"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getCode());
    }

    @Test
    void mapsStateFailuresToConflict() {
        var response = handler.handleConflict(new IllegalStateException("考试已提交"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().getCode());
    }

    @Test
    void mapsUnexpectedFailuresToInternalServerError() {
        var response = handler.handleException(new RuntimeException("secret internal details"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getCode());
        assertEquals("AI 服务暂时不可用，请稍后重试", response.getBody().getMessage());
    }
}
