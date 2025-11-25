package com.example.jsonreceiver;

import com.example.jsonreceiver.dto.MetricsJson;
import com.example.jsonreceiver.service.MetricsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@org.springframework.test.context.TestPropertySource(properties = "tcp.server.port=0")
class TcpServerTest {

    @MockBean
    private MetricsService metricsService;

    @Autowired
    private TcpServer tcpServer;

    @Test
    void shouldProcessMetricsJson() throws Exception {
        // Wait for server to start and bind to a port
        await().atMost(5, TimeUnit.SECONDS).until(() -> tcpServer.getPort() > 0);
        int port = tcpServer.getPort();

        try (Socket socket = new Socket("localhost", port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String json = "{\"Id\":\"123e4567-e89b-12d3-a456-426614174000\",\"NoticeType\":\"METRICS\",\"timestamp\":\"2023-10-27T10:00:00Z\",\"AgentVersion\":\"1.0.0\",\"InstanceName\":\"test-instance\",\"Metrics\":{\"CpuUsage\":15.5,\"MemoryUsage\":45.2}}";
            out.println(json);
        }

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(metricsService).processMetrics(any(MetricsJson.class)));
    }
}
