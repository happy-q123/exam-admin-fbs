package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.dto.UserOnlineExamAnswerDto;
import com.domain.entity.relation.UserOnlineExamAnswer;
import com.domain.enums.redis.UserOnlineKeyEnum;
import com.exam.mapper.UserOnlineExamAnswerMapper;
import com.exam.service.UserOnlineExamAnswerService;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserOnlineExamAnswerServiceImpl extends ServiceImpl<UserOnlineExamAnswerMapper, UserOnlineExamAnswer>
        implements UserOnlineExamAnswerService{
    @Resource
    StringRedisTemplate stringRedisTemplate;

    private final String USER_ONLINE_KEY = UserOnlineKeyEnum.ONLINE_USERS.buildKey();

    //判断用户是否在线
    boolean isUserOnline(String userId){
        return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(USER_ONLINE_KEY, userId));
    }

    @Override
    public void saveAnswer(UserOnlineExamAnswerDto userOnlineExamAnswerDto) {
        UserOnlineExamAnswer userOnlineExamAnswer = userOnlineExamAnswerDto.toEntityForSave();
        String userId = String.valueOf(userOnlineExamAnswer.getUserId());

        if(!isUserOnline(userId)){
            throw new RuntimeException("用户不在线");
        }

        try {
            this.save(userOnlineExamAnswer);
        }catch (DuplicateKeyException e){
            updateAnswer(userOnlineExamAnswer);
        }
    }

    @Override
    public void updateAnswer(UserOnlineExamAnswerDto userOnlineExamAnswerDto) {
        UserOnlineExamAnswer userOnlineExamAnswer = userOnlineExamAnswerDto.toEntityForSave();
        String userId = String.valueOf(userOnlineExamAnswer.getUserId());
        if(!isUserOnline(userId)){
            throw new RuntimeException("用户不在线");
        }
        updateAnswer(userOnlineExamAnswer);
    }

    private void updateAnswer(UserOnlineExamAnswer userOnlineExamAnswer) {
        UserOnlineExamAnswer updatePayload = new UserOnlineExamAnswer();
        updatePayload.setAnswer(userOnlineExamAnswer.getAnswer());         // 只有这俩会被更新
        updatePayload.setOptionTime(userOnlineExamAnswer.getOptionTime());
        lambdaUpdate()
                .eq(UserOnlineExamAnswer::getUserId, userOnlineExamAnswer.getUserId())
                .eq(UserOnlineExamAnswer::getExamId, userOnlineExamAnswer.getExamId())
                .eq(UserOnlineExamAnswer::getQuestionId, userOnlineExamAnswer.getQuestionId())
                .update(updatePayload);

//        //版本2：（有坑版本，不可用）
//        //根据用户id、考试id、题目id更新用户在线考试答案和操作时间
//        lambdaUpdate()
//                .eq(UserOnlineExamAnswer::getUserId, userOnlineExamAnswer.getUserId())
//                .eq(UserOnlineExamAnswer::getExamId, userOnlineExamAnswer.getExamId())
//                .eq(UserOnlineExamAnswer::getQuestionId, userOnlineExamAnswer.getQuestionId())
//                .set(UserOnlineExamAnswer::getAnswer, 这里需要将userOnlineExamAnswer.getAnswer()转换成json格式,切记)
//                .set(UserOnlineExamAnswer::getOptionTime, userOnlineExamAnswer.getOptionTime())
//                .update();


        // 版本3：（可用）
//        UserOnlineExamAnswer updateEntity = new UserOnlineExamAnswer();
//        updateEntity.setAnswer(userOnlineExamAnswer.getAnswer());
//        updateEntity.setOptionTime(userOnlineExamAnswer.getOptionTime());
//
//        // 创建匹配条件的 Wrapper (只负责 WHERE 条件)
//        LambdaUpdateWrapper<UserOnlineExamAnswer> updateWrapper = new LambdaUpdateWrapper<>();
//        updateWrapper.eq(UserOnlineExamAnswer::getUserId, userOnlineExamAnswer.getUserId())
//                .eq(UserOnlineExamAnswer::getExamId, userOnlineExamAnswer.getExamId())
//                .eq(UserOnlineExamAnswer::getQuestionId, userOnlineExamAnswer.getQuestionId());
//
//        // 调用 Mapper 的 update 方法
//        // MP 会解析 updateEntity 中的字段，此时它会读取 @TableField 注解并正确使用 TypeHandler
//        update(updateEntity, updateWrapper);
    }


}
