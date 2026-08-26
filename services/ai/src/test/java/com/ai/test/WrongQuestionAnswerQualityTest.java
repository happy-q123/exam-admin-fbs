package com.ai.test;

import com.ai.utils.WrongQuestionAnswerQuality;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WrongQuestionAnswerQualityTest {

    @Test
    void removesThinkingAndInternalPreambleBeforeDisplayingAnswer() {
        String raw = "<think>这里是模型内部推理，不应展示。</think>\n"
                + "好的，我现在需要处理用户的问题。\n"
                + "1. 正确结论：final 用于保证变量只能赋值一次。\n"
                + "2. 解题步骤：先判断题目要求，再对照关键字语义。\n"
                + "3. 错误原因：把类型推断关键字和不可变修饰符混淆。\n"
                + "4. 知识点：final 与 var 的适用范围不同。\n"
                + "5. 相似练习题：Java 中哪个关键字用于继承？答案是 extends。";

        String sanitized = WrongQuestionAnswerQuality.sanitize(raw);

        assertTrue(sanitized.startsWith("1. 正确结论"));
        assertFalse(sanitized.contains("<think>"));
        assertFalse(sanitized.contains("我现在需要"));
        assertTrue(WrongQuestionAnswerQuality.isUsable(sanitized));
    }

    @Test
    void rejectsInternalHistoryAndIncompleteOutput() {
        String leaked = "1. 正确结论：final 是修饰符。\n"
                + "2. 解题步骤：分析题目。\n"
                + "3. 错误原因：概念混淆。\n"
                + "4. 知识点：Java 关键字。\n"
                + "5. 相似练习题：从LONG_TERM_MEMORY中复制历史内容";
        String truncated = "1. 正确结论：final 是修饰符。\n"
                + "2. 解题步骤：分析题目。\n"
                + "3. 错误原因：概念混淆。\n"
                + "4. 知识点：Java 关键字。\n"
                + "5. 相似练习题：";

        assertFalse(WrongQuestionAnswerQuality.isUsable(leaked));
        assertFalse(WrongQuestionAnswerQuality.isUsable(truncated));
    }

    @Test
    void removesInternalAppendixAndRejectsMidSentenceTruncation() {
        String answer = "1. 正确结论：final 用于保证变量只能赋值一次。\n"
                + "2. 解题步骤：先判断题目要求，再对照关键字语义。\n"
                + "3. 错误原因：把类型推断关键字和不可变修饰符混淆。\n"
                + "4. 知识点：final 与 var 的适用范围不同。\n"
                + "5. 相似练习题：Java 中哪个关键字用于继承？答案是 extends。\n"
                + "从检索上下文：这段内容不能展示给考生。";

        assertTrue(WrongQuestionAnswerQuality.isUsable(WrongQuestionAnswerQuality.sanitize(answer)));
        assertFalse(WrongQuestionAnswerQuality.isUsable(answer.substring(0, answer.indexOf("extends") + 3)));
    }

    @Test
    void acceptsCompleteMultiLineExerciseEndingWithAnOption() {
        String answer = "1. 正确结论：final 是修饰符。\n"
                + "2. 解题步骤：需要显式指定具体类型。\n"
                + "3. 错误原因：混淆了 final 和 var。\n"
                + "4. 知识点：final 不负责类型推断。\n"
                + "5. 相似练习题：以下哪个关键字用于类型推断？\n"
                + "A. static\nB. final\nC. var\nD. public";

        assertTrue(WrongQuestionAnswerQuality.isUsable(answer));
    }

    @Test
    void acceptsMarkdownBoldSectionLabels() {
        String answer = "1. **正确结论**：final 是修饰符。\n"
                + "2. **解题步骤**：需要显式指定具体类型。\n"
                + "3. **错误原因**：混淆了 final 和 var。\n"
                + "4. **知识点**：final 不负责类型推断。\n"
                + "5. **一道相似练习题**：以下哪个关键字用于类型推断？A. static B. final C. var D. public";

        assertTrue(WrongQuestionAnswerQuality.isUsable(answer));
    }

    @Test
    void stripsThinkingFromQualityVerdictWithoutChangingVerdictText() {
        assertEquals("PASS\n原因：内容完整", WrongQuestionAnswerQuality.stripThinking(
                "<think>不要把这个过程当成判定。</think>\nPASS\n原因：内容完整"));
    }
}
