package com.rasklaad.wiremock.metrics;

public class MetricsConfiguration {

    private boolean useMappingUrlPattern;
    private boolean useRequestUrl;
    private boolean registerNotMatchedRequests;
    private boolean ignoreQueryParams;
    private boolean registerAnyUrlMappingAsRequestUrl;
    private boolean totalTimeMetricEnabled = true;
    private boolean processingTimeMetricEnabled = true;
    private boolean serveTimeMetricEnabled = true;
    private boolean responseSendTimeEnabled = true;

    private Double maximumMetricExpectedValue;

    MetricsConfiguration() {

    }
    public MetricsConfiguration useRequestUrl() {
        useRequestUrl = true;
        return this;
    }

    public MetricsConfiguration useMappingUrlPattern() {
        useMappingUrlPattern = true;
        return this;
    }

    public MetricsConfiguration registerNotMatchedRequests() {
        registerNotMatchedRequests = true;
        return this;
    }

    public MetricsConfiguration registerAnyUrlMappingAsRequestUrl() {
        registerAnyUrlMappingAsRequestUrl = true;
        return this;
    }

    public MetricsConfiguration ignoreQueryParams() {
        ignoreQueryParams = true;
        return this;
    }

    public MetricsConfiguration totalTimeMetricEnabled(boolean enabled) {
        totalTimeMetricEnabled = enabled;
        return this;
    }

    public MetricsConfiguration processingTimeMetricEnabled(boolean enabled) {
        processingTimeMetricEnabled = enabled;
        return this;
    }

    public MetricsConfiguration serveTimeMetricEnabled(boolean enabled) {
        serveTimeMetricEnabled = enabled;
        return this;
    }

    public MetricsConfiguration responseSendTimeEnabled(boolean enabled) {
        responseSendTimeEnabled = enabled;
        return this;
    }

    public MetricsConfiguration maximumMetricExpectedValue(Double maximumMetricExpectedValue) {
        this.maximumMetricExpectedValue = maximumMetricExpectedValue;
        return this;
    }

    static MetricsConfiguration defaultConfiguration() {
        return new MetricsConfiguration()
            .totalTimeMetricEnabled(true)
            .processingTimeMetricEnabled(true)
            .serveTimeMetricEnabled(true)
            .responseSendTimeEnabled(true)
            .useRequestUrl();
    }

    void validate() {
        if (useMappingUrlPattern && useRequestUrl) {
            throw new IllegalStateException("You can't use both url path and url pattern");
        }
        if (!useMappingUrlPattern && !useRequestUrl) {
            throw new IllegalStateException("You must use either url path or url pattern");
        }
    }
    boolean shouldUseMappingUrlPattern() {
        return useMappingUrlPattern;
    }

    boolean shouldUseRequestUrl() {
        return useRequestUrl;
    }

    boolean shouldRegisterNotMatchedRequests() {
        return registerNotMatchedRequests;
    }

    boolean shouldRegisterAnyUrlMappingAsRequestUrl() {
        return registerAnyUrlMappingAsRequestUrl;
    }

    boolean shouldIgnoreQueryParams() {
        return ignoreQueryParams;
    }

    boolean isTotalTimeMetricEnabled() {
        return totalTimeMetricEnabled;
    }

    boolean isProcessingTimeMetricEnabled() {
        return processingTimeMetricEnabled;
    }

    boolean isServeTimeMetricEnabled() {
        return serveTimeMetricEnabled;
    }

    boolean isResponseSendTimeEnabled() {
        return responseSendTimeEnabled;
    }

    Double getMaximumMetricExpectedValue() {
        return maximumMetricExpectedValue;
    }
}
