package com.domain.base;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.StringUtils;

/**
 * description pojo基类，提供分页字段以及排序功能
 * author zzq
 * date 2025/12/18 20:01
 */
@Data
public class BasePojo {
    //当前页
    private long current = 1;

    //每页数量
    private long size = 10;

    // 排序字段名 (例如 apply_time)
    private String orderByColumn;
    // 排序方式 (asc 或 desc)
    private String isAsc = "desc";

    public <T> Page<T> buildPage() {
        Page<T> page = new Page<>(this.current, this.size);
        // 如果前端传了排序字段，就加上排序规则
        if (StringUtils.hasText(orderByColumn)) {
            OrderItem orderItem = "asc".equalsIgnoreCase(isAsc)
                    ? OrderItem.asc(orderByColumn)
                    : OrderItem.desc(orderByColumn);
            page.addOrder(orderItem);
        }
        return page;
    }
}
