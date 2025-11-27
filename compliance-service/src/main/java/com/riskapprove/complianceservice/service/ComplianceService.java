package com.riskapprove.complianceservice.service;

import com.riskapprove.complianceservice.dto.ComplianceCheckRequest;
import com.riskapprove.complianceservice.dto.ComplianceCheckResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ComplianceService {
    private static final Logger logger = LoggerFactory.getLogger(ComplianceService.class);
    
    private final RAGServiceClient ragServiceClient;

    public ComplianceService(RAGServiceClient ragServiceClient) {
        this.ragServiceClient = ragServiceClient;
    }

    public ComplianceCheckResponse checkCompliance(ComplianceCheckRequest request) {
        logger.info("Checking compliance for risk profile: {}", request.getRiskProfile());
        
        ComplianceCheckResponse response = new ComplianceCheckResponse();
        List<ComplianceCheckResponse.RuleViolation> ruleViolations = new ArrayList<>();
        
        // Rule-based validation
        Map<String, Double> portfolio = request.getPortfolio();
        String riskProfile = request.getRiskProfile().toUpperCase();
        
        // Check total allocation
        double totalAllocation = portfolio.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        
        if (Math.abs(totalAllocation - 1.0) > 0.01) {
            ComplianceCheckResponse.RuleViolation violation = new ComplianceCheckResponse.RuleViolation();
            violation.setRule("ALLOCATION_TOTAL");
            violation.setDescription(String.format("Total allocation is %.2f%%, must equal 100%%", totalAllocation * 100));
            violation.setSeverity("HIGH");
            ruleViolations.add(violation);
        }
        
        // Risk profile specific limits
        double maxSingleStockAllocation = getMaxSingleStockLimit(riskProfile);
        double maxHighRiskAllocation = getMaxHighRiskLimit(riskProfile);
        
        // Check single stock concentration
        for (Map.Entry<String, Double> entry : portfolio.entrySet()) {
            if (entry.getValue() > maxSingleStockAllocation) {
                ComplianceCheckResponse.RuleViolation violation = new ComplianceCheckResponse.RuleViolation();
                violation.setRule("CONCENTRATION_LIMIT");
                violation.setDescription(String.format(
                    "Stock %s allocation (%.2f%%) exceeds maximum allowed (%.2f%%) for %s risk profile",
                    entry.getKey(), entry.getValue() * 100, maxSingleStockAllocation * 100, riskProfile));
                violation.setSeverity("HIGH");
                ruleViolations.add(violation);
            }
        }
        
        // Check high-risk asset allocation
        double highRiskAllocation = calculateHighRiskAllocation(portfolio, request.getRiskMetrics());
        if (highRiskAllocation > maxHighRiskAllocation) {
            ComplianceCheckResponse.RuleViolation violation = new ComplianceCheckResponse.RuleViolation();
            violation.setRule("HIGH_RISK_LIMIT");
            violation.setDescription(String.format(
                "High-risk asset allocation (%.2f%%) exceeds maximum allowed (%.2f%%) for %s risk profile",
                highRiskAllocation * 100, maxHighRiskAllocation * 100, riskProfile));
            violation.setSeverity("MEDIUM");
            ruleViolations.add(violation);
        }
        
        // Call RAG service for document-based compliance
        ComplianceCheckResponse ragResponse = ragServiceClient.checkCompliance(request);
        
        // Merge results
        response.setCompliant(ruleViolations.isEmpty() && 
                             (ragResponse.getCompliant() == null || ragResponse.getCompliant()));
        response.setRuleViolations(ruleViolations);
        response.setViolations(ragResponse.getViolations());
        response.setWarnings(ragResponse.getWarnings());
        response.setCitations(ragResponse.getCitations());
        
        return response;
    }
    
    private double getMaxSingleStockLimit(String riskProfile) {
        return switch (riskProfile) {
            case "LOW" -> 0.15;      // 15% max per stock
            case "MEDIUM" -> 0.25;   // 25% max per stock
            case "HIGH" -> 0.40;     // 40% max per stock
            default -> 0.25;
        };
    }
    
    private double getMaxHighRiskLimit(String riskProfile) {
        return switch (riskProfile) {
            case "LOW" -> 0.20;      // 20% max in high-risk assets
            case "MEDIUM" -> 0.50;   // 50% max in high-risk assets
            case "HIGH" -> 1.0;      // No limit
            default -> 0.50;
        };
    }
    
    private double calculateHighRiskAllocation(
            Map<String, Double> portfolio,
            Map<String, ComplianceCheckRequest.RiskMetrics> riskMetrics) {
        double highRiskTotal = 0.0;
        
        for (Map.Entry<String, Double> entry : portfolio.entrySet()) {
            ComplianceCheckRequest.RiskMetrics metrics = riskMetrics.get(entry.getKey());
            if (metrics != null && metrics.getRiskScore() != null && metrics.getRiskScore() > 70) {
                highRiskTotal += entry.getValue();
            }
        }
        
        return highRiskTotal;
    }
}

