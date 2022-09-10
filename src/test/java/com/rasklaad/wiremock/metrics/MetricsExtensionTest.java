package com.rasklaad.wiremock.metrics;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class MetricsExtensionTest {
    private static final OkHttpClient client = new OkHttpClient.Builder().build();
    private static WireMockServer server;
    @BeforeAll
    static void createServer() {
        WireMockConfiguration config = new WireMockConfiguration();
        config.extensions(new PrometheusMetricsExtension(), new MetricsEndpointExtension());
        server = new WireMockServer(config);
        server.start();
    }

    @AfterAll
    static void close() {
        server.stop();
    }

    @Test
    void wiremockShouldRegisterMetrics() throws IOException, InterruptedException {
        StubMapping mapping = WireMock.get(WireMock.urlPathEqualTo("/test"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withFixedDelay(200)
                        .withBody("test")).build();
        server.addStubMapping(mapping);

        Request request = new Request.Builder()
                .url(server.baseUrl() + "/test")
                .build();

        client.newCall(request).execute();
        client.newCall(request).execute();
        Thread.sleep(1_000L);

        Response res = client.newCall(new Request.Builder()
                .url(server.baseUrl() + "/__admin/prometheus-metrics")
                .build()).execute();

        String responseBody = res.body().string();
        Assertions.assertTrue(responseBody.contains("wiremock_request_totalTime_ms_count{method=\"GET\",path=\"/test\",status=\"200\",} 2.0"));

    }
}
