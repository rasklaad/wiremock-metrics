package com.rasklaad.wiremock.metrics;

import com.github.tomakehurst.wiremock.admin.AdminTask;
import com.github.tomakehurst.wiremock.admin.Router;
import com.github.tomakehurst.wiremock.common.url.PathParams;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.extension.AdminApiExtension;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.stream.Collectors;

public class MetricsEndpointExtension implements AdminApiExtension {
    public static final String EXTENSION_NAME = "metrics-endpoint-extension";

    @Override
    public void contributeAdminApiRoutes(Router router) {
        router.add(RequestMethod.GET, "/prometheus-metrics", new PrometheusEndpointAdminTask());
    }

    @Override
    public String getName() {
        return EXTENSION_NAME;
    }

    private class PrometheusEndpointAdminTask implements AdminTask {

        private final PrometheusMeterRegistry prometheusMeterRegistry;

        private PrometheusEndpointAdminTask() {
            List<MeterRegistry> registries = Metrics.globalRegistry.getRegistries()
                .stream()
                .filter(registry -> registry instanceof PrometheusMeterRegistry)
                .collect(Collectors.toList());
            if (registries.size() != 1) {
                throw new IllegalStateException("Expected exactly one PrometheusMeterRegistry, found " + registries.size());
            }
            prometheusMeterRegistry = (PrometheusMeterRegistry) registries.get(0);
        }

        @Override
        public ResponseDefinition execute(Admin admin, ServeEvent event, PathParams pathParams) {
            return new ResponseDefinition(HttpURLConnection.HTTP_OK, prometheusMeterRegistry.scrape());
        }
    }
}
