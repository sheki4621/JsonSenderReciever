package com.example.jsonsender;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.jsonsender.metrics.Metrics;
import com.example.jsonsender.metrics.MetricsJson;
import com.example.jsonsender.utils.notice.NoticeType;

@Component
public class Runner implements CommandLineRunner {

    private final TcpClient tcpClient;
    private final com.example.jsonsender.utils.collector.Collector<com.example.jsonsender.metrics.Metrics> metricsCollector;

    public Runner(TcpClient tcpClient,
            com.example.jsonsender.utils.collector.Collector<com.example.jsonsender.metrics.Metrics> metricsCollector) {
        this.tcpClient = tcpClient;
        this.metricsCollector = metricsCollector;
    }

    @Override
    public void run(String... args) throws Exception {
        // Wait a bit for the server to start if running simultaneously
        Thread.sleep(2000);

        com.example.jsonsender.metrics.Metrics metrics = metricsCollector.collect();
        com.example.jsonsender.metrics.MetricsJson metricsJson = new com.example.jsonsender.metrics.MetricsJson(
                com.example.jsonsender.utils.IdUtils.getId(),
                NoticeType.METRICS,
                com.example.jsonsender.utils.TimeUtils.getNow("Asia/Tokyo"),
                "1.0",
                metrics);
        tcpClient.sendJson("localhost", 9999, metricsJson);

        com.example.jsonsender.metrics.Metrics metrics2 = metricsCollector.collect();
        com.example.jsonsender.metrics.MetricsJson metricsJson2 = new com.example.jsonsender.metrics.MetricsJson(
                com.example.jsonsender.utils.IdUtils.getId(),
                NoticeType.METRICS,
                com.example.jsonsender.utils.TimeUtils.getNow("Asia/Tokyo"),
                "1.0",
                metrics2);
        tcpClient.sendJson("localhost", 9999, metricsJson2);
    }
}
