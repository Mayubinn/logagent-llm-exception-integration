package com.logagent.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.logagent.sdk.code.CodeContextCollector;
import com.logagent.sdk.code.ExceptionCodeContext;
import com.logagent.sdk.util.TextUtils;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 连接 LLM API 的 Java 异常分析器。
 * <p>
 * 发送给 LLM 的内容包含：
 * <ul>
 *     <li>异常堆栈</li>
 *     <li>请求上下文（如果调用方传入）</li>
 *     <li>异常抛出点/根因位置的源码片段</li>
 *     <li>当源码不可用时，自动补充反编译后的 Java 代码或反射骨架</li>
 * </ul>
 */
public class ExceptionAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(ExceptionAnalyzer.class);
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是一位资深 Java / Spring Boot 故障分析专家。输入中可能包含异常堆栈、请求上下文、报错位置源码，" +
                    "或者在 jar 运行场景下附带的反编译 Java 代码。请按以下结构输出中文报告：\n" +
                    "1. 异常概览：异常类型、触发位置、影响范围。\n" +
                    "2. 根因判断：必须优先结合代码位置、调用链和根因异常来判断。\n" +
                    "3. 修复建议：给出可落地的代码级修改建议，尽量指出应该改哪个类/方法。\n" +
                    "4. 风险与预防：说明如何避免再次出现。\n" +
                    "如果输入里标注为反编译代码，请明确说明“反编译代码仅供辅助推断，需以真实源码为准”。";

    private final ExceptionAnalyzerConfig config;
    private final ThreadPoolExecutor executor;
    private final OkHttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final CodeContextCollector codeContextCollector = new CodeContextCollector();

    private volatile AnalysisCallback callback;

    private ExceptionAnalyzer(ExceptionAnalyzerConfig config) {
        this.config = config == null ? ExceptionAnalyzerConfig.defaults() : config;

        this.executor = new ThreadPoolExecutor(
                this.config.getCorePoolSize(),
                this.config.getMaxPoolSize(),
                this.config.getKeepAliveSec(), TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(this.config.getQueueCapacity()),
                new ThreadFactory() {
                    private int sequence = 0;

                    @Override
                    public synchronized Thread newThread(Runnable runnable) {
                        Thread thread = new Thread(runnable, "logagent-exception-analyzer-" + (sequence++));
                        thread.setDaemon(true);
                        return thread;
                    }
                },
                new ThreadPoolExecutor.DiscardPolicy()
        );
        this.executor.allowCoreThreadTimeOut(true);

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(this.config.getConnectTimeoutSec(), TimeUnit.SECONDS)
                .readTimeout(this.config.getReadTimeoutSec(), TimeUnit.SECONDS)
                .writeTimeout(this.config.getWriteTimeoutSec(), TimeUnit.SECONDS)
                .build();
    }

    public static ExceptionAnalyzer create() {
        return new ExceptionAnalyzer(ExceptionAnalyzerConfig.defaults());
    }

    public static ExceptionAnalyzer create(ExceptionAnalyzerConfig config) {
        return new ExceptionAnalyzer(config);
    }

    public void analyze(Throwable exception) {
        analyze(exception, null, null);
    }

    public void analyze(Throwable exception, String contextText) {
        analyze(exception, contextText, null);
    }

    public void analyze(Throwable exception, AnalysisCallback callback) {
        analyze(exception, null, callback);
    }

    public void analyze(Throwable exception, String contextText, AnalysisCallback callback) {
        if (exception == null) {
            return;
        }
        try {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    doAnalyze(exception, contextText, callback);
                }
            });
        } catch (RejectedExecutionException ex) {
            log.warn("[ExceptionAnalyzer] analysis task rejected because the pool is full or already shut down");
        }
    }

    public String analyzeSync(Throwable exception) throws Exception {
        return analyzeSync(exception, null);
    }

    public String analyzeSync(Throwable exception, String contextText) throws Exception {
        if (exception == null) {
            return "";
        }
        return callLlm(buildAnalysisInput(exception, contextText));
    }

    public void setCallback(AnalysisCallback callback) {
        this.callback = callback;
    }

    public String getStats() {
        return String.format("active=%d, poolSize=%d, queueSize=%d, completed=%d",
                executor.getActiveCount(),
                executor.getPoolSize(),
                executor.getQueue().size(),
                executor.getCompletedTaskCount());
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void doAnalyze(Throwable exception, String contextText, AnalysisCallback oneOffCallback) {
        try {
            String report = callLlm(buildAnalysisInput(exception, contextText));
            outputResult(exception, report);
            AnalysisCallback current = oneOffCallback != null ? oneOffCallback : callback;
            if (current != null) {
                current.onSuccess(exception, report);
            }
        } catch (Exception ex) {
            log.error("[ExceptionAnalyzer] analyze failed: {}", ex.getMessage(), ex);
            AnalysisCallback current = oneOffCallback != null ? oneOffCallback : callback;
            if (current != null) {
                current.onFailure(exception, ex);
            }
        }
    }

    private String buildAnalysisInput(Throwable exception, String contextText) {
        StringBuilder sb = new StringBuilder(8192);
        sb.append("### 异常堆栈\n");
        sb.append(TextUtils.abbreviate(formatException(exception), config.getMaxExceptionChars()));

        if (!TextUtils.isBlank(contextText)) {
            sb.append("\n\n### 请求上下文\n");
            sb.append(contextText);
        }

        ExceptionCodeContext codeContext = codeContextCollector.collect(exception, config);
        if (codeContext != null && codeContext.hasContent()) {
            sb.append("\n\n### 报错代码位置\n");
            sb.append(codeContext.toPromptText(config.getCodeContextMaxChars()));
        }

        return sb.toString();
    }

    private String formatException(Throwable exception) {
        StringWriter writer = new StringWriter(4096);
        exception.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private String callLlm(String userContent) throws IOException {
        String systemPrompt = TextUtils.isBlank(config.getCustomSystemPrompt())
                ? DEFAULT_SYSTEM_PROMPT
                : config.getCustomSystemPrompt();

        ObjectNode payload = mapper.createObjectNode();
        payload.put("model", config.getLlmModel());
        payload.put("stream", false);
        payload.put("max_tokens", config.getMaxTokens());
        payload.put("temperature", config.getTemperature());

        ArrayNode messages = payload.putArray("messages");
        ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content", systemPrompt);
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", userContent);

        ObjectNode telemetry = payload.putObject("telemetry");
        telemetry.put("name", config.getTelemetryName());
        telemetry.put("clientName", config.getTelemetryClientName());
        telemetry.put("clientVersion", config.getTelemetryClientVersion());

        String jsonBody = mapper.writeValueAsString(payload);
        RequestBody requestBody = RequestBody.create(JSON_MEDIA_TYPE, jsonBody);
        Request.Builder builder = new Request.Builder()
                .url(config.getLlmApiUrl())
                .post(requestBody);

        if (!TextUtils.isBlank(config.getAuthHeaderName()) && !TextUtils.isBlank(config.getAuthToken())) {
            builder.addHeader(config.getAuthHeaderName(), config.getAuthToken());
        }
        for (Map.Entry<String, String> entry : config.getHeaders().entrySet()) {
            if (!TextUtils.isBlank(entry.getKey()) && entry.getValue() != null) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        Response response = httpClient.newCall(builder.build()).execute();
        try {
            if (!response.isSuccessful()) {
                String body = response.body() == null ? "" : response.body().string();
                throw new IOException("LLM API returned status=" + response.code() + ", body=" + body);
            }
            String body = response.body() == null ? "" : response.body().string();
            return extractContent(body);
        } finally {
            response.close();
        }
    }

    private String extractContent(String responseJson) {
        try {
            JsonNode root = mapper.readTree(responseJson);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).path("message");
                JsonNode content = message.path("content");
                if (!content.isMissingNode()) {
                    return content.asText();
                }
            }
            JsonNode content = root.path("content");
            if (!content.isMissingNode()) {
                return content.asText();
            }
            return responseJson;
        } catch (Exception ex) {
            return responseJson;
        }
    }

    private void outputResult(Throwable exception, String report) {
        String header = "===== LLM Exception Analysis [" + exception.getClass().getName() + "] =====";
        char[] chars = new char[header.length()];
        Arrays.fill(chars, '=');
        String separator = new String(chars);

        switch (config.getOutputMode()) {
            case STDOUT:
                System.out.println(header);
                System.out.println(report);
                System.out.println(separator);
                break;
            case CALLBACK:
                break;
            case LOG:
            default:
                log.info("\n{}\n{}\n{}", header, report, separator);
                break;
        }
    }
}
