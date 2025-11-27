package com.riskapprove.apigateway.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public class PortfolioRequest {
    @NotNull(message = "Budget is required")
    @Positive(message = "Budget must be positive")
    private Double budget;
    
    @NotNull(message = "Risk profile is required")
    private String riskProfile;
    
    @NotEmpty(message = "Stocks list cannot be empty")
    private List<String> stocks;
    
    @NotNull(message = "Investment horizon is required")
    @Positive(message = "Investment horizon must be positive")
    private Integer investmentHorizon; // in months

    public Double getBudget() {
        return budget;
    }

    public void setBudget(Double budget) {
        this.budget = budget;
    }

    public String getRiskProfile() {
        return riskProfile;
    }

    public void setRiskProfile(String riskProfile) {
        this.riskProfile = riskProfile;
    }

    public List<String> getStocks() {
        return stocks;
    }

    public void setStocks(List<String> stocks) {
        this.stocks = stocks;
    }

    public Integer getInvestmentHorizon() {
        return investmentHorizon;
    }

    public void setInvestmentHorizon(Integer investmentHorizon) {
        this.investmentHorizon = investmentHorizon;
    }
}

