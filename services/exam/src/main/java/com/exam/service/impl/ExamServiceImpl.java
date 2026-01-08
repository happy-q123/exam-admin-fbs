package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.dto.ExamDto;
import com.domain.entity.Exam;
import com.domain.entity.attribute.ExamSecuritySetting;
import com.domain.enums.redis.OnlineExamEnum;
import com.exam.mapper.ExamMapper;
import com.exam.service.ExamService;
import com.exam.service.UserApplyExamRelationService;
import jakarta.annotation.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class ExamServiceImpl extends ServiceImpl<ExamMapper, Exam> implements ExamService {
    private final UserApplyExamRelationService userApplyExamRelationService;
    private final StringRedisTemplate stringRedisTemplate;
    public ExamServiceImpl(UserApplyExamRelationService userApplyExamRelationService, StringRedisTemplate stringRedisTemplate) {
        this.userApplyExamRelationService = userApplyExamRelationService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean userApplyExam(Long userId, Long examId) {
        Assert.notNull(examId, "考试id不能为空");
        Assert.notNull(userId, "用户id不能为空");

        //检查用户是否已报名（虽然数据库有复合唯一索引保底，但还是查一下提高性能）
        boolean isExist=userApplyExamRelationService.checkExamApplyExist(userId, examId);
        if (isExist)
            throw new RuntimeException("用户已报名该考试，不能重复报名。");

        //尝试扣减剩余人数
        int reduceResult = examUserNumReduceExecutor(examId);
        if (reduceResult==0){
            throw new RuntimeException("考试已满额，不能报名。");
        }
        if (reduceResult<0){
            throw new RuntimeException("考试不存在，不能报名。");
        }
        // 插入报名记录
        try {
            userApplyExamRelationService.insert(userId, examId);
        } catch (DuplicateKeyException e) {
            //其它类型异常直接交给global了
            throw new RuntimeException("用户已报名该考试，不能重复报名。");
        }
        return true;
    }

    @Override
    public boolean insert(ExamDto dto) {
        //转为Exam对象
        Exam exam=dto.toExamForInsert();
        return save(exam);
    }

    @Override
    public boolean currentIsByondExamExpireTime(Long examId, @Nullable LocalDateTime checkTime) { // 改名：是否已过期
        Assert.notNull(examId, "考试ID不能为空");
        LocalDateTime targetTime = (checkTime == null) ? LocalDateTime.now() : checkTime;

        String examExpireTimeKey = OnlineExamEnum.Exam_Expire_Time.buildKey(String.valueOf(examId));
        String examExpireTimeValue = stringRedisTemplate.opsForValue().get(examExpireTimeKey);

        // 走缓存
        if (examExpireTimeValue != null) {
            LocalDateTime examExpireTime = LocalDateTime.parse(examExpireTimeValue);
            // 如果 当前时间 > 过期时间，返回 true
            return targetTime.isAfter(examExpireTime);
        }

        // 查数据库
        Exam e = lambdaQuery()
                .eq(Exam::getId, examId)
                .select(Exam::getBeginTime, Exam::getDurationTime)
                .one();

        if (e == null) {
            throw new RuntimeException("未找到对应的考试信息");
        }

        // 计算过期时间
        LocalDateTime endTime = e.getBeginTime().plusMinutes(e.getDurationTime());

        // 写入缓存
        long secondsUntilExpire = java.time.Duration.between(LocalDateTime.now(), endTime).getSeconds();
        if (secondsUntilExpire > 0) {
            stringRedisTemplate.opsForValue().set(examExpireTimeKey, endTime.toString(), secondsUntilExpire, TimeUnit.SECONDS);
        }

        // 返回逻辑统一：当前时间 > 结束时间
        return targetTime.isAfter(endTime);
    }

    /**
     * description
     *      扣减库存核心逻辑
     *      SQL效果: UPDATE exam SET remaining_num = remaining_num - 1 WHERE id = ? AND remaining_num > 0
     * author zzq
     * date 2025/12/18 16:02
     * param
     * return 0: 考试已满，-1: 考试不存在，1: 扣减成功
     */
    private int examUserNumReduceExecutor(Long examId) {
        // 构建更新条件
        LambdaUpdateWrapper<Exam> updateWrapper = Wrappers.lambdaUpdate(Exam.class)
                .eq(Exam::getId, examId)              // WHERE id = examId
                .gt(Exam::getRestUserNum, 0)         // AND remaining_num > 0
                .setSql("rest_user_num = rest_user_num - 1"); // SET remaining_num = remaining_num - 1
        boolean updateResult = this.update(updateWrapper);

        if (updateResult) {
            // 更新成功
            return 1;
        }

        //查下考试是否存在
        boolean examExist = lambdaQuery().eq(Exam::getId,examId).exists();
        if (!examExist)
            //第一次检查考试不存在，且扣减库存也失败，则认为考试不存在
            return -1;

        //考试存在，考试人员已满
        return 0;
    }


    @Override
    public Long getEnterExamMaxCount(Long examId) {
        Assert.notNull(examId, "考试id不能为空");

        Exam e= lambdaQuery()
                .eq(Exam::getId, examId)
                .select(Exam::getSecuritySetting)
                .one();

        if (e == null)
            throw new RuntimeException("exam对象为空");

        ExamSecuritySetting setting=e.getSecuritySetting();
        return Long.valueOf(setting.getMaxReconnectCount());
    }
}
