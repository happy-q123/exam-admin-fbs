package com.question.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domain.dto.QuestionDto;
import com.domain.entity.Question;
import com.domain.entity.User;
import com.question.mapper.QuestionMapper;
import com.question.service.QuestionOptionService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuestionOptionServiceImpl extends ServiceImpl<QuestionMapper, Question>
        implements QuestionOptionService {


    @Override
    public Long insert(QuestionDto questionDto) {
        if (questionDto == null) {
            throw new IllegalArgumentException("题目内容不能为空");
        }
        Question question = questionDto.buildForInsert();
        try {
            //成功返回生成的id，失败返回null
            return saveOrUpdate(question)?question.getId():null;
        }catch (DuplicateKeyException e){
            throw new IllegalArgumentException("问题已存在", e);
        }
    }

    @Override
    public List<QuestionDto> getListByIds(List<Long> idList) {
        if(idList==null||idList.isEmpty())
            throw new IllegalArgumentException("idList不能为空");
        List<Question> questionList = listByIds(idList);
        List<QuestionDto>questionDtoList=QuestionDto.toDtoList(questionList);
        Map<Long, QuestionDto> questionMap = questionDtoList.stream()
                .collect(Collectors.toMap(QuestionDto::getId, Function.identity(), (first, second) -> first));
        return idList.stream()
                .map(questionMap::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}
