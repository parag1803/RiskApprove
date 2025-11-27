package com.riskapprove.portfolioservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;

public class AllocationRequest {
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
    
    @NotEmpty(message = "Predictions cannot be empty")
    private List<StockPrediction> predictions;

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

    public List<StockPrediction> getPredictions() {
        return predictions;
    }

    public void setPredictions(List<StockPrediction> predictions) {
        this.predictions = predictions;
    }

    public static class StockPrediction {
        private String symbol;
        private Double expectedReturn;
        private Double volatility;
        private Double riskScore;
        private String trend;
        private String signal;

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public Double getExpectedReturn() {
            return expectedReturn;
        }

        public void setExpectedReturn(Double expectedReturn) {
            this.expectedReturn = expectedReturn;
        }

        public Double getVolatility() {
            return volatility;
        }

        public void setVolatility(Double volatility) {
            this.volatility = volatility;
        }

        public Double getRiskScore() {
            return riskScore;
        }

        public void setRiskScore(Double riskScore) {
            this.riskScore = riskScore;
        }

        public String getTrend() {
            return trend;
        }

        public void setTrend(String trend) {
            this.trend = trend;
        }

        public String getSignal() {
            return signal;
        }

        public void setSignal(String signal) {
            this.signal = signal;
        }
    }
}

