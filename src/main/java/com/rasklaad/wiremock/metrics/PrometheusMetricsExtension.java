package com.rasklaad.wiremock.metrics;

import com.github.tomakehurst.wiremock.common.Timing;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.extension.PostServeAction;
import com.github.tomakehurst.wiremock.http.LoggedResponse;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

public class PrometheusMetricsExtension extends PostServeAction {
    public static final String EXTENSION_NAME = "prometheus-metrics-extension";
    private final PrometheusMeterRegistry registry;

    public PrometheusMetricsExtension() {
        registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        Metrics.addRegistry(registry);
        new JvmThreadMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
    }

    @Override
    public void doGlobalAction(ServeEvent serveEvent, Admin admin) {
        super.doGlobalAction(serveEvent, admin);
        StubMapping mapping = serveEvent.getStubMapping();
        if (mapping != null) {
            Timing timing = serveEvent.getTiming();
            LoggedRequest request = serveEvent.getRequest();
            LoggedResponse response = serveEvent.getResponse();
            String urlPath = request.getUrl();
            String method = request.getMethod().getName();
            String status = String.valueOf(response.getStatus());

            DistributionSummary totalTimeSummary = DistributionSummary.builder("wiremock.request.totalTime")
                    .baseUnit("ms")
                    .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                    .description("Request time latency")
                    .tags("path", urlPath,"method", method, "status", status)
                    .register(Metrics.globalRegistry);

            DistributionSummary processingTimeSummary = DistributionSummary.builder("wiremock.request.processingTime")
                    .baseUnit("ms")
                    .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                    .description("Processing time latency")
                    .tags("path", urlPath,"method", method, "status", status)
                    .register(registry);


            DistributionSummary serveTimeSummary = DistributionSummary.builder("wiremock.request.serveTime")
                    .baseUnit("ms")
                    .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                    .description("Serve time latency")
                    .tags("path", urlPath,"method", method, "status", status)
                    .register(registry);

            DistributionSummary responseSendTime = DistributionSummary.builder("wiremock.request.responseSendTime")
                    .baseUnit("ms")
                    .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                    .description("Response send time latency")
                    .tags("path", urlPath,"method", method, "status", status)
                    .register(registry);

            totalTimeSummary.record(timing.getTotalTime());
            processingTimeSummary.record(timing.getProcessTime());
            serveTimeSummary.record(timing.getServeTime());
            responseSendTime.record(timing.getResponseSendTime());
        }
    }

    @Override
    public String getName() {
        return EXTENSION_NAME;
    }
}
