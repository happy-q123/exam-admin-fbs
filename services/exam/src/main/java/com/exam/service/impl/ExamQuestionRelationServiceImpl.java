package com.exam.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.dto.ExamQuestionRelationDto;
import com.domain.dto.QuestionDto;
import com.domain.entity.relation.ExamQuestionRelation;
import com.domain.restful.RestResponse;
import com.domain.vo.ExamQuestionRelationVo;
import com.exam.feign.ExamQuestionRelationFeignClient;
import com.exam.mapper.ExamQuestionRelationMapper;
import com.exam.service.ExamQuestionRelationService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExamQuestionRelationServiceImpl extends ServiceImpl<ExamQuestionRelationMapper, ExamQuestionRelation>
        implements ExamQuestionRelationService {

    @Resource
    ExamQuestionRelationFeignClient examQuestionRelationFeignClient;

    @Override
    public Page<ExamQuestionRelationVo> getExamQuestionsByExamId(ExamQuestionRelationDto dto) {
        if (dto == null|| dto.getExamId() == null)
            throw new RuntimeException("参数不可为空");

        // 准备分页参数
        Page<ExamQuestionRelation> pageParam = dto.buildPage();

        // 用 .page() 只查当前页的数据（比如10条）
        Page<ExamQuestionRelation> dbPage = lambdaQuery()
                .eq(ExamQuestionRelation::getExamId, dto.getExamId())
                .page(pageParam);

        // 获取当前页的记录列表
        List<ExamQuestionRelation> records = dbPage.getRecords();

        // 如果没有查到关联关系，直接返回空页，避免后续 Feign 报错
        if (records == null || records.isEmpty()) {
            // 返回一个空的 Page 对象
            return new Page<>(dbPage.getCurrent(), dbPage.getSize());
        }

        //提取这 10 条数据的题目 ID
        List<Long> questionIdList = records.stream()
                .map(ExamQuestionRelation::getQuestionId)
                .collect(Collectors.toList());

        // 远程调用 Feign (只查这 10 个题目，性能高)
        RestResponse<List<QuestionDto>> feignResponse = examQuestionRelationFeignClient.getListByIds(questionIdList);
        List<QuestionDto> questionList = feignResponse != null ? feignResponse.getData() : new ArrayList<>();

        // 数据组装 (DTO -> VO)
        List<ExamQuestionRelationDto> relationDtoList = ExamQuestionRelationDto.toDtoList(records);
        List<ExamQuestionRelationVo> voList = ExamQuestionRelationVo.toVoList(relationDtoList, questionList);

        // 将 List 转为 Page 并返回
        Page<ExamQuestionRelationVo> resultPage = new Page<>();

        // 复制分页元数据 (当前页、总页数、总条数等)
        // 忽略 "records" 属性，待会放进去
        BeanUtils.copyProperties(dbPage, resultPage, "records");
        // 设置转换好的 VO 列表
        resultPage.setRecords(voList);
        return resultPage;
    }
}
