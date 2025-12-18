package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.dto.UserApplyExamRelationDto;
import com.domain.entity.relation.UserApplyExamRelation;
import com.exam.mapper.UserApplyExamRelationMapper;
import com.exam.service.UserApplyExamRelationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
@Service
public class UserApplyExamRelationServiceImpl extends ServiceImpl<UserApplyExamRelationMapper, UserApplyExamRelation>
        implements UserApplyExamRelationService {
    @Override
    public Page<UserApplyExamRelation> getList(UserApplyExamRelationDto userApplyExamRelationDto) {
        Page<UserApplyExamRelation> pageResult=lambdaQuery(userApplyExamRelationDto.build())
                .page(userApplyExamRelationDto.buildPage());
        return pageResult;
    }

    @Override
    public boolean checkExist(Long userId, Long examId) {
        return lambdaQuery()
                .eq(UserApplyExamRelation::getUserId, userId)
                .eq(UserApplyExamRelation::getExamId, examId)
                .exists();
    }

    @Override
    public void insert(Long userId, Long examId) {
        //构建插入对象，插入
        UserApplyExamRelation examUserRelation= UserApplyExamRelation.builder()
                .examId(examId)
                .userId(userId)
                .applyTime(LocalDateTime.now())
                .build();
        save(examUserRelation);
    }
}
