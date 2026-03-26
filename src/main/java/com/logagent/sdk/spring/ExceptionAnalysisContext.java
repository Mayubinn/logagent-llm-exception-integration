package com.logagent.sdk.spring;

import com.logagent.sdk.util.TextUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 异常发生时的请求/链路上下文。
 */
public final class ExceptionAnalysisContext {

    private final String requestUri;
    private final String httpMethod;
    private final String queryString;
    private final String clientIp;
    private final String traceId;
    private final String handlerSignature;
    private final Map<String, String> extraAttributes;

    private ExceptionAnalysisContext(Builder builder) {
        this.requestUri = builder.requestUri;
        this.httpMethod = builder.httpMethod;
        this.queryString = builder.queryString;
        this.clientIp = builder.clientIp;
        this.traceId = builder.traceId;
        this.handlerSignature = builder.handlerSignature;
        this.extraAttributes = builder.extraAttributes.isEmpty()
                ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, String>(builder.extraAttributes));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String toPromptText(ExceptionAnalyzerProperties properties) {
        if (properties == null || !properties.isIncludeRequestContext()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(256);
        append(sb, "requestUri", requestUri);
        append(sb, "httpMethod", httpMethod);
        if (properties.isIncludeQueryString()) {
            append(sb, "queryString", queryString);
        }
        if (properties.isIncludeClientIp()) {
            append(sb, "clientIp", clientIp);
        }
        if (properties.isIncludeTraceId()) {
            append(sb, "traceId", traceId);
        }
        if (properties.isIncludeHandlerSignature()) {
            append(sb, "handler", handlerSignature);
        }
        for (Map.Entry<String, String> entry : extraAttributes.entrySet()) {
            append(sb, entry.getKey(), entry.getValue());
        }
        return TextUtils.abbreviate(sb.toString(), properties.getMaxContextLength());
    }

    private void append(StringBuilder sb, String key, String value) {
        if (TextUtils.isBlank(value)) {
            return;
        }
        sb.append(key).append('=').append(value).append('\n');
    }

    public static final class Builder {
        private String requestUri;
        private String httpMethod;
        private String queryString;
        private String clientIp;
        private String traceId;
        private String handlerSignature;
        private final Map<String, String> extraAttributes = new LinkedHashMap<String, String>();

        private Builder() {
        }

        public Builder requestUri(String value) {
            this.requestUri = value;
            return this;
        }

        public Builder httpMethod(String value) {
            this.httpMethod = value;
            return this;
        }

        public Builder queryString(String value) {
            this.queryString = value;
            return this;
        }

        public Builder clientIp(String value) {
            this.clientIp = value;
            return this;
        }

        public Builder traceId(String value) {
            this.traceId = value;
            return this;
        }

        public Builder handlerSignature(String value) {
            this.handlerSignature = value;
            return this;
        }

        public Builder attribute(String key, String value) {
            if (!TextUtils.isBlank(key) && value != null) {
                this.extraAttributes.put(key, value);
            }
            return this;
        }

        public ExceptionAnalysisContext build() {
            return new ExceptionAnalysisContext(this);
        }
    }
}
