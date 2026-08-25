package com.exam.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.entity.UserAnswer;
import com.exam.mapper.UserAnswerMapper;
import com.exam.service.UserAnswerService;
import org.springframework.stereotype.Service;

/**
 * 用户答题明细服务实现类
 *
 * @author zzq
 * @date 2026-06-11
 */
@Service
public class UserAnswerServiceImpl extends ServiceImpl<UserAnswerMapper, UserAnswer> implements UserAnswerService {
}
