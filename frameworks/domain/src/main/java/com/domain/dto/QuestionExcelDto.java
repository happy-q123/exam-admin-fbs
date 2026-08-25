package com.domain.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class QuestionExcelDto {
    @ExcelProperty("题型")
    private String type;

    @ExcelProperty("难度")
    private String difficulty;

    @ExcelProperty("题干内容")
    private String content;

    @ExcelProperty("选项A")
    private String optionA;

    @ExcelProperty("选项B")
    private String optionB;

    @ExcelProperty("选项C")
    private String optionC;

    @ExcelProperty("选项D")
    private String optionD;

    @ExcelProperty("正确答案")
    private String correctAnswer;

    @ExcelProperty("分值")
    private Integer score;
}
