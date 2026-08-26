package com.domain.aspect;

import com.domain.annotation.Audit;
import com.domain.entity.AuditLog;
import com.domain.mapper.AuditLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 审计日志切面
 *
 * @author zzq
 * @date 2026-06-11
 */
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Pointcut("@annotation(com.domain.annotation.Audit)")
    public void auditPointcut() {
    }

    @Around("auditPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long beginTime = System.currentTimeMillis();
        // 执行方法
        Object result = point.proceed();
        // 执行时长(毫秒)
        long time = System.currentTimeMillis() - beginTime;

        // 保存日志
        saveAuditLog(point, time);

        return result;
    }

    private void saveAuditLog(ProceedingJoinPoint joinPoint, long time) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            AuditLog auditLog = new AuditLog();
            Audit auditAnnotation = method.getAnnotation(Audit.class);
            if (auditAnnotation != null) {
                auditLog.setAction(auditAnnotation.value());
            }

            // 获取请求上下文信息
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setIpAddress(request.getRemoteAddr());

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
                    Object claimValue = jwtAuthenticationToken.getToken().getClaim("userId");
                    if (claimValue instanceof Number number) {
                        auditLog.setUserId(number.longValue());
                    } else if (claimValue != null) {
                        try {
                            auditLog.setUserId(Long.parseLong(String.valueOf(claimValue)));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                if (auditLog.getUserId() == null) {
                    String userIdStr = request.getHeader("X-User-Id");
                    if (userIdStr != null && !userIdStr.isBlank()) {
                        try {
                            auditLog.setUserId(Long.parseLong(userIdStr));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }

            // 提取方法参数描述
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                StringBuilder params = new StringBuilder();
                for (Object arg : args) {
                    if (arg != null) {
                        params.append(arg.toString()).append("; ");
                    }
                }
                auditLog.setRequestParams(params.length() > 255 ? params.substring(0, 255) : params.toString());
            }

            auditLog.setDuration(time);
            auditLog.setCreateTime(LocalDateTime.now());

            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.error("保存审计日志失败: {}", e.getMessage());
        }
    }
}
