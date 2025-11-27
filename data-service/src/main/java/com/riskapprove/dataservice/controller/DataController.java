package com.riskapprove.dataservice.controller;

import com.riskapprove.dataservice.dto.MLServiceResponse;
import com.riskapprove.dataservice.dto.StockPredictionRequest;
import com.riskapprove.dataservice.service.MLServiceClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/data")
@Tag(name = "Data Service", description = "Stock prediction and data retrieval API")
public class DataController {
    private static final Logger logger = LoggerFactory.getLogger(DataController.class);
    
    private final MLServiceClient mlServiceClient;

    public DataController(MLServiceClient mlServiceClient) {
        this.mlServiceClient = mlServiceClient;
    }

    @PostMapping("/predictions")
    @Operation(summary = "Get stock predictions", 
               description = "Fetches predictions for a list of stocks from the ML service")
    public ResponseEntity<MLServiceResponse> getPredictions(
            @Valid @RequestBody StockPredictionRequest request) {
        logger.info("Received prediction request for {} stocks", request.getStocks().size());
        
        try {
            MLServiceResponse response = mlServiceClient.getPredictions(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing prediction request: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "healthy", "service", "data-service"));
    }
}

