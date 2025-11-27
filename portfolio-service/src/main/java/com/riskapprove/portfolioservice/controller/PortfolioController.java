package com.riskapprove.portfolioservice.controller;

import com.riskapprove.portfolioservice.dto.AllocationRequest;
import com.riskapprove.portfolioservice.dto.AllocationResponse;
import com.riskapprove.portfolioservice.service.PortfolioAllocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/portfolio")
@Tag(name = "Portfolio Service", description = "Portfolio allocation and optimization API")
public class PortfolioController {
    private static final Logger logger = LoggerFactory.getLogger(PortfolioController.class);
    
    private final PortfolioAllocationService allocationService;

    public PortfolioController(PortfolioAllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @PostMapping("/allocate")
    @Operation(summary = "Calculate portfolio allocation", 
               description = "Computes optimal portfolio weights based on predictions and risk profile")
    public ResponseEntity<AllocationResponse> allocate(
            @Valid @RequestBody AllocationRequest request) {
        logger.info("Received allocation request for budget: {}, risk profile: {}", 
                   request.getBudget(), request.getRiskProfile());
        
        try {
            AllocationResponse response = allocationService.calculateAllocation(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error calculating allocation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "healthy", "service", "portfolio-service"));
    }
}

