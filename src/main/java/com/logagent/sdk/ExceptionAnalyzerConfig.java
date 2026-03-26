package com.logagent.sdk;

import com.logagent.sdk.util.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ExceptionAnalyzer 的全部客户化参数。
 */
public final class ExceptionAnalyzerConfig {

    public enum OutputMode {
        LOG,
        STDOUT,
        CALLBACK
    }

    private final int corePoolSize;
    private final int maxPoolSize;
    private final int queueCapacity;
    private final int keepAliveSec;

    private final String llmApiUrl;
    private final String llmModel;
    private final int maxTokens;
    private final double temperature;

    private final int connectTimeoutSec;
    private final int readTimeoutSec;
    private final int writeTimeoutSec;

    private final String authHeaderName;
    private final String authToken;
    private final Map<String, String> headers;

    private final String customSystemPrompt;
    private final OutputMode outputMode;
    private final int maxExceptionChars;

    private final String telemetryName;
    private final String telemetryClientName;
    private final String telemetryClientVersion;

    private final boolean codeContextEnabled;
    private final int codeContextLinesBefore;
    private final int codeContextLinesAfter;
    private final int codeContextMaxChars;
    private final boolean searchSourcesJar;
    private final boolean decompileWhenSourceMissing;
    private final int maxCodeFrames;
    private final List<String> applicationPackages;
    private final List<String> sourceRoots;

