package com.logagent.sdk.spring;

import com.logagent.sdk.ExceptionAnalyzer;
import com.logagent.sdk.ExceptionAnalyzerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 读取 exception.llm.* 配置，并按需重建 ExceptionAnalyzer。
 * 每次分析前都会从 Spring Environment 重新绑定参数，方便配置中心动态刷新。
 */
public class DefaultExceptionAnalyzerFacade implements ExceptionAnalyzerFacade, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(DefaultExceptionAnalyzerFacade.class);
    private static final String PREFIX = "exception.llm";

    private final Environment environment;
    private final List<ExceptionAnalyzerCustomizer> customizers;
    private final AtomicReference<Holder> holderRef = new AtomicReference<Holder>(Holder.disabled());
    private final Object monitor = new Object();

    public DefaultExceptionAnalyzerFacade(Environment environment, List<ExceptionAnalyzerCustomizer> customizers) {
        this.environment = environment;
        List<ExceptionAnalyzerCustomizer> ordered = new ArrayList<ExceptionAnalyzerCustomizer>(
                customizers == null ? Collections.<ExceptionAnalyzerCustomizer>emptyList() : customizers);
        AnnotationAwareOrderComparator.sort(ordered);
        this.customizers = Collections.unmodifiableList(ordered);
    }

    @Override
    public void analyze(Throwable throwable) {
        analyze(throwable, null);
    }

    @Override
    public void analyze(Throwable throwable, ExceptionAnalysisContext context) {
        if (throwable == null) {
            return;
        }
        Snapshot snapshot = currentSnapshot(false);
        if (!snapshot.enabled || snapshot.analyzer == null) {
            return;
        }
        String contextText = context == null ? null : context.toPromptText(snapshot.properties);
        snapshot.analyzer.analyze(throwable, contextText);
    }

    @Override
    public boolean isEnabled() {
        return currentSnapshot(false).enabled;
    }

    @Override
    public void refresh() {
        currentSnapshot(true);
    }

    @Override
    public String getStats() {
        Snapshot snapshot = currentSnapshot(false);
        if (!snapshot.enabled || snapshot.analyzer == null) {
            return "disabled";
        }
        return snapshot.analyzer.getStats();
    }

    @Override
    public void destroy() {
        Holder holder = holderRef.getAndSet(Holder.disabled());
        if (holder.analyzer != null) {
            holder.analyzer.shutdown();
        }
    }

    private Snapshot currentSnapshot(boolean forceRefresh) {
        ExceptionAnalyzerProperties properties = bindProperties();
        boolean enabled = properties != null && properties.isEnabled();
        ExceptionAnalyzerConfig targetConfig = enabled ? buildConfig(properties) : null;
        Holder current = holderRef.get();
        if (!forceRefresh && current.matches(enabled, targetConfig)) {
            return new Snapshot(enabled, properties, current.analyzer);
        }
        synchronized (monitor) {
            current = holderRef.get();
            if (!forceRefresh && current.matches(enabled, targetConfig)) {
                return new Snapshot(enabled, properties, current.analyzer);
            }
            ExceptionAnalyzer newAnalyzer = enabled ? ExceptionAnalyzer.create(targetConfig) : null;
            Holder next = new Holder(enabled, targetConfig, newAnalyzer);
            holderRef.set(next);
            if (current.analyzer != null) {
                current.analyzer.shutdown();
            }
            log.info("[ExceptionAnalyzerFacade] enabled={}, config={}", enabled, targetConfig);
            return new Snapshot(enabled, properties, newAnalyzer);
        }
    }

    private ExceptionAnalyzerProperties bindProperties() {
        Binder binder = Binder.get(environment);
        BindResult<ExceptionAnalyzerProperties> result = binder.bind(PREFIX, ExceptionAnalyzerProperties.class);
        return result.orElseGet(ExceptionAnalyzerProperties::new);
    }

    private ExceptionAnalyzerConfig buildConfig(ExceptionAnalyzerProperties properties) {
        ExceptionAnalyzerConfig.Builder builder = properties.toBuilder();
        for (ExceptionAnalyzerCustomizer customizer : customizers) {
            customizer.customize(builder, properties);
        }
        return builder.build();
    }

    private static final class Snapshot {
        private final boolean enabled;
        private final ExceptionAnalyzerProperties properties;
        private final ExceptionAnalyzer analyzer;

        private Snapshot(boolean enabled, ExceptionAnalyzerProperties properties, ExceptionAnalyzer analyzer) {
            this.enabled = enabled;
            this.properties = properties;
            this.analyzer = analyzer;
        }
    }

    private static final class Holder {
        private final boolean enabled;
        private final ExceptionAnalyzerConfig config;
        private final ExceptionAnalyzer analyzer;

        private Holder(boolean enabled, ExceptionAnalyzerConfig config, ExceptionAnalyzer analyzer) {
            this.enabled = enabled;
            this.config = config;
            this.analyzer = analyzer;
        }

        private static Holder disabled() {
            return new Holder(false, null, null);
        }

        private boolean matches(boolean targetEnabled, ExceptionAnalyzerConfig targetConfig) {
            if (enabled != targetEnabled) {
                return false;
            }
            if (!enabled) {
                return true;
            }
            return config != null && config.equals(targetConfig);
        }
    }
}
