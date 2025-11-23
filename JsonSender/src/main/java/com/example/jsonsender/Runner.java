package com.example.jsonsender;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.jsonsender.metrics.Metrics;
import com.example.jsonsender.metrics.MetricsJson;
import com.example.jsonsender.utils.collector.Collector;
import com.example.jsonsender.utils.notice.NoticeType;
import com.example.jsonsender.utils.IdUtils;
import com.example.jsonsender.utils.TimeUtils;

@Component
public class Runner implements CommandLineRunner {

    private final TcpClient tcpClient;
    private final Collector<Metrics> metricsCollector;

    public Runner(TcpClient tcpClient,
            Collector<Metrics> metricsCollector) {
        this.tcpClient = tcpClient;
        this.metricsCollector = metricsCollector;
    }

    @Override
    public void run(String... args) throws Exception {

        Metrics metrics = metricsCollector.collect();
        MetricsJson metricsJson = new MetricsJson(
                IdUtils.getId(),
                NoticeType.METRICS,
                TimeUtils.getNow("Asia/Tokyo"),
                "1.0",
                metrics);
        tcpClient.sendJson("localhost", 9999, metricsJson);

        Metrics metrics2 = metricsCollector.collect();
        MetricsJson metricsJson2 = new MetricsJson(
                IdUtils.getId(),
                NoticeType.METRICS,
                TimeUtils.getNow("Asia/Tokyo"),
                "1.0",
                metrics2);
        tcpClient.sendJson("localhost", 9999, metricsJson2);
    }
}
