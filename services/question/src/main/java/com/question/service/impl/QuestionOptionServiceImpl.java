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

@Service
public class QuestionOptionServiceImpl extends ServiceImpl<QuestionMapper, Question>
        implements QuestionOptionService {


    @Override
    public Long insert(QuestionDto questionDto) {
        Question question = questionDto.buildForInsert();
        try {
            //成功返回生成的id，失败返回null
            return saveOrUpdate(question)?question.getId():null;
        }catch (DuplicateKeyException e){
            throw new RuntimeException("问题已存在");
        }
    }

    @Override
    public List<QuestionDto> getListByIds(List<Long> idList) {
        if(idList==null||idList.isEmpty())
            throw new RuntimeException("idList为空");
        List<Question> questionList = listByIds(idList);
        List<QuestionDto>questionDtoList=QuestionDto.toDtoList(questionList);
        return questionDtoList;
    }
}
