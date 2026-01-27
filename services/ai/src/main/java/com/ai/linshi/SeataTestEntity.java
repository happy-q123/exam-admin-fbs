package com.ai.linshi;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("seata_test_tab")
public class SeataTestEntity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private LocalDateTime createTime;
}