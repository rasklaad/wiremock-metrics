# Wiremock metrics
Wiremock extension that expose jvm and wiremock requests metrics in prometheus format.

## Metrics
There are following metrics: 
* JVM metrics with `jvm_` prefix. For example `jvm_buffer_memory_used_bytes{id="direct",} 16384.0`
* System metrics with `system_` and `process_` prefixes. For example `system_cpu_usage 0.1` and `process_cpu_usage 0.3333333333333333`
* Wiremock requests metrics with `wiremock_` prefix. For example `wiremock_request_totalTime_ms{method="GET",path="/test",status="200",quantile="0.99",} 432.0`
### Wiremock requests metrics
* `wiremock_request_totalTime_ms`
* `wiremock_request_processingTime_ms`
* `wiremock_request_serveTime_ms`
* `wiremock_request_responseSendTime_ms`

## Installation
### Programmatically
Add dependency to your project
```xml
<dependency>
    <groupId>com.rasklaad</groupId>
    <artifactId>wiremock-metrics</artifactId>
    <version>1.0.0</version>
</dependency>
```
Add `MetricsEndpointExtension` and `PrometheusMetricsExtension` to your wiremock configuration, for example:
```java
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.rasklaad.wiremock.metrics.MetricsEndpointExtension;
import com.rasklaad.wiremock.metrics.PrometheusMetricsExtension;

public class Main {
    public static void main(String[] args) {
        WireMockServer server = new WireMockServer(
                WireMockConfiguration.options()
                        .port(8080)
                        .extensions(new PrometheusMetricsExtension(), new MetricsEndpointExtension())
        );
        server.start();
    }
}
```
### Standalone process
TODO

## Usage
Metrics are available on `/__admin/prometheus-metrics` endpoint. Configure your prometheus or any prometheus compatible scrapper to scrape this endpoint.
