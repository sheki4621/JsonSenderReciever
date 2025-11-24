package com.example.jsonsender;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.jsonsender.metrics.Metrics;
import com.example.jsonsender.metrics.MetricsJson;
import com.example.jsonsender.utils.collector.Collector;
import com.example.jsonsender.utils.notice.FinJson;
import com.example.jsonsender.utils.notice.InitJson;
import com.example.jsonsender.utils.notice.NoticeType;
import com.example.jsonsender.utils.IdUtils;
import com.example.jsonsender.utils.TimeUtils;

import jakarta.annotation.PreDestroy;

@Component
public class Runner implements CommandLineRunner {

    private final TcpClient tcpClient;
    private final Collector<Metrics> metricsCollector;
    private final com.example.jsonsender.config.AppConfig appConfig;

    public Runner(TcpClient tcpClient,
            Collector<Metrics> metricsCollector,
            com.example.jsonsender.config.AppConfig appConfig) {
        this.tcpClient = tcpClient;
        this.metricsCollector = metricsCollector;
        this.appConfig = appConfig;
    }

    @Override
    public void run(String... args) throws Exception {
        // Send INIT notification
        InitJson initNotice = new InitJson(
                IdUtils.getId(),
                TimeUtils.getNow(appConfig.getTimezone()),
                appConfig.getAgentVersion());
        tcpClient.sendJson(appConfig.getDistHostname(), appConfig.getDistPort(), initNotice);

        while (true) {
            try {
                Metrics metrics = metricsCollector.collect();
                MetricsJson metricsJson = new MetricsJson(
                        IdUtils.getId(),
                        NoticeType.METRICS,
                        TimeUtils.getNow(appConfig.getTimezone()),
                        appConfig.getAgentVersion(),
                        metrics);

                tcpClient.sendJson(appConfig.getDistHostname(), appConfig.getDistPort(), metricsJson);

                Thread.sleep(appConfig.getNoticeIntervalSec() * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Log error but continue loop
                e.printStackTrace();
                Thread.sleep(5000); // Wait a bit before retrying loop on error
            }
        }
    }

    @PreDestroy
    public void onExit() {
        // Send FIN notification
        FinJson finNotice = new FinJson(
                IdUtils.getId(),
                TimeUtils.getNow(appConfig.getTimezone()),
                appConfig.getAgentVersion());
        tcpClient.sendJsonDirectly(appConfig.getDistHostname(), appConfig.getDistPort(), finNotice);
    }
}
