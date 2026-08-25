package com.user.controller;

import com.domain.entity.AuditLog;
import com.domain.mapper.AuditLogMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/auditLog")
public class AuditLogController {

    private final AuditLogMapper auditLogMapper;

    public AuditLogController(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @GetMapping("/list")
    @PreAuthorize("@roleGuard.isAdmin(authentication)")
    public List<AuditLog> list() {
        return auditLogMapper.selectList(null);
    }
}
