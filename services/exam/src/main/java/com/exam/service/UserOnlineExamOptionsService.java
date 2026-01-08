package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domain.dto.UserOnlineExamOptionsDto;
import com.domain.entity.relation.UserOnlineExamOptions;

public interface UserOnlineExamOptionsService extends IService<UserOnlineExamOptions> {

    /**
     * description 保存用户在线考试的行为
     * author zzq
     */
    void saveUserOnlineExamOption(UserOnlineExamOptionsDto userOnlineExamOptionsDto);

    /**
     * description 获取用户进入考试的次数
     * author zzq
     * date 2026/1/8 11:19
     */
    Long getUserEnterExamCount(Long userId, Long examId);
}
