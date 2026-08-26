package com.question.controller;

import com.domain.dto.QuestionDto;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.domain.dto.QuestionExcelDto;
import com.domain.entity.attribute.QuestionBody;
import com.domain.enums.QuestionDifficultyEnum;
import com.domain.enums.QuestionTypeEnum;
import com.domain.restful.RestResponse;
import com.question.service.QuestionOptionService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * description 问题操作controller
 * author zzq
 * date 2025/12/20 12:12
 */
@RestController
public class QuestionOptionController {
    private final QuestionOptionService questionOptionService;

    public QuestionOptionController(QuestionOptionService questionOptionService) {
        this.questionOptionService = questionOptionService;
    }

    /**
     * description 添加一个问题
     * author zzq
     * date 2025/12/20 14:18
     * param
     * return 成功返回插入记录的时生成的id
     */
    @PostMapping("/insertOne")
    @PreAuthorize("@roleGuard.isTeacherOrAdmin(authentication)")
    public RestResponse<Map<String,String>> insertOne(@AuthenticationPrincipal Jwt jwt, @RequestBody QuestionDto dto) {
        Long userId = currentUserId(jwt);
        if(userId==null)
            return RestResponse.fail("token中无userId");
        if (dto == null) {
            throw new IllegalArgumentException("题目内容不能为空");
        }
        dto.setCreatorId(userId);
        Long result=questionOptionService.insert(dto);
        String message=result==null?"添加失败":"添加成功";
        return RestResponse.success(message, Map.of("id",String.valueOf(result)));
    }

    /**
     * description 根据问题id列表获取问题列表
     * author zzq
     * date 2025/12/20 17:10
     * param
     * return
     */
    @GetMapping("/getListByIds")
    public RestResponse<List<QuestionDto>> getListByIds(@RequestParam("ids") List<Long> ids) {
        List<QuestionDto> questions = questionOptionService.getListByIds(ids).stream()
                .map(this::sanitizeForClient)
                .toList();
        return RestResponse.success("查询成功", questions);
    }

