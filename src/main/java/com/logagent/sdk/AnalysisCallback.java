package com.logagent.sdk;

/**
 * 异常分析结果回调。
 */
public interface AnalysisCallback {

    /**
     * 分析成功。
     *
     * @param exception 原始异常
     * @param report    LLM 返回报告
     */
    void onSuccess(Throwable exception, String report);

    /**
     * 分析失败。
     *
     * @param exception 原始异常
     * @param error     失败原因
     */
    void onFailure(Throwable exception, Throwable error);
}
