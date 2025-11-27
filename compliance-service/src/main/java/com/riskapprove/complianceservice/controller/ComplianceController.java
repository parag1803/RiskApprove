package com.riskapprove.complianceservice.controller;

import com.riskapprove.complianceservice.dto.ComplianceCheckRequest;
import com.riskapprove.complianceservice.dto.ComplianceCheckResponse;
import com.riskapprove.complianceservice.service.ComplianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/compliance")
@Tag(name = "Compliance Service", description = "Portfolio compliance checking API")
public class ComplianceController {
    private static final Logger logger = LoggerFactory.getLogger(ComplianceController.class);
    
    private final ComplianceService complianceService;

    public ComplianceController(ComplianceService complianceService) {
        this.complianceService = complianceService;
    }

    @PostMapping("/check")
    @Operation(summary = "Check portfolio compliance", 
               description = "Validates portfolio against risk limits and regulations")
    public ResponseEntity<ComplianceCheckResponse> checkCompliance(
            @Valid @RequestBody ComplianceCheckRequest request) {
        logger.info("Received compliance check request for risk profile: {}", request.getRiskProfile());
        
        try {
            ComplianceCheckResponse response = complianceService.checkCompliance(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing compliance check: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "healthy", "service", "compliance-service"));
    }
}

