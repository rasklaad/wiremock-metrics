package com.rasklaad.wiremock.metrics;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MetricsExtensionTest {
    private static final OkHttpClient client = new OkHttpClient.Builder().build();
    private WireMockServer server;


    private WireMockServer startServer(MetricsConfiguration configuration) {
        WireMockConfiguration config = new WireMockConfiguration()
            .dynamicPort()
            .extensions(new PrometheusMetricsExtension(configuration), new MetricsEndpointExtension());
        WireMockServer server = new WireMockServer(config);
        server.start();
        this.server = server;
        // Metrics.add add values from previous tests
        Metrics.globalRegistry.getRegistries().forEach(MeterRegistry::clear);

        return server;
    }

    private WireMockServer startServer() {
        WireMockConfiguration config = new WireMockConfiguration()
            .dynamicPort()
            .extensions(new PrometheusMetricsExtension(), new MetricsEndpointExtension());
        WireMockServer server = new WireMockServer(config);
        server.start();
        this.server = server;
        // Metrics.add add values from previous tests
        Metrics.globalRegistry.getRegistries().forEach(MeterRegistry::clear);

        return server;
    }

    @AfterEach
    public void stopServers()  {
        if (server != null) {
            server.stop();
        }
        // Metrics.add add values from previous tests
        Metrics.globalRegistry.getRegistries().forEach(registry -> {
            registry.clear();
            registry.close();
            Metrics.removeRegistry(registry);
        });
    }

    private List<String> scrape(WireMockServer server) throws IOException {
        Response response = client.newCall(new Request.Builder()
            .url(server.baseUrl() + "/__admin/prometheus-metrics")
            .build()).execute();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()))) {
            return reader.lines().collect(Collectors.toList());
        }
    }

    private void httpCall(WireMockServer server, String endpoint) throws IOException {
        Request request = new Request.Builder()
            .url(server.baseUrl() + endpoint)
            .build();
        client.newCall(request).execute().close();
    }

    private StubMapping createDefaultMapping() {
        return WireMock.get(WireMock.urlPathEqualTo("/test"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withBody("test")).build();
    }
    @Test
    void wiremockShouldRegisterMetrics() throws IOException, InterruptedException {
        WireMockServer server = startServer();

        server.addStubMapping(WireMock.get(WireMock.urlPathEqualTo("/simple-test"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withBody("test")).build());

        httpCall(server, "/simple-test");
        httpCall(server, "/simple-test");
        Thread.sleep(1_000L);

        List<String> metrics = scrape(server);
        Assertions.assertThat(metrics).contains("wiremock_request_totalTime_ms_count{method=\"GET\",path=\"/simple-test\",status=\"200\",} 2.0");
    }

    @Test
    void shouldThrowExceptionWhenBothMappingAndUrlOptionsAreAbsent() {
        Assertions.assertThatThrownBy(() -> startServer(new MetricsConfiguration()))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldThrowExceptionWhenBothMappingAndUrlOptionsArePresent() {
        Assertions.assertThatThrownBy(() ->
                startServer(new MetricsConfiguration().useMappingUrlPattern().useRequestUrl())
            ).isInstanceOf(IllegalStateException.class);
    }


    private static Stream<Arguments> urlPathProvider() {
        return Stream.of(
            Arguments.of(WireMock.urlPathEqualTo("/some-test"), "/some-test"),
            Arguments.of(WireMock.urlPathMatching("/some-t.+"), "/some-t.+"),
            Arguments.of(WireMock.urlEqualTo("/some-test?withQueryParam=true"), "/some-test?withQueryParam=true"),
            Arguments.of(WireMock.urlMatching("/some-test.+"), "/some-test.+")
        );
    }
    @ParameterizedTest
    @MethodSource("urlPathProvider")
    void shouldRegisterMetricsByMapping(UrlPattern pattern, String expectedMetricPath) throws IOException, InterruptedException {
        WireMockServer server = startServer(
            new MetricsConfiguration().useMappingUrlPattern()
        );

        StubMapping mapping = WireMock.get(pattern)
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withBody("test")).build();
        server.addStubMapping(mapping);

        httpCall(server, "/some-test?withQueryParam=true");
        Thread.sleep(1000L);

        List<String> metrics = scrape(server);
        Assertions.assertThat(metrics).contains("wiremock_request_totalTime_ms_count{method=\"GET\",path=\"" + expectedMetricPath + "\",status=\"200\",} 1.0");
    }

    @Test
    void shouldIgnoreQueryParamUsingMappingUrl() throws IOException, InterruptedException {
        WireMockServer server = startServer(
            new MetricsConfiguration()
                .useMappingUrlPattern()
                .ignoreQueryParams()
        );
        server.addStubMapping(WireMock.get(WireMock.urlPathEqualTo("/test-something-with-param"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withBody("test")).build());

        httpCall(server, "/test-something-with-param?withQueryParam=true");
        Thread.sleep(1000L);

        List<String> metrics = scrape(server);
        Assertions.assertThat(metrics).contains("wiremock_request_totalTime_ms_count{method=\"GET\",path=\"/test-something-with-param\",status=\"200\",} 1.0");
    }
    @Test
    void shouldIgnoreQueryParamUsingRequestUrl() throws IOException, InterruptedException {
        WireMockServer server = startServer(
            new MetricsConfiguration()
                .useRequestUrl()
                .ignoreQueryParams()
        );
        server.addStubMapping(createDefaultMapping());

        httpCall(server, "/test?withQueryParam=true");
        Thread.sleep(1000L);

        List<String> metrics = scrape(server);
        Assertions.assertThat(metrics).contains("wiremock_request_totalTime_ms_count{method=\"GET\",path=\"/test\",status=\"200\",} 1.0");
    }

    @Test
    void shouldRegisterNonMatchedRequestsByRequestUrl() throws IOException, InterruptedException {
        WireMockServer server = startServer(
            new MetricsConfiguration()
                .useMappingUrlPattern()
                .registerNotMatchedRequests()
        );

        httpCall(server, "/test?withQueryParam=true");
        Thread.sleep(1000L);

        List<String> metrics = scrape(server);
        Assertions.assertThat(metrics).contains("wiremock_request_totalTime_ms_count{method=\"GET\",path=\"/test?withQueryParam=true\",status=\"404\",} 1.0");
    }

    @Test
    void shouldNotRegisterNonMatchedRequests() throws IOException, InterruptedException {
        WireMockServer server = startServer(
            new MetricsConfiguration()
                .useRequestUrl()
        );

        httpCall(server, "/some-url-that-should-not-be-matched");
        Thread.sleep(1000L);

        List<String> metrics = scrape(server);
        List<String> filtered = metrics.stream().filter(s -> s.startsWith("wiremock")).collect(Collectors.toList());
        Assertions.assertThat(filtered).isEmpty();

    }

    @Test
    void shouldRegisterAnyUrlMappingAsRequestUrl() throws IOException, InterruptedException {
        WireMockServer server = startServer(
            new MetricsConfiguration()
                .useMappingUrlPattern()
                .registerAnyUrlMappingAsRequestUrl()
        );

        server.addStubMapping(WireMock.any(WireMock.anyUrl())
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withBody("test")).build());

        httpCall(server, "/any-url?param=true");
        Thread.sleep(1000L);

        List<String> metrics = scrape(server);
        Assertions.assertThat(metrics).contains("wiremock_request_totalTime_ms_count{method=\"GET\",path=\"/any-url?param=true\",status=\"200\",} 1.0");
    }

}
