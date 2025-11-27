package com.riskapprove.dataservice.service;

import com.riskapprove.dataservice.dto.MLServiceResponse;
import com.riskapprove.dataservice.dto.StockPredictionRequest;
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
public class MLServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(MLServiceClient.class);
    
    private final RestTemplate restTemplate;
    private final String mlServiceUrl;

    public MLServiceClient(
            RestTemplate restTemplate,
            @Value("${ml.service.url}") String mlServiceUrl) {
        this.restTemplate = restTemplate;
        this.mlServiceUrl = mlServiceUrl;
    }

    public MLServiceResponse getPredictions(StockPredictionRequest request) {
        try {
            logger.info("Calling ML service for stocks: {}", request.getStocks());
            
            String url = mlServiceUrl + "/predict";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = new HashMap<>();
            body.put("stocks", request.getStocks());
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<MLServiceResponse> response = restTemplate.postForEntity(
                    url, entity, MLServiceResponse.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("Successfully received predictions from ML service");
                return response.getBody();
            } else {
                logger.error("ML service returned error: {}", response.getStatusCode());
                throw new RuntimeException("ML service returned error: " + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error calling ML service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get predictions from ML service", e);
        }
    }
}

