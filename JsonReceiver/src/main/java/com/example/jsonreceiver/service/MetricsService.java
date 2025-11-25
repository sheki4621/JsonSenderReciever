package com.example.jsonreceiver.service;

import com.example.jsonreceiver.dto.MetricsJson;
import com.example.jsonreceiver.repository.CsvRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final CsvRepository csvRepository;

    public void processMetrics(MetricsJson metricsJson) {
        try {
            csvRepository.saveResourceInfo(metricsJson);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save metrics to CSV", e);
        }
    }
}
