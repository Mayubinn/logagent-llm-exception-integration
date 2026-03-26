package com.logagent.sdk.spring;

import com.logagent.sdk.ExceptionAnalyzerConfig;

/**
 * 业务方可通过此扩展点在 yml 之外再动态改写 SDK Builder 参数。
 */
public interface ExceptionAnalyzerCustomizer {

    void customize(ExceptionAnalyzerConfig.Builder builder, ExceptionAnalyzerProperties properties);
}
