package com.logagent.sdk.spring;

import com.logagent.sdk.ExceptionAnalyzer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Boot 自动装配。
 */
@Configuration
@ConditionalOnClass(ExceptionAnalyzer.class)
@EnableConfigurationProperties(ExceptionAnalyzerProperties.class)
public class ExceptionAnalyzerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ExceptionAnalyzerFacade.class)
    public ExceptionAnalyzerFacade exceptionAnalyzerFacade(Environment environment,
                                                           ObjectProvider<ExceptionAnalyzerCustomizer> customizers) {
        List<ExceptionAnalyzerCustomizer> ordered = customizers == null
                ? Collections.<ExceptionAnalyzerCustomizer>emptyList()
                : customizers.orderedStream().collect(Collectors.<ExceptionAnalyzerCustomizer>toList());
        return new DefaultExceptionAnalyzerFacade(environment, ordered);
    }
}
