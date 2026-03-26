package com.logagent.sdk;

import com.example.demo.ExceptionFlowDemo;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class ExceptionAnalyzerLiveOpenAiTest {

    @Test
    public void shouldAnalyzeExceptionWithOpenAiWhenApiKeyConfigured() throws Exception {
        String apiKey = System.getenv("OPENAI_API_KEY");
        Assume.assumeTrue(apiKey != null && apiKey.trim().length() > 0);

        String apiUrl = envOrDefault("OPENAI_API_URL", "https://api.openai.com/v1/chat/completions");
        String model = envOrDefault("OPENAI_MODEL", "gpt-4o-mini");

        ExceptionAnalyzer analyzer = null;
        try {
            ExceptionAnalyzerConfig config = ExceptionAnalyzerConfig.builder()
                    .llmApiUrl(apiUrl)
                    .llmModel(model)
                    .authHeaderName("Authorization")
                    .authToken("Bearer " + apiKey)
                    .customSystemPrompt("你是一位 Java 故障分析助手，请给出简洁中文结论。")
                    .addApplicationPackage("com.example.demo")
                    .codeContextLinesBefore(2)
                    .codeContextLinesAfter(2)
                    .maxTokens(400)
                    .build();

            analyzer = ExceptionAnalyzer.create(config);
            String report = analyzer.analyzeSync(captureException(), "requestUri=/demo/test\nhttpMethod=GET");

            Assert.assertNotNull(report);
            Assert.assertFalse(report.trim().isEmpty());
        } finally {
            if (analyzer != null) {
                analyzer.shutdown();
            }
        }
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

    private String envOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }
}
