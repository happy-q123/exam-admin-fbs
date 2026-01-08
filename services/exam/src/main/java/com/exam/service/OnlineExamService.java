package com.exam.service;

import com.domain.dto.UserOnlineExamOptionsDto;

import java.time.LocalDateTime;

public interface OnlineExamService {
    /**
     * description 判断用户是否能够再次进入考试
     * 检查用户是否报名->检查考试是否过期->检查是否初次进入->
     *                                  检查是否达到考试的最大重进次数->
     * author zzq
     * date 2026/1/8 11:38
     */
    void enterExam(Long userId, Long examId, LocalDateTime acquireTime);


    /**
     * description 检查用户进入考试的次数是否已达最大值
     * author zzq
     * date 2026/1/8 11:46
     */
    boolean checkUserEnterExamCountIsMax(Long userId, Long examId);

    /**
     * description 用户下线，且用户进入过考试时调用，用以产生用户退出考试的记录。调用前务必确保考试
     * author zzq
     * date 2026/1/8 14:17
     */
    void processUserDropOnline(UserOnlineExamOptionsDto dto);
}
