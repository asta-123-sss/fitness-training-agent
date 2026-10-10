package com.fitness.agent.agent;

/**
 * 从 LLM 输出中抽取 JSON 片段。LLM 可能把 JSON 包裹在解释文字或 Markdown 代码块里，
 * 这里按大括号配平的方式取出第一个完整 JSON 对象。
 */
public final class JsonExtractor {

    private JsonExtractor() {
    }

    public static String extractJson(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        int start = text.indexOf('{');
        if (start < 0) {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            // 跳过字符串内部内容，避免字符串中的 {} 干扰括号配平
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        return null;
    }
}
