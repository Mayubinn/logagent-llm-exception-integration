package com.logagent.sdk.spring;

import com.logagent.sdk.ExceptionAnalyzerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Boot 外部化配置：exception.llm.*
 */
@ConfigurationProperties(prefix = "exception.llm")
public class ExceptionAnalyzerProperties {

    private boolean enabled = false;

    private boolean includeRequestContext = true;
    private boolean includeQueryString = false;
    private boolean includeClientIp = false;
    private boolean includeTraceId = true;
    private boolean includeHandlerSignature = true;
    private String traceIdMdcKey = "traceId";
    private int maxContextLength = 1024;

    private int corePoolSize = 1;
    private int maxPoolSize = 2;
    private int queueCapacity = 64;
    private int keepAliveSec = 60;

    private String llmApiUrl = "https://api.openai.com/v1/chat/completions";
    private String llmModel = "gpt-4o-mini";
    private int maxTokens = 1500;
    private double temperature = 0.2D;

    private int connectTimeoutSec = 10;
    private int readTimeoutSec = 60;
    private int writeTimeoutSec = 10;

    private String authHeaderName = "Authorization";
    private String authToken;
    private Map<String, String> headers = new LinkedHashMap<String, String>();

    private String customSystemPrompt;
    private ExceptionAnalyzerConfig.OutputMode outputMode = ExceptionAnalyzerConfig.OutputMode.LOG;
    private int maxExceptionChars = 16000;

    private String telemetryName = "logagent";
    private String telemetryClientName = "logagent-exception-analyzer-sdk";
    private String telemetryClientVersion = "1.0.1";

    private boolean codeContextEnabled = true;
    private int codeContextLinesBefore = 20;
    private int codeContextLinesAfter = 20;
    private int codeContextMaxChars = 12000;
    private boolean searchSourcesJar = true;
    private boolean decompileWhenSourceMissing = true;
    private int maxCodeFrames = 2;
    private List<String> applicationPackages = new ArrayList<String>();
    private List<String> sourceRoots = new ArrayList<String>();

    public ExceptionAnalyzerProperties() {
        this.sourceRoots.add("src/main/java");
        this.sourceRoots.add("src/test/java");
    }

