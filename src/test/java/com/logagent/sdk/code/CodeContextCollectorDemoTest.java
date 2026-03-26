package com.logagent.sdk.code;

import com.example.demo.ExceptionFlowDemo;
import com.logagent.sdk.ExceptionAnalyzerConfig;
import org.junit.Assert;
import org.junit.Test;

public class CodeContextCollectorDemoTest {

    @Test
    public void shouldCollectSourceSnippetsForThrowPointAndRootCause() {
        Throwable exception = captureException();

        ExceptionAnalyzerConfig config = ExceptionAnalyzerConfig.builder()
                .addApplicationPackage("com.example.demo")
                .codeContextLinesBefore(2)
                .codeContextLinesAfter(2)
                .build();

        ExceptionCodeContext codeContext = new CodeContextCollector().collect(exception, config);

        Assert.assertTrue(codeContext.hasContent());
        Assert.assertEquals(2, codeContext.getSnippets().size());

        CodeSnippet throwPoint = codeContext.getSnippets().get(0);
        Assert.assertEquals("异常抛出点", throwPoint.getLabel());
        Assert.assertEquals("com.example.demo.ExceptionFlowDemo", throwPoint.getClassName());
        Assert.assertEquals("handleRequest", throwPoint.getMethodName());
        Assert.assertEquals(CodeSnippet.SourceType.SOURCE_FILE, throwPoint.getSourceType());
        Assert.assertTrue(throwPoint.getContent().contains("loadProfile(userId);"));
        Assert.assertTrue(throwPoint.getContent().contains("throw new RuntimeException"));

        CodeSnippet rootCause = codeContext.getSnippets().get(1);
        Assert.assertEquals("根因位置", rootCause.getLabel());
        Assert.assertEquals("loadProfile", rootCause.getMethodName());
        Assert.assertEquals(CodeSnippet.SourceType.SOURCE_FILE, rootCause.getSourceType());
        Assert.assertTrue(rootCause.getContent().contains("Integer.parseInt(rawValue);"));

        String promptText = codeContext.toPromptText(4000);
        Assert.assertTrue(promptText.contains("#### 异常抛出点"));
        Assert.assertTrue(promptText.contains("#### 根因位置"));
        Assert.assertTrue(promptText.contains("sourceType: SOURCE_FILE"));
        Assert.assertTrue(promptText.contains("Integer.parseInt(rawValue);"));
    }

    private Throwable captureException() {
        try {
            new ExceptionFlowDemo().handleRequest("42");
            Assert.fail("expected exception");
            return null;
        } catch (RuntimeException ex) {
            return ex;
        }
    }
}
