package com.exam.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.dto.QuestionDto;
import com.domain.dto.UserOnlineExamAnswerDto;
import com.domain.entity.Exam;
import com.domain.entity.relation.UserOnlineExamAnswer;
import com.domain.enums.redis.UserOnlineKeyEnum;
import com.domain.restful.RestResponse;
import com.domain.vo.UserErrorQuestionsVo;
import com.exam.feign.ExamQuestionRelationFeignClient;
import com.exam.mapper.UserOnlineExamAnswerMapper;
import com.exam.service.ExamQuestionRelationService;
import com.exam.service.ExamService;
import com.exam.service.OnlineExamService;
import com.exam.service.UserApplyExamRelationService;
import com.exam.service.UserOnlineExamAnswerService;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserOnlineExamAnswerServiceImpl extends ServiceImpl<UserOnlineExamAnswerMapper, UserOnlineExamAnswer>
        implements UserOnlineExamAnswerService{
    @Resource
    StringRedisTemplate stringRedisTemplate;

    //todo 远程调用问题
    private final ExamQuestionRelationFeignClient examQuestionRelationFeignClient;
    private final ExamQuestionRelationService examQuestionRelationService;
    private final ExamService examService;
    private final UserApplyExamRelationService userApplyExamRelationService;
    private final OnlineExamService onlineExamService;
    private final String USER_ONLINE_KEY = UserOnlineKeyEnum.ONLINE_USERS.buildKey();

    public UserOnlineExamAnswerServiceImpl(ExamQuestionRelationFeignClient examQuestionRelationFeignClient,
                                           ExamQuestionRelationService examQuestionRelationService,
                                           ExamService examService,
                                           UserApplyExamRelationService userApplyExamRelationService,
                                           OnlineExamService onlineExamService) {
        this.examQuestionRelationFeignClient = examQuestionRelationFeignClient;
        this.examQuestionRelationService = examQuestionRelationService;
        this.examService = examService;
        this.userApplyExamRelationService = userApplyExamRelationService;
        this.onlineExamService = onlineExamService;
    }

    //判断用户是否在线
    boolean isUserOnline(String userId){
        return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(USER_ONLINE_KEY, userId));
    }

    @Override
    public void saveAnswer(UserOnlineExamAnswerDto userOnlineExamAnswerDto) {
        if (userOnlineExamAnswerDto == null || userOnlineExamAnswerDto.getUserId() == null
                || userOnlineExamAnswerDto.getExamId() == null || userOnlineExamAnswerDto.getQuestionId() == null) {
            throw new IllegalArgumentException("答案缺少用户、考试或题目编号");
        }
        assertActiveExamSession(userOnlineExamAnswerDto.getUserId(), userOnlineExamAnswerDto.getExamId());
        if (!examQuestionRelationService.lambdaQuery()
                .eq(com.domain.entity.relation.ExamQuestionRelation::getExamId, userOnlineExamAnswerDto.getExamId())
                .eq(com.domain.entity.relation.ExamQuestionRelation::getQuestionId, userOnlineExamAnswerDto.getQuestionId())
                .exists()) {
            throw new IllegalArgumentException("题目不属于当前考试");
        }
        UserOnlineExamAnswer userOnlineExamAnswer = userOnlineExamAnswerDto.toEntityForSave();
        String userId = String.valueOf(userOnlineExamAnswer.getUserId());

        if(!isUserOnline(userId)){
            throw new IllegalStateException("用户不在线");
        }

        try {
            this.save(userOnlineExamAnswer);
        }catch (DuplicateKeyException e){
            updateAnswer(userOnlineExamAnswer);
        }
    }

    private void assertActiveExamSession(Long userId, Long examId) {
        Exam exam = examService.getById(examId);
        if (exam == null) {
            throw new IllegalArgumentException("考试不存在");
        }
        if (Boolean.FALSE.equals(exam.getStatus())) {
            throw new IllegalArgumentException("考试已停用");
        }
        if (!userApplyExamRelationService.checkExamApplyExist(userId, examId)) {
            throw new IllegalArgumentException("未报名该考试");
        }
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (exam.getBeginTime() == null || exam.getDurationTime() == null
                || now.isBefore(exam.getBeginTime())
                || now.isAfter(exam.getBeginTime().plusMinutes(exam.getDurationTime()).plusSeconds(30))) {
            throw new IllegalArgumentException("当前不在考试答题时间内");
        }
        if (!onlineExamService.isExamSessionActive(userId, examId)) {
            throw new IllegalStateException("考试会话已失效，请重新进入考试");
        }
        if (!isUserOnline(String.valueOf(userId))) {
            throw new IllegalStateException("用户不在线");
        }
    }

    @Override
    public void updateAnswer(UserOnlineExamAnswerDto userOnlineExamAnswerDto) {
        if (userOnlineExamAnswerDto == null || userOnlineExamAnswerDto.getUserId() == null
                || userOnlineExamAnswerDto.getExamId() == null || userOnlineExamAnswerDto.getQuestionId() == null) {
            throw new IllegalArgumentException("答案缺少用户、考试或题目编号");
        }
        assertActiveExamSession(userOnlineExamAnswerDto.getUserId(), userOnlineExamAnswerDto.getExamId());
        if (!examQuestionRelationService.lambdaQuery()
                .eq(com.domain.entity.relation.ExamQuestionRelation::getExamId, userOnlineExamAnswerDto.getExamId())
                .eq(com.domain.entity.relation.ExamQuestionRelation::getQuestionId, userOnlineExamAnswerDto.getQuestionId())
                .exists()) {
            throw new IllegalArgumentException("题目不属于当前考试");
        }
        UserOnlineExamAnswer userOnlineExamAnswer = userOnlineExamAnswerDto.toEntityForSave();
        String userId = String.valueOf(userOnlineExamAnswer.getUserId());
        if(!isUserOnline(userId)){
            throw new IllegalStateException("用户不在线");
        }
        updateAnswer(userOnlineExamAnswer);
    }

    @Override
    public List<UserErrorQuestionsVo> getUserAnswersByUserId(Long userId) {
        List<UserOnlineExamAnswer> l= lambdaQuery()
                .eq(UserOnlineExamAnswer::getUserId,userId)
                .list();
        if (l.isEmpty()) return List.of();

        List<UserOnlineExamAnswerDto> userOnlineExamAnswerDtoList = UserOnlineExamAnswerDto.toDto(l) ;
        List<Long> questionIds=l.stream().map(UserOnlineExamAnswer::getQuestionId).toList();
        RestResponse<List<QuestionDto>> questionDtos=examQuestionRelationFeignClient.getListByIds(questionIds);
        if (questionDtos == null || !Integer.valueOf(200).equals(questionDtos.getCode()) || questionDtos.getData() == null) {
            return List.of();
        }
        List<QuestionDto> questionDtoList=questionDtos.getData();

        List<UserErrorQuestionsVo> resultList=UserErrorQuestionsVo.toVo(userOnlineExamAnswerDtoList,questionDtoList);

        return resultList;
    }

    @Override
    public List<UserOnlineExamAnswerDto> getAnswersByExam(Long userId, Long examId) {
        if (userId == null || examId == null) {
            throw new IllegalArgumentException("用户ID和考试ID不能为空");
        }
        return UserOnlineExamAnswerDto.toDto(lambdaQuery()
                .eq(UserOnlineExamAnswer::getUserId, userId)
                .eq(UserOnlineExamAnswer::getExamId, examId)
                .orderByAsc(UserOnlineExamAnswer::getQuestionId)
                .list());
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
