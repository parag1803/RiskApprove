package com.riskapprove.apigateway.dto;

import java.util.List;
import java.util.Map;

public class PortfolioResponse {
    private Map<String, Double> allocations;
    private Map<String, Double> amounts;
    private Double totalExpectedReturn;
    private Double totalRisk;
    private Boolean compliant;
    private List<ComplianceViolation> violations;
    private List<ComplianceWarning> warnings;
    private List<Citation> citations;
    private String aiExplanation;
    private List<StockPrediction> predictions;

    public Map<String, Double> getAllocations() {
        return allocations;
    }

    public void setAllocations(Map<String, Double> allocations) {
        this.allocations = allocations;
    }

    public Map<String, Double> getAmounts() {
        return amounts;
    }

    public void setAmounts(Map<String, Double> amounts) {
        this.amounts = amounts;
    }

    public Double getTotalExpectedReturn() {
        return totalExpectedReturn;
    }

    public void setTotalExpectedReturn(Double totalExpectedReturn) {
        this.totalExpectedReturn = totalExpectedReturn;
    }

    public Double getTotalRisk() {
        return totalRisk;
    }

    public void setTotalRisk(Double totalRisk) {
        this.totalRisk = totalRisk;
    }

    public Boolean getCompliant() {
        return compliant;
    }

    public void setCompliant(Boolean compliant) {
        this.compliant = compliant;
    }

    public List<ComplianceViolation> getViolations() {
        return violations;
    }

    public void setViolations(List<ComplianceViolation> violations) {
        this.violations = violations;
    }

    public List<ComplianceWarning> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<ComplianceWarning> warnings) {
        this.warnings = warnings;
    }

    public List<Citation> getCitations() {
        return citations;
    }

    public void setCitations(List<Citation> citations) {
        this.citations = citations;
    }

    public String getAiExplanation() {
        return aiExplanation;
    }

    public void setAiExplanation(String aiExplanation) {
        this.aiExplanation = aiExplanation;
    }

    public List<StockPrediction> getPredictions() {
        return predictions;
    }

    public void setPredictions(List<StockPrediction> predictions) {
        this.predictions = predictions;
    }

    public static class ComplianceViolation {
        private String type;
        private String message;
        private String severity;
        private String source;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }

    public static class ComplianceWarning {
        private String type;
        private String message;
        private String source;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }

    public static class Citation {
        private String text;
        private String source;
        private Double relevanceScore;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public Double getRelevanceScore() {
            return relevanceScore;
        }

        public void setRelevanceScore(Double relevanceScore) {
            this.relevanceScore = relevanceScore;
        }
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

