package com.logagent.sdk;

import com.example.demo.ExceptionFlowDemo;
import org.junit.Assert;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

public class ExceptionAnalyzerAnalyzeSyncTest {

    @Test
    public void shouldCallOpenAiCompatibleApiAndReturnAnalysisReport() throws Exception {
        AtomicReference<String> requestBodyRef = new AtomicReference<String>();
        AtomicReference<String> authHeaderRef = new AtomicReference<String>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                authHeaderRef.set(exchange.getRequestHeaders().getFirst("Authorization"));
                requestBodyRef.set(readRequestBody(exchange.getRequestBody()));

                byte[] responseBody = ("{"
                        + "\"choices\":[{"
                        + "\"message\":{"
                        + "\"role\":\"assistant\","
                        + "\"content\":\"模拟分析结果：请检查 loadProfile 中的数字转换逻辑\""
                        + "}"
                        + "}]"
                        + "}").getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, responseBody.length);
                OutputStream out = exchange.getResponseBody();
                try {
                    out.write(responseBody);
                } finally {
                    out.close();
                }
            }
        });
        server.start();

        ExceptionAnalyzer analyzer = null;
        try {
            ExceptionAnalyzerConfig config = ExceptionAnalyzerConfig.builder()
                    .llmApiUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions")
                    .llmModel("gpt-4o-mini")
                    .authHeaderName("Authorization")
                    .authToken("Bearer test-openai-key")
                    .addApplicationPackage("com.example.demo")
                    .codeContextLinesBefore(2)
                    .codeContextLinesAfter(2)
                    .build();

            analyzer = ExceptionAnalyzer.create(config);
            String report = analyzer.analyzeSync(captureException(), "requestUri=/demo/test\nhttpMethod=GET");

            Assert.assertEquals("模拟分析结果：请检查 loadProfile 中的数字转换逻辑", report);
            Assert.assertEquals("Bearer test-openai-key", authHeaderRef.get());

            String requestBody = requestBodyRef.get();
            Assert.assertNotNull(requestBody);
            Assert.assertTrue(requestBody.contains("\"model\":\"gpt-4o-mini\""));
            Assert.assertTrue(requestBody.contains("requestUri=/demo/test"));
            Assert.assertTrue(requestBody.contains("### 异常堆栈"));
            Assert.assertTrue(requestBody.contains("### 报错代码位置"));
            Assert.assertTrue(requestBody.contains("throw new RuntimeException"));
            Assert.assertTrue(requestBody.contains("Integer.parseInt(rawValue);"));
        } finally {
            if (analyzer != null) {
                analyzer.shutdown();
            }
            server.stop(0);
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

    private String readRequestBody(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, len);
        }
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }
}
