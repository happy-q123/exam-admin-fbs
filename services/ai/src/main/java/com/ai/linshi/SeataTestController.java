package com.ai.linshi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/seata/test")
public class SeataTestController {

    @Autowired
    private SeataTestService seataTestService;

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