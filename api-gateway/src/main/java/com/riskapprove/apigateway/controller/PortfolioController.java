package com.riskapprove.apigateway.controller;

import com.riskapprove.apigateway.dto.PortfolioRequest;
import com.riskapprove.apigateway.dto.PortfolioResponse;
import com.riskapprove.apigateway.service.PortfolioOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Portfolio API", description = "Main API for portfolio generation and analysis")
public class PortfolioController {
    private static final Logger logger = LoggerFactory.getLogger(PortfolioController.class);
    
    private final PortfolioOrchestrationService orchestrationService;

    public PortfolioController(PortfolioOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping("/portfolio/generate")
    @Operation(summary = "Generate optimized portfolio", 
               description = "Generates a complete portfolio with predictions, allocation, compliance check, and AI explanation")
    public ResponseEntity<PortfolioResponse> generatePortfolio(
            @Valid @RequestBody PortfolioRequest request) {
        logger.info("Received portfolio generation request");
        
        try {
            PortfolioResponse response = orchestrationService.generatePortfolio(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error generating portfolio: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "healthy", "service", "api-gateway"));
    }
}

