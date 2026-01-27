package com.ai.linshi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 1. @Tag：将这个 Controller 在文档中归类为一个分组
@Tag(name = "Seata分布式事务测试", description = "包含 Seata 回滚测试等相关接口")
@RestController
@RequestMapping("/seata/test")
public class SeataTestController {

    @Autowired
    private SeataTestService seataTestService;

    // 2. @Operation：描述这个具体接口的功能
    @Operation(summary = "测试事务回滚", description = "调用此接口会手动触发异常，用于验证 Seata 是否成功回滚了数据库数据")
    @GetMapping("/rollback")
    public String testRollback() {
        try {
            seataTestService.createWithRollback();
            return "执行成功 (未触发异常? 请检查代码)";
        } catch (Exception e) {
            return "已触发异常: " + e.getMessage() + " -> 请检查数据库，数据应该为空！";
        }
    }
}