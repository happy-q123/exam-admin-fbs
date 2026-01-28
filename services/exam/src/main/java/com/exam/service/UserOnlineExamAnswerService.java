package com.exam.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domain.dto.UserOnlineExamAnswerDto;
import com.domain.entity.relation.UserOnlineExamAnswer;
import com.domain.vo.UserErrorQuestionsVo;

import java.util.List;

public interface UserOnlineExamAnswerService extends IService<UserOnlineExamAnswer> {
    /**
     * description 记录（或更新）用户的考试答案
     * author zzq
     * date 2026/1/7 16:33
     */
    void saveAnswer(UserOnlineExamAnswerDto userOnlineExamAnswerDto);

    /**
     * description 更新用户的考试答案
     * author zzq
     * date 2026/1/7 17:56
     */
    void updateAnswer(UserOnlineExamAnswerDto userOnlineExamAnswerDto);

    /*
     * description 根据用户id获取答案记录
     * author zzq
     * date 2026/1/28 18:11
    */
    List<UserErrorQuestionsVo> getUserAnswersByUserId(Long userId);
}
