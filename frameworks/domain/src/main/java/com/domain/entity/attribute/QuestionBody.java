package com.domain.entity.attribute;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 对应数据库 body 字段的具体结构
 */
@NoArgsConstructor // 确保 Jackson 反序列化不报错
@AllArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 忽略未知的属性，如之前遇到的 score 字段
public class QuestionBody {

    /** 题干 */
    private String stem;

    /** 题干配图 (可选) */
    private String stemImg;

    /** 选项列表 */
    private List<Option> options;

    /** 正确答案 */
    //单选题答案是"字母"，多选题答案是"["A","B"]",简单题答案是字符串，判断题答案是"T"或"F"
    private Object correct;

    /**
     * 题目解析
     */
    private String analysis;

    /**
     * 获取答案，并尝试转为为字符串格式（适用于单选题、判断题、简答题）
     */
    @JsonIgnore
    public String getCorrectAsString() {
        return correct instanceof String ? (String) correct : null;
    }

    /**
     * 获取答案,并尝试转为列表格式（适用于多选题）
     */
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public List<String> getCorrectAsList() {
        if (correct instanceof List) {
            return (List<String>) correct;
        }
        return Collections.emptyList();
    }

    @NoArgsConstructor // 确保 Jackson 反序列化不报错
    @AllArgsConstructor
    @Data
    @Builder
    public static class Option {
        /** 选项标识，如 A, B, C */
        private String key;
        /** 选项内容 */
        private String val;
    }
}