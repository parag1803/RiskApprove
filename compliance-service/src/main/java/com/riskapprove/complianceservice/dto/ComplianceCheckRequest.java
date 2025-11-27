package com.riskapprove.complianceservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class ComplianceCheckRequest {
    @NotEmpty(message = "Portfolio cannot be empty")
    private Map<String, Double> portfolio;
    
    @NotNull(message = "Risk profile is required")
    private String riskProfile;
    
    @NotEmpty(message = "Risk metrics cannot be empty")
    private Map<String, RiskMetrics> riskMetrics;

    public Map<String, Double> getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(Map<String, Double> portfolio) {
        this.portfolio = portfolio;
    }

    public String getRiskProfile() {
        return riskProfile;
    }

    public void setRiskProfile(String riskProfile) {
        this.riskProfile = riskProfile;
    }

    public Map<String, RiskMetrics> getRiskMetrics() {
        return riskMetrics;
    }

    public void setRiskMetrics(Map<String, RiskMetrics> riskMetrics) {
        this.riskMetrics = riskMetrics;
    }

    public static class RiskMetrics {
        private Double riskScore;
        private Double volatility;

        public Double getRiskScore() {
            return riskScore;
        }

        public void setRiskScore(Double riskScore) {
            this.riskScore = riskScore;
        }

        public Double getVolatility() {
            return volatility;
        }

        public void setVolatility(Double volatility) {
            this.volatility = volatility;
        }
    }
}

