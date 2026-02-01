package com.ai.service.common;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domain.entity.LocalRag;
import io.swagger.v3.oas.models.security.SecurityScheme;

import javax.annotation.Nullable;
import java.util.List;

public interface LocalRagService extends IService<LocalRag> {

    //todo 写一个根据内容插入数据库的方法
//    void insertRag(List<LocalRag> ragList);

    /**
     * description 根据messageSource搜索 rag ，参数为null表示搜索所有来源
     * author zzq
     * date 2026/2/1 16:40
    */
    List<LocalRag> searchSimilarRag(String vectorJson, @Nullable List<String> sources, @Nullable Integer limit);
}
