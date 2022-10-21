package com.rasklaad.wiremock.metrics;

public class MetricsConfiguration {

    private boolean useMappingUrlPattern;
    private boolean useRequestUrl;
    private boolean registerNotMatchedRequests;

    private boolean ignoreQueryParams;

    private boolean registerAnyUrlMappingAsRequestUrl;

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

    static MetricsConfiguration defaultConfiguration() {
        return new MetricsConfiguration().useRequestUrl();
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
}
