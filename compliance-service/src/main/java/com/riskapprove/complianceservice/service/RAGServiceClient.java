package com.riskapprove.complianceservice.service;

import com.riskapprove.complianceservice.dto.ComplianceCheckRequest;
import com.riskapprove.complianceservice.dto.ComplianceCheckResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class RAGServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(RAGServiceClient.class);
    
    private final RestTemplate restTemplate;
    private final String ragServiceUrl;

    public RAGServiceClient(
            RestTemplate restTemplate,
            @Value("${rag.service.url}") String ragServiceUrl) {
        this.restTemplate = restTemplate;
        this.ragServiceUrl = ragServiceUrl;
    }

    public ComplianceCheckResponse checkCompliance(ComplianceCheckRequest request) {
        try {
            logger.info("Calling RAG service for compliance check");
            
            String url = ragServiceUrl + "/compliance/check";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = new HashMap<>();
            body.put("portfolio", request.getPortfolio());
            body.put("riskProfile", request.getRiskProfile());
            
            // Convert risk metrics to simple map
            Map<String, Map<String, Double>> riskMetricsMap = new HashMap<>();
            request.getRiskMetrics().forEach((symbol, metrics) -> {
                Map<String, Double> metricsMap = new HashMap<>();
                if (metrics.getRiskScore() != null) {
                    metricsMap.put("riskScore", metrics.getRiskScore());
                }
                if (metrics.getVolatility() != null) {
                    metricsMap.put("volatility", metrics.getVolatility());
                }
                riskMetricsMap.put(symbol, metricsMap);
            });
            body.put("riskMetrics", riskMetricsMap);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<ComplianceCheckResponse> response = restTemplate.postForEntity(
                    url, entity, ComplianceCheckResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("Successfully received compliance check from RAG service");
                return response.getBody();
            } else {
                logger.error("RAG service returned error: {}", response.getStatusCode());
                // Return empty response on error
                ComplianceCheckResponse emptyResponse = new ComplianceCheckResponse();
                emptyResponse.setCompliant(true);
                return emptyResponse;
            }
        } catch (Exception e) {
            logger.error("Error calling RAG service: {}", e.getMessage(), e);
            // Return empty response on error
            ComplianceCheckResponse emptyResponse = new ComplianceCheckResponse();
            emptyResponse.setCompliant(true);
            return emptyResponse;
        }
    }
}

