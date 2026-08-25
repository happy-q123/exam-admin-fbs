package com.domain.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * 基于访问令牌中的业务角色做方法级授权。
 * 角色由认证服务签发，不能以请求参数或前端菜单作为权限依据。
 */
public class RoleGuard {

    public boolean isTeacherOrAdmin(Authentication authentication) {
        String role = roleOf(authentication);
        return "teacher".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role);
    }

    public boolean isAdmin(Authentication authentication) {
        return "admin".equalsIgnoreCase(roleOf(authentication));
    }

    private String roleOf(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            Object role = jwtAuthenticationToken.getToken().getClaims().get("role");
            return role == null ? "" : String.valueOf(role).replaceFirst("^ROLE_", "");
        }
        return "";
    }
}
