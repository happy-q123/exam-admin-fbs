package com.exam.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.domain.dto.UserApplyExamRelationDto;
import com.domain.entity.relation.UserApplyExamRelation;


public interface UserApplyExamRelationService extends IService<UserApplyExamRelation> {
    /**
     * description 返回用户报名的考试列表
     * userApplyExamRelationDto字段若不为null且不为""，则作为查询条件
     * //todo 分页
     * author zzq
     * date 2025/12/18 15:47
     * param
     * return
     */
    Page<UserApplyExamRelation> getList(UserApplyExamRelationDto userApplyExamRelationDto);

    /**
     * description  检查用户是否报名了该考试
     * author zzq
     * date 2025/12/18 16:55
     * param
     * return true存在，false不存在
     */
    boolean checkExamApplyExist(Long userId, Long examId);

    /**
     * description 根据userId和examId插入报名关系
     * author zzq
     * date 2025/12/18 17:00
     * param
     */
     void insert(Long userId, Long examId);

}
