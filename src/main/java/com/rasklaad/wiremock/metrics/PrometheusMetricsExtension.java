package com.rasklaad.wiremock.metrics;

import com.github.tomakehurst.wiremock.common.Timing;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.extension.PostServeAction;
import com.github.tomakehurst.wiremock.http.LoggedResponse;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmHeapPressureMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.net.URI;

public class PrometheusMetricsExtension extends PostServeAction {
    public static final String EXTENSION_NAME = "prometheus-metrics-extension";
    private final PrometheusMeterRegistry registry;
    private MetricsConfiguration configuration;

    public PrometheusMetricsExtension() {
        registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        Metrics.addRegistry(registry);
        new JvmThreadMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new JvmInfoMetrics().bindTo(registry);
        new JvmHeapPressureMetrics().bindTo(registry);
        new ClassLoaderMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new UptimeMetrics().bindTo(registry);
        configuration = MetricsConfiguration.defaultConfiguration();
    }

    public PrometheusMetricsExtension(MetricsConfiguration configuration) {
        this();
        configuration.validate();
        this.configuration = configuration;
    }

    @Override
    public void doGlobalAction(ServeEvent serveEvent, Admin admin) {
        super.doGlobalAction(serveEvent, admin);
        Timing timing = serveEvent.getTiming();
        LoggedRequest request = serveEvent.getRequest();
        LoggedResponse response = serveEvent.getResponse();

        String requestUrlPath = request.getUrl();
        String method = request.getMethod().getName();
        String status = String.valueOf(response.getStatus());

        if (!serveEvent.getWasMatched()) {
            if (configuration.shouldRegisterNotMatchedRequests()) {
                registerByUrlPath(timing, requestUrlPath, method, status);
            }
            return;
        }

        if (configuration.shouldUseRequestUrl()) {
            registerByUrlPath(timing, requestUrlPath, method, status);
            return;
        }

        UrlPattern urlPattern = serveEvent.getStubMapping().getRequest().getUrlMatcher();
        if (configuration.shouldUseMappingUrlPattern()) {
            if (urlPattern == UrlPattern.ANY && configuration.shouldRegisterAnyUrlMappingAsRequestUrl()) {
                registerByUrlPath(timing, requestUrlPath, method, status);
            } else {
                registerByUrlMapping(timing, urlPattern, method, status);
            }
        }

    }

    private void registerByUrlMapping(Timing timing, UrlPattern urlPattern, String method, String status) {
        String mappingUrlPath = urlPattern.getExpected();
        if (!(urlPattern.getPattern() instanceof RegexPattern) || !(urlPattern.getPattern() instanceof AnythingPattern)) {
            if (configuration.shouldIgnoreQueryParams()) {
                try {
                    URI uri = URI.create(mappingUrlPath);
                    mappingUrlPath = uri.getPath();
                } catch (IllegalArgumentException e) {
                    // can't ignore query param; fallback to mapping url
                }
            }
        }
        register(timing, mappingUrlPath, method, status);
    }

    private void registerByUrlPath(Timing timing, String urlPath, String method, String statusCode) {
        String path = urlPath;
        if (configuration.shouldIgnoreQueryParams()) {
            try {
                URI uri = URI.create(path);
                path = uri.getPath();
            } catch (IllegalArgumentException e) {
                // can't ignore query param; fallback to full url
            }
        }
        register(timing, path, method, statusCode);
    }

    private void register(Timing timing, String path, String method, String statusCode) {
        if (configuration.isTotalTimeMetricEnabled()) {
            DistributionSummary totalTimeSummary = DistributionSummary.builder("wiremock.request.totalTime")
                .baseUnit("ms")
                .publishPercentileHistogram()
                .maximumExpectedValue(configuration.getMaximumMetricExpectedValue())
                .description("Request time latency")
                .tags("path", path, "method", method, "status", statusCode)
                .register(Metrics.globalRegistry);
            totalTimeSummary.record(timing.getTotalTime());
        }

        if (configuration.isProcessingTimeMetricEnabled()) {
            DistributionSummary processingTimeSummary = DistributionSummary.builder("wiremock.request.processingTime")
                .baseUnit("ms")
                .publishPercentileHistogram()
                .maximumExpectedValue(configuration.getMaximumMetricExpectedValue())
                .description("Processing time latency")
                .tags("path", path, "method", method, "status", statusCode)
                .register(registry);
            processingTimeSummary.record(timing.getProcessTime());
        }

        if (configuration.isServeTimeMetricEnabled()) {
            DistributionSummary serveTimeSummary = DistributionSummary.builder("wiremock.request.serveTime")
                .baseUnit("ms")
                .publishPercentileHistogram()
                .maximumExpectedValue(configuration.getMaximumMetricExpectedValue())
                .description("Serve time latency")
                .tags("path", path, "method", method, "status", statusCode)
                .register(registry);
            serveTimeSummary.record(timing.getServeTime());
        }

        if (configuration.isResponseSendTimeEnabled()) {
            DistributionSummary responseSendTime = DistributionSummary.builder("wiremock.request.responseSendTime")
                .baseUnit("ms")
                .publishPercentileHistogram()
                .maximumExpectedValue(configuration.getMaximumMetricExpectedValue())
                .description("Response send time latency")
                .tags("path", path, "method", method, "status", statusCode)
                .register(registry);

            responseSendTime.record(timing.getResponseSendTime());
        }
    }

    @Override
    public String getName() {
        return EXTENSION_NAME;
    }

    public static MetricsConfiguration options() {
        return new MetricsConfiguration();
    }
}
