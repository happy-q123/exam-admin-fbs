package com.ai.linshi;

import org.apache.seata.spring.annotation.GlobalTransactional;
import io.seata.core.context.RootContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@Slf4j
public class SeataTestService {

    @Autowired
    private SeataTestMapper seataTestMapper;

    /**
     * 测试 Seata 分布式事务回滚
     * 场景：插入数据成功 -> 模拟异常 -> 期望数据被自动回滚
     */
    @GlobalTransactional(name = "test-seata-rollback", rollbackFor = Exception.class)
    public void createWithRollback() {
        // 1. 打印当前 XID (全局事务ID)，如果为空说明 Seata 没介入
        log.info("【Seata测试】当前 XID: {}", RootContext.getXID());

        // 2. 正常执行 SQL 操作
        SeataTestEntity entity = new SeataTestEntity();
        entity.setName("Seata-Test-Data-" + System.currentTimeMillis());
        entity.setCreateTime(LocalDateTime.now());
        
        seataTestMapper.insert(entity);
        log.info("【Seata测试】数据已插入数据库 (此时本地事务已提交，undo_log 应有记录)");

        // --- 可以在这里打断点，去数据库看 seata_test_tab 和 undo_log 表 ---

        // 3. 模拟发生异常
        if (true) {
            throw new RuntimeException("【Seata测试】模拟业务异常，触发 Seata 回滚！");
        }
    }
}