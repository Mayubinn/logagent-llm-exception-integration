package com.logagent.sdk.spring;

/**
 * Spring Boot 场景下对异常分析 SDK 的统一门面。
 */
public interface ExceptionAnalyzerFacade {

    void analyze(Throwable throwable);

    void analyze(Throwable throwable, ExceptionAnalysisContext context);

    boolean isEnabled();

    void refresh();

    String getStats();
}
