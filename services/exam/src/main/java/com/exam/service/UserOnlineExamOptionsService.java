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
}
