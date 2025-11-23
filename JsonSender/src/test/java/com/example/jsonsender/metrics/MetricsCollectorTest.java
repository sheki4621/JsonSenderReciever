package com.example.jsonsender.metrics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MetricsCollectorTest {

    @Test
    void testCollectWithNormalValues() {
        MetricsCollector collector = new MetricsCollector() {
            @Override
            protected Double getCpuUsage() {
                return 50.0;
            }

            @Override
            protected Double getMemoryUsage() {
                return 60.0;
            }
        };

        Metrics metrics = collector.collect();

        assertNotNull(metrics);
        assertEquals(50.0, metrics.getCpuUsage());
        assertEquals(60.0, metrics.getMemoryUsage());
    }

    @Test
    void testCollectWithNullValues() {
        MetricsCollector collector = new MetricsCollector() {
            @Override
            protected Double getCpuUsage() {
                return null;
            }

            @Override
            protected Double getMemoryUsage() {
                return null;
            }
        };

        Metrics metrics = collector.collect();

        assertNotNull(metrics);
        assertNull(metrics.getCpuUsage());
        assertNull(metrics.getMemoryUsage());
    }
}
