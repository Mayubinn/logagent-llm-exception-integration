package com.logagent.sdk;

import com.example.demo.ExceptionFlowDemo;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ExceptionAnalyzerLiveOpenAiTest {

    @Test
    public void shouldAnalyzeExceptionWithOpenAiWhenTestPropertiesConfigured() throws Exception {
        Properties properties = loadTestProperties();
        String apiUrl = readRequired(properties, "openai.api.url");
        String apiKey = readRequired(properties, "openai.api.key");
        String model = readOptional(properties, "openai.model", "gpt-4o-mini");

        Assume.assumeTrue(apiUrl != null && apiKey != null);

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

    private Properties loadTestProperties() throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream("openai-test.properties");
        Assume.assumeTrue(in != null);

        Properties properties = new Properties();
        try {
            properties.load(in);
        } finally {
            in.close();
        }
        return properties;
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

    private String readRequired(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            return null;
        }
        value = value.trim();
        if (value.isEmpty() || value.startsWith("your-")) {
            return null;
        }
        return value;
    }

    private String readOptional(Properties properties, String key, String defaultValue) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }
}
