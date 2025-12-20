package com.question.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domain.dto.QuestionDto;
import com.domain.entity.Question;

import java.util.List;

public interface QuestionOptionService extends IService<Question> {

    /**
     * description 添加一个问题
     * author zzq
     * date 2025/12/20 13:05
     * param
     * return 插入成功后，返回的 id
     */
    Long insert(QuestionDto questionDto);

    /**
     * description 根据ids获取问题列表
     * author zzq
     * date 2025/12/20 16:55
     * param * @param null
     * return
     */
    List<QuestionDto> getListByIds(List<Long> idList);
}
