package com.domain.entity.attribute;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 对应数据库 body 字段的具体结构
 */
@NoArgsConstructor // 确保 Jackson 反序列化不报错
@AllArgsConstructor
@Data
public class QuestionBody {

    /** 题干 */
    private String stem;

    /** 题干配图 (可选) */
    private String stemImg;

    /** 选项列表 */
    private List<Option> options;

    /** 正确答案 */
    private String correct;

    /**
     * 题目解析
     */
    private String analysis;

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