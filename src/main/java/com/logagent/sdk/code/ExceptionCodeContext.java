package com.logagent.sdk.code;

import com.logagent.sdk.util.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 异常对应的代码上下文集合。
 */
public final class ExceptionCodeContext {

    private final List<CodeSnippet> snippets;

    public ExceptionCodeContext(List<CodeSnippet> snippets) {
        if (snippets == null || snippets.isEmpty()) {
            this.snippets = Collections.emptyList();
        } else {
            this.snippets = Collections.unmodifiableList(new ArrayList<CodeSnippet>(snippets));
        }
    }

    public boolean hasContent() {
        if (snippets.isEmpty()) {
            return false;
        }
        for (CodeSnippet snippet : snippets) {
            if (snippet != null && snippet.hasUsableContent()) {
                return true;
            }
        }
        return false;
    }

    public List<CodeSnippet> getSnippets() {
        return snippets;
    }

    public String toPromptText(int maxChars) {
        if (!hasContent()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(4096);
        for (CodeSnippet snippet : snippets) {
            if (snippet == null || !snippet.hasUsableContent()) {
                continue;
            }
            sb.append("#### ").append(TextUtils.safe(snippet.getLabel())).append('\n');
            sb.append("- class: ").append(TextUtils.safe(snippet.getClassName())).append('\n');
            sb.append("- method: ").append(TextUtils.safe(snippet.getMethodName())).append('\n');
            sb.append("- file: ").append(TextUtils.safe(snippet.getFileName())).append('\n');
            sb.append("- line: ").append(snippet.getLineNumber()).append('\n');
            sb.append("- sourceType: ").append(snippet.getSourceType()).append('\n');
            if (!TextUtils.isBlank(snippet.getSourceOrigin())) {
                sb.append("- sourceOrigin: ").append(snippet.getSourceOrigin()).append('\n');
            }
            if (!TextUtils.isBlank(snippet.getNote())) {
                sb.append("- note: ").append(snippet.getNote()).append('\n');
            }
            sb.append("```java\n");
            sb.append(TextUtils.normalizeLineBreaks(snippet.getContent())).append('\n');
            sb.append("```\n\n");
        }
        return TextUtils.abbreviate(sb.toString(), maxChars);
    }
}
