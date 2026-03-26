package com.logagent.sdk.code;

import com.logagent.sdk.util.TextUtils;

/**
 * 单个异常代码片段。
 */
public final class CodeSnippet {

    public enum SourceType {
        SOURCE_FILE,
        CLASSPATH_SOURCE,
        SOURCES_JAR,
        DECOMPILED,
        REFLECTION_STUB,
        UNAVAILABLE
    }

    private final String label;
    private final String className;
    private final String methodName;
    private final String fileName;
    private final int lineNumber;
    private final SourceType sourceType;
    private final String sourceOrigin;
    private final String content;
    private final String note;

    public CodeSnippet(String label,
                       String className,
                       String methodName,
                       String fileName,
                       int lineNumber,
                       SourceType sourceType,
                       String sourceOrigin,
                       String content,
                       String note) {
        this.label = label;
        this.className = className;
        this.methodName = methodName;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.sourceType = sourceType == null ? SourceType.UNAVAILABLE : sourceType;
        this.sourceOrigin = sourceOrigin;
        this.content = content;
        this.note = note;
    }

    public String getLabel() {
        return label;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getFileName() {
        return fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public String getSourceOrigin() {
        return sourceOrigin;
    }

    public String getContent() {
        return content;
    }

    public String getNote() {
        return note;
    }

    public boolean hasUsableContent() {
        return !TextUtils.isBlank(content);
    }
}
