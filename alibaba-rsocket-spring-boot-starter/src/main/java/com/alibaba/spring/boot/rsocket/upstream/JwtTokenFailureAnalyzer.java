package com.alibaba.spring.boot.rsocket.upstream;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * JWT token failure analyzer
 *
 * @author leijuan
 */
public class JwtTokenFailureAnalyzer extends AbstractFailureAnalyzer<JwtTokenNotFoundException> {
    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, JwtTokenNotFoundException cause) {
        return new FailureAnalysis(getDescription(cause), getAction(cause), cause);
    }

    private String getDescription(JwtTokenNotFoundException ex) {
        return "rsocket.jwt-token not found in the application.properties";
    }

    private String getAction(JwtTokenNotFoundException ex) {
        return "Please contact the Ops or open RSocket Broker http://localhost:9998/ to generate one";
    }
}
