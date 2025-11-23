package com.example.jsonsender;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.jsonsender.metrics.Metrics;
import com.example.jsonsender.metrics.MetricsJson;
import com.example.jsonsender.notice.NoticeType;

@Component
public class Runner implements CommandLineRunner {

    private final TcpClient tcpClient;

    public Runner(TcpClient tcpClient) {
        this.tcpClient = tcpClient;
    }

    @Override
    public void run(String... args) throws Exception {
        // Wait a bit for the server to start if running simultaneously
        Thread.sleep(2000);

        Metrics metrics = new Metrics(23.4, 34.5);
        MetricsJson metricsJson = new MetricsJson(com.example.jsonsender.utils.IdUtils.getId(), NoticeType.METRICS,
                com.example.jsonsender.utils.TimeUtils.getNow("Asia/Tokyo"), "1.0", metrics);
        tcpClient.sendJson("localhost", 9999, metricsJson);

        Metrics metrics2 = new Metrics(null, null);
        MetricsJson metricsJson2 = new MetricsJson(com.example.jsonsender.utils.IdUtils.getId(), NoticeType.METRICS,
                com.example.jsonsender.utils.TimeUtils.getNow("Asia/Tokyo"), "1.0", metrics2);
        tcpClient.sendJson("localhost", 9999, metricsJson2);
    }
}
