package com.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class CommonController {

    /**
     * 响应根路径请求，用于处理非预期的重定向并记录路径信息
     */
    @GetMapping("/")
    public String index(HttpServletRequest request) {
        log.info("接收到认证服务根路径请求，请求URI: {}", request.getRequestURI());
        return "Auth Service Online";
    }

    /**
     * 模拟获取图形验证码接口，返回 Mock 数据以避免登录页报错
     */
    @GetMapping("/getValidateCode")
    public com.domain.restful.RestResponse<java.util.Map<String, String>> getValidateCode() {
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("captchaId", "mock-captcha-id");
        // 返回 1x1 透明 GIF 的 Base64 数据
        data.put("imageSrc", "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");
        return com.domain.restful.RestResponse.success(data);
    }


}