    private ExceptionAnalyzerConfig(Builder builder) {
        this.corePoolSize = builder.corePoolSize;
        this.maxPoolSize = builder.maxPoolSize;
        this.queueCapacity = builder.queueCapacity;
        this.keepAliveSec = builder.keepAliveSec;
        this.llmApiUrl = builder.llmApiUrl;
        this.llmModel = builder.llmModel;
        this.maxTokens = builder.maxTokens;
        this.temperature = builder.temperature;
        this.connectTimeoutSec = builder.connectTimeoutSec;
        this.readTimeoutSec = builder.readTimeoutSec;
        this.writeTimeoutSec = builder.writeTimeoutSec;
        this.authHeaderName = builder.authHeaderName;
        this.authToken = builder.authToken;
        this.headers = TextUtils.copyMap(builder.headers);
        this.customSystemPrompt = builder.customSystemPrompt;
        this.outputMode = builder.outputMode;
        this.maxExceptionChars = builder.maxExceptionChars;
        this.telemetryName = builder.telemetryName;
        this.telemetryClientName = builder.telemetryClientName;
        this.telemetryClientVersion = builder.telemetryClientVersion;
        this.codeContextEnabled = builder.codeContextEnabled;
        this.codeContextLinesBefore = builder.codeContextLinesBefore;
        this.codeContextLinesAfter = builder.codeContextLinesAfter;
        this.codeContextMaxChars = builder.codeContextMaxChars;
        this.searchSourcesJar = builder.searchSourcesJar;
        this.decompileWhenSourceMissing = builder.decompileWhenSourceMissing;
        this.maxCodeFrames = builder.maxCodeFrames;
        this.applicationPackages = TextUtils.copyList(builder.applicationPackages);
        this.sourceRoots = TextUtils.copyList(builder.sourceRoots);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ExceptionAnalyzerConfig defaults() {
        return builder().build();
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public int getKeepAliveSec() {
        return keepAliveSec;
    }

    public String getLlmApiUrl() {
        return llmApiUrl;
    }

    public String getLlmModel() {
        return llmModel;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getConnectTimeoutSec() {
        return connectTimeoutSec;
    }

    public int getReadTimeoutSec() {
        return readTimeoutSec;
    }

    public int getWriteTimeoutSec() {
        return writeTimeoutSec;
    }

    public String getAuthHeaderName() {
        return authHeaderName;
    }

    public String getAuthToken() {
        return authToken;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getCustomSystemPrompt() {
        return customSystemPrompt;
    }

    public OutputMode getOutputMode() {
        return outputMode;
    }

    public int getMaxExceptionChars() {
        return maxExceptionChars;
    }

    public String getTelemetryName() {
        return telemetryName;
    }

    public String getTelemetryClientName() {
        return telemetryClientName;
    }

    public String getTelemetryClientVersion() {
        return telemetryClientVersion;
    }

    public boolean isCodeContextEnabled() {
        return codeContextEnabled;
    }

    public int getCodeContextLinesBefore() {
        return codeContextLinesBefore;
    }

    public int getCodeContextLinesAfter() {
        return codeContextLinesAfter;
    }

    public int getCodeContextMaxChars() {
        return codeContextMaxChars;
    }

    public boolean isSearchSourcesJar() {
        return searchSourcesJar;
    }

    public boolean isDecompileWhenSourceMissing() {
        return decompileWhenSourceMissing;
    }

    public int getMaxCodeFrames() {
        return maxCodeFrames;
    }

    public List<String> getApplicationPackages() {
        return applicationPackages;
    }

    public List<String> getSourceRoots() {
        return sourceRoots;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExceptionAnalyzerConfig)) {
            return false;
        }
        ExceptionAnalyzerConfig that = (ExceptionAnalyzerConfig) o;
        return corePoolSize == that.corePoolSize
                && maxPoolSize == that.maxPoolSize
                && queueCapacity == that.queueCapacity
                && keepAliveSec == that.keepAliveSec
                && maxTokens == that.maxTokens
                && Double.compare(that.temperature, temperature) == 0
                && connectTimeoutSec == that.connectTimeoutSec
                && readTimeoutSec == that.readTimeoutSec
                && writeTimeoutSec == that.writeTimeoutSec
                && maxExceptionChars == that.maxExceptionChars
                && codeContextEnabled == that.codeContextEnabled
                && codeContextLinesBefore == that.codeContextLinesBefore
                && codeContextLinesAfter == that.codeContextLinesAfter
                && codeContextMaxChars == that.codeContextMaxChars
                && searchSourcesJar == that.searchSourcesJar
                && decompileWhenSourceMissing == that.decompileWhenSourceMissing
                && maxCodeFrames == that.maxCodeFrames
                && Objects.equals(llmApiUrl, that.llmApiUrl)
                && Objects.equals(llmModel, that.llmModel)
                && Objects.equals(authHeaderName, that.authHeaderName)
                && Objects.equals(authToken, that.authToken)
                && Objects.equals(headers, that.headers)
                && Objects.equals(customSystemPrompt, that.customSystemPrompt)
                && outputMode == that.outputMode
                && Objects.equals(telemetryName, that.telemetryName)
                && Objects.equals(telemetryClientName, that.telemetryClientName)
                && Objects.equals(telemetryClientVersion, that.telemetryClientVersion)
                && Objects.equals(applicationPackages, that.applicationPackages)
                && Objects.equals(sourceRoots, that.sourceRoots);
    }

    @Override
    public int hashCode() {
        return Objects.hash(corePoolSize, maxPoolSize, queueCapacity, keepAliveSec,
                llmApiUrl, llmModel, maxTokens, temperature,
                connectTimeoutSec, readTimeoutSec, writeTimeoutSec,
                authHeaderName, authToken, headers,
                customSystemPrompt, outputMode, maxExceptionChars,
                telemetryName, telemetryClientName, telemetryClientVersion,
                codeContextEnabled, codeContextLinesBefore, codeContextLinesAfter,
                codeContextMaxChars, searchSourcesJar, decompileWhenSourceMissing,
                maxCodeFrames, applicationPackages, sourceRoots);
    }

    @Override
    public String toString() {
        return "ExceptionAnalyzerConfig{" +
                "llmApiUrl='" + llmApiUrl + '\'' +
                ", llmModel='" + llmModel + '\'' +
                ", maxTokens=" + maxTokens +
                ", temperature=" + temperature +
                ", outputMode=" + outputMode +
                ", authHeaderName='" + authHeaderName + '\'' +
                ", authToken='" + TextUtils.maskToken(authToken) + '\'' +
                ", codeContextEnabled=" + codeContextEnabled +
                ", applicationPackages=" + applicationPackages +
                '}';
    }

    public static final class Builder {
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
        private OutputMode outputMode = OutputMode.LOG;
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
        private List<String> sourceRoots = new ArrayList<String>(Arrays.asList("src/main/java", "src/test/java"));

        private Builder() {
        }

        public Builder corePoolSize(int value) {
            this.corePoolSize = value;
            return this;
        }

        public Builder maxPoolSize(int value) {
            this.maxPoolSize = value;
            return this;
        }

        public Builder queueCapacity(int value) {
            this.queueCapacity = value;
            return this;
        }

        public Builder keepAliveSec(int value) {
            this.keepAliveSec = value;
            return this;
        }

        public Builder llmApiUrl(String value) {
            this.llmApiUrl = value;
            return this;
        }

        public Builder llmModel(String value) {
            this.llmModel = value;
            return this;
        }

        public Builder maxTokens(int value) {
            this.maxTokens = value;
            return this;
        }

        public Builder temperature(double value) {
            this.temperature = value;
            return this;
        }

        public Builder connectTimeoutSec(int value) {
            this.connectTimeoutSec = value;
            return this;
        }

        public Builder readTimeoutSec(int value) {
            this.readTimeoutSec = value;
            return this;
        }

        public Builder writeTimeoutSec(int value) {
            this.writeTimeoutSec = value;
            return this;
        }

        public Builder authHeaderName(String value) {
            this.authHeaderName = value;
            return this;
        }

        public Builder authToken(String value) {
            this.authToken = value;
            return this;
        }

        public Builder headers(Map<String, String> value) {
            this.headers = value == null ? new LinkedHashMap<String, String>() : new LinkedHashMap<String, String>(value);
            return this;
        }

        public Builder header(String key, String value) {
            if (!TextUtils.isBlank(key) && value != null) {
                this.headers.put(key, value);
            }
            return this;
        }

        public Builder customSystemPrompt(String value) {
            this.customSystemPrompt = value;
            return this;
        }

        public Builder outputMode(OutputMode value) {
            this.outputMode = value == null ? OutputMode.LOG : value;
            return this;
        }

        public Builder maxExceptionChars(int value) {
            this.maxExceptionChars = value;
            return this;
        }

        public Builder telemetryName(String value) {
            this.telemetryName = value;
            return this;
        }

        public Builder telemetryClientName(String value) {
            this.telemetryClientName = value;
            return this;
        }

        public Builder telemetryClientVersion(String value) {
            this.telemetryClientVersion = value;
            return this;
        }

        public Builder codeContextEnabled(boolean value) {
            this.codeContextEnabled = value;
            return this;
        }

        public Builder codeContextLinesBefore(int value) {
            this.codeContextLinesBefore = value;
            return this;
        }

        public Builder codeContextLinesAfter(int value) {
            this.codeContextLinesAfter = value;
            return this;
        }

        public Builder codeContextMaxChars(int value) {
            this.codeContextMaxChars = value;
            return this;
        }

        public Builder searchSourcesJar(boolean value) {
            this.searchSourcesJar = value;
            return this;
        }

        public Builder decompileWhenSourceMissing(boolean value) {
            this.decompileWhenSourceMissing = value;
            return this;
        }

        public Builder maxCodeFrames(int value) {
            this.maxCodeFrames = value;
            return this;
        }

        public Builder applicationPackages(List<String> values) {
            this.applicationPackages = values == null ? new ArrayList<String>() : new ArrayList<String>(values);
            return this;
        }

        public Builder addApplicationPackage(String value) {
            if (!TextUtils.isBlank(value)) {
                this.applicationPackages.add(value.trim());
            }
            return this;
        }

        public Builder sourceRoots(List<String> values) {
            this.sourceRoots = values == null ? new ArrayList<String>() : new ArrayList<String>(values);
            return this;
        }

        public Builder addSourceRoot(String value) {
            if (!TextUtils.isBlank(value)) {
                this.sourceRoots.add(value.trim());
            }
            return this;
        }

        public ExceptionAnalyzerConfig build() {
            return new ExceptionAnalyzerConfig(this);
        }
    }
}
