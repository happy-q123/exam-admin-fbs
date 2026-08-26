package com.ai.utils;

import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 错题助手最终答案的确定性清洗与质量门禁。
 *
 * <p>模型的思考内容、历史检索结果和工具描述都属于内部数据，不能直接展示给考生，
 * 也不能仅凭答案长度判定生成质量。该类不依赖 Spring 容器，便于单元测试和在 Advisor
 * 保存记忆前复用。</p>
 */
public final class WrongQuestionAnswerQuality {
    private static final String MARKDOWN_DECORATION = "[*_`~]*";
    private static final Pattern THINK_BLOCK = Pattern.compile("(?is)<think>.*?</think>");
    private static final Pattern OPEN_THINK = Pattern.compile("(?is)<think\\b[^>]*>");
    private static final Pattern THINK_END = Pattern.compile("(?is)</think>");
    private static final Pattern FIRST_SECTION = Pattern.compile(
            "(?m)^\\s*(?:#{1,6}\\s*)?(?:1\\s*[.、)：:]\\s*)?"
                    + MARKDOWN_DECORATION + "(?:正确结论|结论)" + MARKDOWN_DECORATION + "\\s*[:：]?");

    private static final Pattern CORRECT_SECTION = Pattern.compile(
            "(?im)(?:^|\\n)\\s*(?:#{1,6}\\s*)?(?:1\\s*[.、)：:]\\s*)?"
                    + MARKDOWN_DECORATION + "(?:正确结论|结论)" + MARKDOWN_DECORATION + "\\s*[:：]?");
    private static final Pattern STEPS_SECTION = Pattern.compile(
            "(?im)(?:^|\\n)\\s*(?:#{1,6}\\s*)?(?:2\\s*[.、)：:]\\s*)?"
                    + MARKDOWN_DECORATION + "(?:解题步骤|解题思路|分析步骤|步骤)" + MARKDOWN_DECORATION + "\\s*[:：]?");
    private static final Pattern ERROR_SECTION = Pattern.compile(
            "(?im)(?:^|\\n)\\s*(?:#{1,6}\\s*)?(?:3\\s*[.、)：:]\\s*)?"
                    + MARKDOWN_DECORATION + "(?:错误原因|失分原因|易错点)" + MARKDOWN_DECORATION + "\\s*[:：]?");
    private static final Pattern KNOWLEDGE_SECTION = Pattern.compile(
            "(?im)(?:^|\\n)\\s*(?:#{1,6}\\s*)?(?:4\\s*[.、)：:]\\s*)?"
                    + MARKDOWN_DECORATION + "(?:知识点|考点)" + MARKDOWN_DECORATION + "\\s*[:：]?");
    private static final Pattern EXERCISE_SECTION = Pattern.compile(
            "(?im)(?:^|\\n)\\s*(?:#{1,6}\\s*)?(?:5\\s*[.、)：:]\\s*)?"
                    + MARKDOWN_DECORATION + "(?:一道相似练习题|相似练习题|类似练习题|练习题)" + MARKDOWN_DECORATION + "\\s*[:：]?");
    private static final Pattern SECTION_ONLY = Pattern.compile(
            "(?im)^\\s*(?:#{1,6}\\s*)?(?:[1-5]\\s*[.、)：:]\\s*)?"
                    + MARKDOWN_DECORATION + "(?:正确结论|结论|解题步骤|解题思路|分析步骤|步骤|错误原因|失分原因|易错点|知识点|考点|一道相似练习题|相似练习题|类似练习题|练习题)"
                    + MARKDOWN_DECORATION + "\\s*[:：]?\\s*$");

    private WrongQuestionAnswerQuality() {
    }

    /**
     * 删除模型思考块和面向模型的开场白，保留面向考生的最终答案。
     */
    public static String sanitize(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }

        String text = raw.replace("\r\n", "\n").replace('\r', '\n').trim();
        text = THINK_BLOCK.matcher(text).replaceAll("");

        Matcher openThink = OPEN_THINK.matcher(text);
        if (openThink.find()) {
            // 未闭合的思考块通常意味着模型输出被截断，不能把内部推理展示出去。
            text = text.substring(0, openThink.start()).trim();
        }
        text = THINK_END.matcher(text).replaceAll("").trim();

        Matcher firstSection = FIRST_SECTION.matcher(text);
        if (firstSection.find()) {
            text = text.substring(firstSection.start()).trim();
        }

