package com.domain.entity.attribute;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor // 确保 Jackson 反序列化不报错
@AllArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 忽略未知的属性，如之前遇到的 score 字段
public class UserOnlineExamQuestionAnswerBody {

    /** 正确答案 */
    //单选题答案是"字母"，多选题答案是"["A","B"]",简单题答案是字符串，判断题答案是"T"或"F"
    private Object answer;
}
