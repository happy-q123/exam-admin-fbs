package com.question.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domain.dto.QuestionDto;
import com.domain.entity.Question;

public interface QuestionOptionService extends IService<Question> {

    /**
     * description 添加一个问题
     * author zzq
     * date 2025/12/20 13:05
     * param
     * return 插入成功后，返回的 id
     */
    Long insert(QuestionDto questionDto);
}
