package com.example.jsonsender;

import com.example.jsonsender.TcpClient;
import com.example.jsoncommon.dto.*;
import com.example.jsonsender.service.MetricsSendService;
import com.example.jsoncommon.util.HostnameUtil;
import com.example.jsonsender.utils.IdUtils;
import com.example.jsonsender.utils.TimeUtils;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class Runner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(Runner.class);
    private final TcpClient tcpClient;
    private final MetricsSendService metricsSendService;
    private final com.example.jsonsender.config.AppConfig appConfig;

    public Runner(TcpClient tcpClient,
            MetricsSendService metricsSendService,
            com.example.jsonsender.config.AppConfig appConfig) {
        this.tcpClient = tcpClient;
        this.metricsSendService = metricsSendService;
        this.appConfig = appConfig;
    }

    @Override
    public void run(String... args) throws Exception {
        // Send UP notification
        UpJson upNotice = new UpJson(
                IdUtils.getId(),
                TimeUtils.getNow(appConfig.getTimezone()),
                appConfig.getAgentVersion(),
                HostnameUtil.getHostname());
        tcpClient.sendJson(appConfig.getDist().getHostname(), appConfig.getDist().getPort(), upNotice);

        while (true) {
            try {
                Metrics metrics = metricsSendService.collect();
                MetricsJson metricsJson = new MetricsJson(
                        IdUtils.getId(),
                        NoticeType.METRICS,
                        TimeUtils.getNow(appConfig.getTimezone()),
                        appConfig.getAgentVersion(),
                        HostnameUtil.getHostname(),
                        metrics);

                tcpClient.sendJson(appConfig.getDist().getHostname(), appConfig.getDist().getPort(), metricsJson);

                Thread.sleep(appConfig.getNoticeIntervalSec() * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Log error but continue loop
                logger.error("Error in metrics collection loop, retrying in {} seconds",
                        appConfig.getErrorRetryIntervalSec(), e);
                Thread.sleep(appConfig.getErrorRetryIntervalSec() * 1000L);
            }
        }
    }

    @PreDestroy
    public void onExit() {
        // Send DOWN notification
        DownJson downNotice = new DownJson(
                IdUtils.getId(),
                TimeUtils.getNow(appConfig.getTimezone()),
                appConfig.getAgentVersion(),
                HostnameUtil.getHostname());
        tcpClient.sendJsonDirectly(appConfig.getDist().getHostname(), appConfig.getDist().getPort(), downNotice);
    }
}
