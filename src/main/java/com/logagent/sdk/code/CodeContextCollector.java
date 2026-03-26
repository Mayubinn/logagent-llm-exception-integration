package com.logagent.sdk.code;

import com.logagent.sdk.ExceptionAnalyzerConfig;
import com.logagent.sdk.util.TextUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 从异常堆栈中挑选业务帧，并提取对应源码/反编译代码。
 */
public final class CodeContextCollector {

    private static final String[] FRAMEWORK_PREFIXES = new String[]{
            "java.",
            "javax.",
            "jdk.",
            "sun.",
            "com.sun.",
            "org.springframework.",
            "org.apache.",
            "org.slf4j.",
            "ch.qos.logback.",
            "com.fasterxml.",
            "okhttp3.",
            "kotlin.",
            "reactor.",
            "net.sf.cglib.",
            "org.hibernate.",
            "org.mybatis.",
            "com.logagent.sdk."
    };

    private final SourceCodeLocator sourceCodeLocator = new SourceCodeLocator();

    public ExceptionCodeContext collect(Throwable throwable, ExceptionAnalyzerConfig config) {
        if (throwable == null || config == null || !config.isCodeContextEnabled()) {
            return new ExceptionCodeContext(null);
        }
        List<CodeSnippet> snippets = new ArrayList<CodeSnippet>();
        Set<String> seen = new HashSet<String>();

        collectCandidate(snippets, seen, throwable, "异常抛出点", config);

        Throwable root = findRootCause(throwable);
        if (root != null && root != throwable) {
            collectCandidate(snippets, seen, root, "根因位置", config);
        }

        return new ExceptionCodeContext(snippets);
    }

    private void collectCandidate(List<CodeSnippet> snippets,
                                  Set<String> seen,
                                  Throwable throwable,
                                  String label,
                                  ExceptionAnalyzerConfig config) {
        if (throwable == null || snippets.size() >= config.getMaxCodeFrames()) {
            return;
        }
        StackTraceElement frame = selectApplicationFrame(throwable.getStackTrace(), config);
        if (frame == null) {
            return;
        }
        String key = frame.getClassName() + "#" + frame.getMethodName() + ":" + frame.getLineNumber();
        if (!seen.add(key)) {
            return;
        }
        snippets.add(sourceCodeLocator.locate(label, frame, config));
    }

    private StackTraceElement selectApplicationFrame(StackTraceElement[] frames, ExceptionAnalyzerConfig config) {
        if (frames == null || frames.length == 0) {
            return null;
        }
        for (StackTraceElement frame : frames) {
            if (isApplicationFrame(frame, config)) {
                return frame;
            }
        }
        return frames[0];
    }

    private boolean isApplicationFrame(StackTraceElement frame, ExceptionAnalyzerConfig config) {
        if (frame == null || TextUtils.isBlank(frame.getClassName())) {
            return false;
        }
        String className = frame.getClassName();
        List<String> applicationPackages = config.getApplicationPackages();
        if (applicationPackages != null && !applicationPackages.isEmpty()) {
            for (String prefix : applicationPackages) {
                if (!TextUtils.isBlank(prefix) && className.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }
        for (String prefix : FRAMEWORK_PREFIXES) {
            if (className.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    private Throwable findRootCause(Throwable throwable) {
        Throwable cursor = throwable;
        Set<Throwable> seen = new HashSet<Throwable>();
        while (cursor != null && cursor.getCause() != null && seen.add(cursor.getCause())) {
            cursor = cursor.getCause();
        }
        return cursor;
    }
}