        text = truncateInternalAppendix(text);
        text = text.replaceAll("(?m)^\\s*```(?:markdown|text)?\\s*$", "").trim();
        return text;
    }

    /**
     * 去除思考块，但不截取编号段落，供质量评估结果解析使用。
     */
    public static String stripThinking(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String text = raw.replace("\r\n", "\n").replace('\r', '\n');
        text = THINK_BLOCK.matcher(text).replaceAll("");
        Matcher openThink = OPEN_THINK.matcher(text);
        if (openThink.find()) {
            text = text.substring(0, openThink.start());
        }
        return THINK_END.matcher(text).replaceAll("").trim();
    }

    /**
     * 严格校验答案是否可以交给考生。该规则故意偏保守，宁可触发 Agent 重试或兜底，
     * 也不把内部推理、提示词泄漏和半截答案标记为通过。
     */
    public static boolean isUsable(String answer) {
        if (!StringUtils.hasText(answer) || answer.length() < 120) {
            return false;
        }

        String normalized = answer.toLowerCase(Locale.ROOT);
        String[] forbiddenMarkers = {
                "<think", "</think>", "long_term_memory", "用户要求", "我现在需要",
                "工具列表", "系统提示", "调用工具", "assistant的回复", "提示词",
                "从检索上下文", "从上下文列表", "最终输出", "检查是否符合",
                "从用户提供的上下文列表", "输出必须严格", "严格输出五个部分",
                "所以，直接写", "确保不编造", "用户强调", "用户说",
                "我需要直接输出", "直接输出这个", "确保格式正确"
        };
        for (String marker : forbiddenMarkers) {
            if (normalized.contains(marker.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }

        // 结构校验不应受 Markdown 加粗、行内代码等展示格式影响。
        String structureText = answer.replaceAll("[*_`~]", "");
        if (!hasSection(CORRECT_SECTION, structureText)
                || !hasSection(STEPS_SECTION, structureText)
                || !hasSection(ERROR_SECTION, structureText)
                || !hasSection(KNOWLEDGE_SECTION, structureText)
                || !hasSection(EXERCISE_SECTION, structureText)) {
            return false;
        }

        String lastLine = structureText.lines()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .reduce((previous, current) -> current)
                .orElse("");
        if (SECTION_ONLY.matcher(lastLine).matches()
                || lastLine.endsWith(":") || lastLine.endsWith("：")
                || lastLine.endsWith("...") || lastLine.endsWith("…")
                || lastLine.endsWith("-") || lastLine.endsWith("—")) {
            return false;
        }

        char lastCharacter = answer.trim().charAt(answer.trim().length() - 1);
        boolean completeExerciseLine = hasCompleteExercise(structureText, lastLine);
        if (Character.isLetterOrDigit(lastCharacter) && !completeExerciseLine) {
            return false;
        }

        return true;
    }

    private static String truncateInternalAppendix(String text) {
        String lowerCase = text.toLowerCase(Locale.ROOT);
        String[] markers = {
                "从检索上下文", "从上下文列表", "最终输出必须", "检查是否符合",
                "从用户提供的上下文列表", "输出必须严格", "严格输出五个部分",
                "所以，直接写", "确保不编造", "用户强调", "用户说", "在输出中，确保",
                "我需要直接输出", "直接输出这个", "确保格式正确"
        };
        int cutIndex = text.length();
        for (String marker : markers) {
            int index = lowerCase.indexOf(marker.toLowerCase(Locale.ROOT));
            if (index > 0 && index < cutIndex) {
                cutIndex = index;
            }
        }
        return text.substring(0, cutIndex).trim();
    }

    private static boolean hasSection(Pattern pattern, String answer) {
        return pattern.matcher(answer).find();
    }

    private static boolean hasCompleteExercise(String answer, String lastLine) {
        if (lastLine.contains("正确答案") || lastLine.contains("选项")) {
            return true;
        }
        Matcher exercise = EXERCISE_SECTION.matcher(answer);
        if (!exercise.find()) {
            return false;
        }
        String exerciseText = answer.substring(exercise.end());
        return optionExists(exerciseText, 'A') && optionExists(exerciseText, 'B')
                && optionExists(exerciseText, 'C') && optionExists(exerciseText, 'D');
    }

    private static boolean optionExists(String text, char option) {
        return Pattern.compile("(?m)(?<![A-Za-z0-9])" + option + "\\s*[.．、)]\\s+").matcher(text).find();
    }
}
