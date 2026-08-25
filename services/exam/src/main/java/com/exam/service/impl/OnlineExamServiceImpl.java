package com.exam.service.impl;

import com.domain.dto.UserOnlineExamOptionsDto;
import com.domain.entity.relation.UserOnlineExamOptions;
import com.domain.enums.UserOnlineExamOptionTypeEnum;
import com.domain.enums.redis.OnlineExamEnum;
import com.domain.enums.redis.UserOnlineKeyEnum;
import com.exam.service.ExamService;
import com.exam.service.OnlineExamService;
import com.exam.service.UserApplyExamRelationService;
import com.exam.service.UserOnlineExamOptionsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OnlineExamServiceImpl implements OnlineExamService {
    private final ExamService examService;
    private final UserOnlineExamOptionsService userOnlineExamOptionsService;
    private final UserApplyExamRelationService userApplyExamRelationService;
    private final StringRedisTemplate stringRedisTemplate;

    //redis中在线用户列表key
    private final String USER_ONLINE_KEY = UserOnlineKeyEnum.ONLINE_USERS.buildKey();
    public OnlineExamServiceImpl(ExamService examService, UserApplyExamRelationService userApplyExamRelationService,
                                 UserOnlineExamOptionsService userOnlineExamOptionsService,
                                 StringRedisTemplate stringRedisTemplate) {
        this.examService = examService;
        this.userApplyExamRelationService = userApplyExamRelationService;
        this.userOnlineExamOptionsService = userOnlineExamOptionsService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    //判断用户是否在线
    boolean isUserOnline(String userId){
        return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(USER_ONLINE_KEY, userId));
    }
    @Override
    public void enterExam(Long userId, Long examId, LocalDateTime acquireTime) {

        if (!isUserOnline(String.valueOf(userId)))
            throw new RuntimeException("用户socket连接处于离线状态！");

        if (acquireTime == null){
            log.warn("acquireTime为null，将使用系统时间");
            acquireTime = LocalDateTime.now();
        }

        boolean isApply=userApplyExamRelationService.checkExamApplyExist(userId, examId);
        if (!isApply)
            throw new RuntimeException("未报名该考试");

        com.domain.entity.Exam exam = examService.getById(examId);
        if (exam == null) {
            throw new RuntimeException("考试不存在");
        }
        if (Boolean.FALSE.equals(exam.getStatus())) {
            throw new RuntimeException("考试已停用");
        }
        if (exam.getBeginTime() == null || exam.getDurationTime() == null || exam.getDurationTime() <= 0) {
            throw new RuntimeException("考试时间配置不完整");
        }
        if (acquireTime.isBefore(exam.getBeginTime())) {
            throw new RuntimeException("考试尚未开始");
        }

        boolean isExpire=examService.currentIsByondExamExpireTime(examId, acquireTime);
        if(isExpire)
            throw new RuntimeException("考试已过期");

        if(checkUserEnterExamCountIsMax(userId, examId))
            throw new RuntimeException("已超过最大进入次数");

        UserOnlineExamOptions userOnlineExamOptions = UserOnlineExamOptions.builder()
                .userId(userId)
                .examId(examId)
                .optionType(UserOnlineExamOptionTypeEnum.Enter)
                .optionTime(acquireTime)
                .build();

        //检查用户是否进入过某个正在进行的考试
        String examingKey= OnlineExamEnum.Is_Examing.buildKey(String.valueOf(userId));
        String value= stringRedisTemplate.opsForValue().get(examingKey);

        if(value==null){
            //如果没有，说明用户当前处于空闲状态，可以参加目标考试。
            //查询目标考试的结束时间。
            String examExpireTimeKey=OnlineExamEnum.Exam_Expire_Time.buildKey(String.valueOf(examId));
            String examExpireTimeValue=stringRedisTemplate.opsForValue().get(examExpireTimeKey);
            if (examExpireTimeValue==null)
                throw new RuntimeException("无法从缓存查询到考试持续时间");

            LocalDateTime examExpireTime=LocalDateTime.parse(examExpireTimeValue);
            // 计算考试持续时间（分钟）
            long remainingSeconds = java.time.Duration.between(acquireTime, examExpireTime).getSeconds();
            if (remainingSeconds <= 0) {
                throw new RuntimeException("考试已结束");
            }
            // 将用户标记为正在考试状态，并设置过期时间为剩余考试时间
            stringRedisTemplate.opsForValue().set(examingKey, examId.toString(), remainingSeconds, TimeUnit.SECONDS);
        }else{
            Long examingId=Long.parseLong(value);
            if(!examingId.equals(examId)){
                throw new RuntimeException("用户已经进入考试："+examingId+"，不可参加其它考试。");
            }
        }
        userOnlineExamOptionsService.save(userOnlineExamOptions);


    }

    @Override
    public boolean isExamSessionActive(Long userId, Long examId) {
        if (userId == null || examId == null) {
            return false;
        }
        String activeExamId = stringRedisTemplate.opsForValue()
                .get(OnlineExamEnum.Is_Examing.buildKey(String.valueOf(userId)));
        return examId.toString().equals(activeExamId);
    }

    @Override
    public boolean checkUserEnterExamCountIsMax(Long userId, Long examId) {
        Long maxCount=examService.getEnterExamMaxCount(examId);
        Long enterCount=userOnlineExamOptionsService.getUserEnterExamCount(userId, examId);
        if(maxCount==-1)
            return false;
        return enterCount >= maxCount;
    }

    @Override
    public void processUserDropOnline(UserOnlineExamOptionsDto dto) {
        if (dto == null || dto.getUserId() == null || dto.getExamId() == null) {
            return;
        }
        dto.setOptionType(UserOnlineExamOptionTypeEnum.Exit);
        dto.setOptionTime(LocalDateTime.now());
        UserOnlineExamOptions userOnlineExamOption = dto.toEntityForSave();
        userOnlineExamOptionsService.save(userOnlineExamOption);
        String examingKey = OnlineExamEnum.Is_Examing.buildKey(String.valueOf(dto.getUserId()));
        String currentExamId = stringRedisTemplate.opsForValue().get(examingKey);
        if (dto.getExamId().toString().equals(currentExamId)) {
            stringRedisTemplate.delete(examingKey);
        }
    }
}
