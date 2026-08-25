package com.exam.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.entity.Question;
import com.exam.mapper.QuestionMapper;
import com.exam.service.QuestionService;
import org.springframework.stereotype.Service;

/**
 * 题库服务实现类
 * @author zzq
 * @date 2026-06-11
 */
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {
}
