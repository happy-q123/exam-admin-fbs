package com.domain.vo;

import com.domain.dto.QuestionDto;
import com.domain.dto.UserOnlineExamAnswerDto;
import com.domain.entity.attribute.QuestionBody;
import com.domain.entity.attribute.UserOnlineExamQuestionAnswerBody;
import com.domain.enums.QuestionDifficultyEnum;
import com.domain.enums.QuestionTypeEnum;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * description 用户在线考试的错题vo
 * author zzq
 * date 2026/01/28 19:06
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserErrorQuestionsVo {

    /*
    * userOnlineExamAnswer的ID
    * */
    private Long userOnlineExamAnswerId;

    /**
     * 考试id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long examId;

    /**
     * 问题id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;

//    /**
//     * 问题的分数
//     * TODO 这个分数在关系表
//     */
//    private BigDecimal score;

    /*
    *  实际得分
    * */
    private BigDecimal practicalScore;

    /**
     * 用户答案
     */
    private UserOnlineExamQuestionAnswerBody userAnswer;

//    /**
//     * 问题的序号
//     * 也在关系表中
//     */
//    private Integer seq;

    /**
     * 类型。“单选题”、“多选题”、“简答题”
     */
    private QuestionTypeEnum type;

    /**
     * 难度：0-简单 1-中等 2-困难
     */
    private QuestionDifficultyEnum difficulty;

    /**
     是否启用
     */
    private Boolean status;

    /**
     * 题干、选项、媒体资源等 JSON 内容
     */
    private QuestionBody body;

    /**
     * 问题标签（对应 PG 的 varchar[]）
     */
    private List<String> tags;


    public static UserErrorQuestionsVo toVo(UserOnlineExamAnswerDto userOnlineExamAnswerDto, QuestionDto questionDto){
        if (userOnlineExamAnswerDto == null || questionDto == null)
            throw new RuntimeException("参数为空");
        return UserErrorQuestionsVo.builder()
                .userOnlineExamAnswerId(userOnlineExamAnswerDto.getId())
                .examId(userOnlineExamAnswerDto.getExamId())
                .userAnswer(userOnlineExamAnswerDto.getAnswer())
                .practicalScore(BigDecimal.valueOf(userOnlineExamAnswerDto.getScore()))

                .questionId(questionDto.getId())
                .type(questionDto.getType())
                .difficulty(questionDto.getDifficulty())
                .status(questionDto.getStatus())
                .body(questionDto.getBody())
                .tags(questionDto.getTags())
                .build();
    }

    public static List<UserErrorQuestionsVo> toVo(List<UserOnlineExamAnswerDto> userOnlineExamAnswerDtoList,
                                                  List<QuestionDto> questionDtoList) {
        // 1. 空值检查，如果任意一个列表为空，直接返回空列表
        if (userOnlineExamAnswerDtoList == null || userOnlineExamAnswerDtoList.isEmpty()
                || questionDtoList == null || questionDtoList.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 将 QuestionDto 列表转换为 Map<Long, QuestionDto>
        // Key: QuestionDto.getId(), Value: QuestionDto 对象
        // 使用 Map 可以将查找的时间复杂度从 O(N) 降低到 O(1)
        Map<Long, QuestionDto> questionMap = questionDtoList.stream()
                .collect(Collectors.toMap(
                        QuestionDto::getId,       // Key mapper
                        Function.identity(),      // Value mapper
                        (existing, replacement) -> existing // Merge function: 如果有重复ID，保留第一个
                ));

        // 3. 遍历 Answer 列表，通过 ID 从 Map 中获取对应的 Question 并进行组装
        return userOnlineExamAnswerDtoList.stream()
                .map(answer -> {
                    // 根据 answer 中的 questionId 找到对应的 question
                    QuestionDto question = questionMap.get(answer.getQuestionId());

                    // 如果找不到对应的题目（数据不一致的情况），则跳过，防止调用单个 toVo 时报空指针异常
                    if (question == null) {
                        return null;
                    }

                    // 复用已有的单个对象转换方法
                    return toVo(answer, question);
                })
                .filter(Objects::nonNull) // 过滤掉未匹配到题目的空对象
                .collect(Collectors.toList());
    }
}
