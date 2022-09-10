package com.rasklaad.wiremock.metrics;

import com.github.tomakehurst.wiremock.admin.AdminTask;
import com.github.tomakehurst.wiremock.admin.Router;
import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.extension.AdminApiExtension;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.net.HttpURLConnection;

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

    private final static class PrometheusEndpointAdminTask implements AdminTask {

        private final PrometheusMeterRegistry prometheusMeterRegistry;

        private PrometheusEndpointAdminTask() {
            prometheusMeterRegistry = (PrometheusMeterRegistry) Metrics.globalRegistry.getRegistries()
                .stream()
                .filter(registry -> registry instanceof PrometheusMeterRegistry)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("PrometheusMeterRegistry not found," +
                    " you should define com.rasklaad.wiremock.metrics.PrometheusMetricsExtension" +
                    " in your wiremock extensions"));
        }

        @Override
        public ResponseDefinition execute(Admin admin, Request request, PathParams pathParams) {
            return new ResponseDefinition(HttpURLConnection.HTTP_OK, prometheusMeterRegistry.scrape());
        }
    }
}