    /**
     * 从 Excel 批量导入题目。
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@roleGuard.isTeacherOrAdmin(authentication)")
    public RestResponse<Integer> importQuestions(@AuthenticationPrincipal Jwt jwt,
                                                 @RequestPart("file") MultipartFile file) {
        Long userId = jwt == null ? null : currentUserId(jwt);
        if (userId == null) {
            return RestResponse.fail("token中无userId");
        }
        if (file == null || file.isEmpty()) {
            return RestResponse.fail("请选择要导入的 Excel 文件");
        }

        List<com.domain.entity.Question> questions = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), QuestionExcelDto.class, new ReadListener<QuestionExcelDto>() {
                @Override
                public void invoke(QuestionExcelDto data, AnalysisContext context) {
                    int rowNumber = context.readRowHolder().getRowIndex() + 1;
                    try {
                        questions.add(toQuestion(data, userId));
                    } catch (RuntimeException ex) {
                        throw new IllegalArgumentException("第 " + rowNumber + " 行数据不合法：" + ex.getMessage(), ex);
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    // 数据在读取完成后统一保存，避免部分成功、部分失败。
                }
            }).sheet().doRead();

            if (questions.isEmpty()) {
                return RestResponse.fail("Excel 中没有可导入的数据");
            }
            questionOptionService.saveBatch(questions);
            return RestResponse.success("导入成功", questions.size());
        } catch (Exception ex) {
            return RestResponse.fail("导入失败：" + ex.getMessage());
        }
    }

    @GetMapping("/page")
    @PreAuthorize("@roleGuard.isTeacherOrAdmin(authentication)")
    public RestResponse<com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.domain.entity.Question>> page(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize) {
        long safePageNum = Math.max(pageNum, 1);
        long safePageSize = Math.min(Math.max(pageSize, 1), 100);
        return RestResponse.success(questionOptionService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(safePageNum, safePageSize)));
    }

    private static com.domain.entity.Question toQuestion(QuestionExcelDto data, Long creatorId) {
        if (data == null) {
            throw new IllegalArgumentException("数据为空");
        }
        if (data.getContent() == null || data.getContent().isBlank()) {
            throw new IllegalArgumentException("题干不能为空");
        }

        QuestionTypeEnum type = parseType(data.getType());
        QuestionDifficultyEnum difficulty = parseDifficulty(data.getDifficulty());
        QuestionBody body = new QuestionBody();
        body.setStem(data.getContent().trim());
        body.setCorrect(parseCorrect(data.getCorrectAnswer(), type));
        body.setOptions(buildOptions(data));

        if (body.getCorrect() == null) {
            throw new IllegalArgumentException("正确答案不能为空");
        }
        if (type != QuestionTypeEnum.BriefResponse
                && type != QuestionTypeEnum.Judge
                && (body.getOptions() == null || body.getOptions().isEmpty())) {
            throw new IllegalArgumentException("选择题至少需要一个选项");
        }

        com.domain.entity.Question question = new com.domain.entity.Question();
        question.setType(type);
        question.setDifficulty(difficulty);
        question.setCreatorId(creatorId);
        question.setStatus(true);
        question.setCreateTime(LocalDateTime.now());
        question.setLatestUpdateTime(LocalDateTime.now());
        question.setLatestUpdateId(creatorId);
        question.setBody(body);
        question.setTags(List.of());
        return question;
    }

    private static List<QuestionBody.Option> buildOptions(QuestionExcelDto data) {
        List<QuestionBody.Option> options = new ArrayList<>();
        addOption(options, "A", data.getOptionA());
        addOption(options, "B", data.getOptionB());
        addOption(options, "C", data.getOptionC());
        addOption(options, "D", data.getOptionD());
        return options;
    }

    private static void addOption(List<QuestionBody.Option> options, String key, String value) {
        if (value != null && !value.isBlank()) {
            options.add(QuestionBody.Option.builder().key(key).val(value.trim()).build());
        }
    }

    private static Object parseCorrect(String answer, QuestionTypeEnum type) {
        if (answer == null || answer.isBlank()) {
            return null;
        }
        String normalized = answer.trim().toUpperCase();
        if (type == QuestionTypeEnum.MultiOption) {
            return java.util.Arrays.stream(normalized.split("[,，、\\s]+"))
                    .filter(item -> !item.isBlank())
                    .toList();
        }
        if (type == QuestionTypeEnum.Judge) {
            return switch (normalized) {
                case "正确", "对", "TRUE", "YES", "是" -> "T";
                case "错误", "错", "FALSE", "NO", "否" -> "F";
                default -> normalized;
            };
        }
        return normalized;
    }

    private static QuestionTypeEnum parseType(String value) {
        if (value == null) {
            throw new IllegalArgumentException("题型不能为空");
        }
        return switch (value.trim().toLowerCase()) {
            case "0", "singleoption", "single", "单选", "单选题" -> QuestionTypeEnum.SingleOption;
            case "1", "multioption", "multiple", "多选", "多选题" -> QuestionTypeEnum.MultiOption;
            case "2", "judge", "判断", "判断题" -> QuestionTypeEnum.Judge;
            case "3", "briefresponse", "essay", "简答", "简答题" -> QuestionTypeEnum.BriefResponse;
            default -> throw new IllegalArgumentException("无法识别题型：" + value);
        };
    }

    private QuestionDto sanitizeForClient(QuestionDto source) {
        if (source == null || source.getBody() == null) {
            return source;
        }
        QuestionBody body = new QuestionBody();
        body.setStem(source.getBody().getStem());
        body.setStemImg(source.getBody().getStemImg());
        body.setOptions(source.getBody().getOptions());
        return QuestionDto.builder()
                .id(source.getId())
                .type(source.getType())
                .difficulty(source.getDifficulty())
                .creatorId(source.getCreatorId())
                .status(source.getStatus())
                .createTime(source.getCreateTime())
                .body(body)
                .tags(source.getTags())
                .latestUpdateTime(source.getLatestUpdateTime())
                .latestUpdateId(source.getLatestUpdateId())
                .build();
    }

    private static QuestionDifficultyEnum parseDifficulty(String value) {
        if (value == null || value.isBlank()) {
            return QuestionDifficultyEnum.Common;
        }
        return switch (value.trim().toLowerCase()) {
            case "0", "easy", "简单" -> QuestionDifficultyEnum.Easy;
            case "1", "common", "medium", "一般", "中等" -> QuestionDifficultyEnum.Common;
            case "2", "difficult", "hard", "困难" -> QuestionDifficultyEnum.Difficult;
            default -> throw new IllegalArgumentException("无法识别难度：" + value);
        };
    }

    private Long currentUserId(Jwt jwt) {
        if (jwt == null || jwt.getClaim("userId") == null) return null;
        Object value = jwt.getClaim("userId");
        return value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
    }
}
