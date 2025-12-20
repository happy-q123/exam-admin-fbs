package com.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.domain.dto.ExamQuestionRelationDto;
import com.domain.restful.RestResponse;
import com.domain.dto.ExamDto;
import com.domain.entity.Exam;
import com.domain.vo.ExamQuestionRelationVo;
import com.exam.service.ExamOptionService;
import com.exam.service.ExamQuestionRelationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
public class ExamOptionController {
    private final ExamOptionService examOptionService;
    private final ExamQuestionRelationService examQuestionRelationService;

    public ExamOptionController(ExamOptionService examOptionService, ExamQuestionRelationService examQuestionRelationService) {
        this.examOptionService = examOptionService;
        this.examQuestionRelationService = examQuestionRelationService;
    }

    @PostMapping("/addExam")
    public RestResponse<String> addExam(@AuthenticationPrincipal Jwt jwt, @RequestBody ExamDto dto){
        Long userId = jwt.getClaim("userId");
        if(userId==null)
            return RestResponse.fail("token中无userId");
        dto.setCreator(userId);

        String result=examOptionService.insert(dto)? "添加成功":"添加失败";
        return RestResponse.success(result);
    }

    /**
     * description 临时
     * author zzq
     * date 2025/12/19 16:17
     * param * @param null
     * return
     */
    @GetMapping("/getExamById/{examId}")
    public RestResponse<Exam> getExamList(@PathVariable("examId") String examId){
        Long id=Long.parseLong(examId);
        Exam e=examOptionService.getOne(new LambdaQueryWrapper<Exam>().eq(Exam::getId,id));
        return RestResponse.success(e);
    }

    /**
     * description 根据考试id获取考试题目（分页）
     * author zzq
     * date 2025/12/20 17:51
     * param
     * return
     */
    @PostMapping("/getExamQuestions")
    public RestResponse<Page<ExamQuestionRelationVo>> getExamQuestions(@RequestBody ExamQuestionRelationDto dto){
        Page<ExamQuestionRelationVo> page=examQuestionRelationService.getExamQuestionsByExamId(dto);
        return RestResponse.success(page);
    }
}