    public ExceptionAnalyzerConfig.Builder toBuilder() {
        return ExceptionAnalyzerConfig.builder()
                .corePoolSize(corePoolSize)
                .maxPoolSize(maxPoolSize)
                .queueCapacity(queueCapacity)
                .keepAliveSec(keepAliveSec)
                .llmApiUrl(llmApiUrl)
                .llmModel(llmModel)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .connectTimeoutSec(connectTimeoutSec)
                .readTimeoutSec(readTimeoutSec)
                .writeTimeoutSec(writeTimeoutSec)
                .authHeaderName(authHeaderName)
                .authToken(authToken)
                .headers(headers)
                .customSystemPrompt(customSystemPrompt)
                .outputMode(outputMode)
                .maxExceptionChars(maxExceptionChars)
                .telemetryName(telemetryName)
                .telemetryClientName(telemetryClientName)
                .telemetryClientVersion(telemetryClientVersion)
                .codeContextEnabled(codeContextEnabled)
                .codeContextLinesBefore(codeContextLinesBefore)
                .codeContextLinesAfter(codeContextLinesAfter)
                .codeContextMaxChars(codeContextMaxChars)
                .searchSourcesJar(searchSourcesJar)
                .decompileWhenSourceMissing(decompileWhenSourceMissing)
                .maxCodeFrames(maxCodeFrames)
                .applicationPackages(applicationPackages)
                .sourceRoots(sourceRoots);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isIncludeRequestContext() {
        return includeRequestContext;
    }

    public void setIncludeRequestContext(boolean includeRequestContext) {
        this.includeRequestContext = includeRequestContext;
    }

    public boolean isIncludeQueryString() {
        return includeQueryString;
    }

    public void setIncludeQueryString(boolean includeQueryString) {
        this.includeQueryString = includeQueryString;
    }

    public boolean isIncludeClientIp() {
        return includeClientIp;
    }

    public void setIncludeClientIp(boolean includeClientIp) {
        this.includeClientIp = includeClientIp;
    }

    public boolean isIncludeTraceId() {
        return includeTraceId;
    }

    public void setIncludeTraceId(boolean includeTraceId) {
        this.includeTraceId = includeTraceId;
    }

    public boolean isIncludeHandlerSignature() {
        return includeHandlerSignature;
    }

    public void setIncludeHandlerSignature(boolean includeHandlerSignature) {
        this.includeHandlerSignature = includeHandlerSignature;
    }

    public String getTraceIdMdcKey() {
        return traceIdMdcKey;
    }

    public void setTraceIdMdcKey(String traceIdMdcKey) {
        this.traceIdMdcKey = traceIdMdcKey;
    }

    public int getMaxContextLength() {
        return maxContextLength;
    }

    public void setMaxContextLength(int maxContextLength) {
        this.maxContextLength = maxContextLength;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public int getKeepAliveSec() {
        return keepAliveSec;
    }

    public void setKeepAliveSec(int keepAliveSec) {
        this.keepAliveSec = keepAliveSec;
    }

    public String getLlmApiUrl() {
        return llmApiUrl;
    }

    public void setLlmApiUrl(String llmApiUrl) {
        this.llmApiUrl = llmApiUrl;
    }

    public String getLlmModel() {
        return llmModel;
    }

    public void setLlmModel(String llmModel) {
        this.llmModel = llmModel;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getConnectTimeoutSec() {
        return connectTimeoutSec;
    }

    public void setConnectTimeoutSec(int connectTimeoutSec) {
        this.connectTimeoutSec = connectTimeoutSec;
    }

    public int getReadTimeoutSec() {
        return readTimeoutSec;
    }

    public void setReadTimeoutSec(int readTimeoutSec) {
        this.readTimeoutSec = readTimeoutSec;
    }

    public int getWriteTimeoutSec() {
        return writeTimeoutSec;
    }

    public void setWriteTimeoutSec(int writeTimeoutSec) {
        this.writeTimeoutSec = writeTimeoutSec;
    }

    public String getAuthHeaderName() {
        return authHeaderName;
    }

    public void setAuthHeaderName(String authHeaderName) {
        this.authHeaderName = authHeaderName;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers == null ? new LinkedHashMap<String, String>() : headers;
    }

    public String getCustomSystemPrompt() {
        return customSystemPrompt;
    }

    public void setCustomSystemPrompt(String customSystemPrompt) {
        this.customSystemPrompt = customSystemPrompt;
    }

    public ExceptionAnalyzerConfig.OutputMode getOutputMode() {
        return outputMode;
    }

    public void setOutputMode(ExceptionAnalyzerConfig.OutputMode outputMode) {
        this.outputMode = outputMode;
    }

    public int getMaxExceptionChars() {
        return maxExceptionChars;
    }

    public void setMaxExceptionChars(int maxExceptionChars) {
        this.maxExceptionChars = maxExceptionChars;
    }

    public String getTelemetryName() {
        return telemetryName;
    }

    public void setTelemetryName(String telemetryName) {
        this.telemetryName = telemetryName;
    }

    public String getTelemetryClientName() {
        return telemetryClientName;
    }

    public void setTelemetryClientName(String telemetryClientName) {
        this.telemetryClientName = telemetryClientName;
    }

    public String getTelemetryClientVersion() {
        return telemetryClientVersion;
    }

    public void setTelemetryClientVersion(String telemetryClientVersion) {
        this.telemetryClientVersion = telemetryClientVersion;
    }

    public boolean isCodeContextEnabled() {
        return codeContextEnabled;
    }

    public void setCodeContextEnabled(boolean codeContextEnabled) {
        this.codeContextEnabled = codeContextEnabled;
    }

    public int getCodeContextLinesBefore() {
        return codeContextLinesBefore;
    }

    public void setCodeContextLinesBefore(int codeContextLinesBefore) {
        this.codeContextLinesBefore = codeContextLinesBefore;
    }

    public int getCodeContextLinesAfter() {
        return codeContextLinesAfter;
    }

    public void setCodeContextLinesAfter(int codeContextLinesAfter) {
        this.codeContextLinesAfter = codeContextLinesAfter;
    }

    public int getCodeContextMaxChars() {
        return codeContextMaxChars;
    }

    public void setCodeContextMaxChars(int codeContextMaxChars) {
        this.codeContextMaxChars = codeContextMaxChars;
    }

    public boolean isSearchSourcesJar() {
        return searchSourcesJar;
    }

    public void setSearchSourcesJar(boolean searchSourcesJar) {
        this.searchSourcesJar = searchSourcesJar;
    }

    public boolean isDecompileWhenSourceMissing() {
        return decompileWhenSourceMissing;
    }

    public void setDecompileWhenSourceMissing(boolean decompileWhenSourceMissing) {
        this.decompileWhenSourceMissing = decompileWhenSourceMissing;
    }

    public int getMaxCodeFrames() {
        return maxCodeFrames;
    }

    public void setMaxCodeFrames(int maxCodeFrames) {
        this.maxCodeFrames = maxCodeFrames;
    }

    public List<String> getApplicationPackages() {
        return applicationPackages;
    }

    public void setApplicationPackages(List<String> applicationPackages) {
        this.applicationPackages = applicationPackages == null ? new ArrayList<String>() : applicationPackages;
    }

    public List<String> getSourceRoots() {
        return sourceRoots;
    }

    public void setSourceRoots(List<String> sourceRoots) {
        this.sourceRoots = sourceRoots == null ? new ArrayList<String>() : sourceRoots;
    }
}
