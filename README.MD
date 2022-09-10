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
If you are running wiremock as [standalone](https://wiremock.org/docs/running-standalone/) process, you still can use this extension:
1. Download metrics extension standalone jar from [releases](https://github.com/rasklaad/wiremock-metrics/releases/)
2. Place it near your wiremock standalone jar
3. Run via command line: `java -cp './*' com.github.tomakehurst.wiremock.standalone.WireMockServerRunner --extensions com.rasklaad.wiremock.metrics.MetricsEndpointExtension,com.rasklaad.wiremock.metrics.PrometheusMetricsExtension` (both wiremock jar and extension jar need to be in current folder)


## Usage
Metrics are available on `/__admin/prometheus-metrics` endpoint. Configure your prometheus or any prometheus compatible scrapper to scrape this endpoint.
