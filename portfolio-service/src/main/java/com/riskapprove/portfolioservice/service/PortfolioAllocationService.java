package com.riskapprove.portfolioservice.service;

import com.riskapprove.portfolioservice.dto.AllocationRequest;
import com.riskapprove.portfolioservice.dto.AllocationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PortfolioAllocationService {
    private static final Logger logger = LoggerFactory.getLogger(PortfolioAllocationService.class);

    public AllocationResponse calculateAllocation(AllocationRequest request) {
        logger.info("Calculating portfolio allocation for risk profile: {}", request.getRiskProfile());
        
        List<AllocationRequest.StockPrediction> predictions = request.getPredictions();
        String riskProfile = request.getRiskProfile().toUpperCase();
        Double budget = request.getBudget();
        
        // Filter out predictions with errors
        List<AllocationRequest.StockPrediction> validPredictions = predictions.stream()
                .filter(p -> p.getExpectedReturn() != null && p.getRiskScore() != null)
                .collect(Collectors.toList());
        
        if (validPredictions.isEmpty()) {
            throw new RuntimeException("No valid predictions available for allocation");
        }
        
        // Calculate proportional return-based weights adjusted by risk constraints
        Map<String, Double> rawWeights = calculateProportionalWeights(validPredictions);
        
        // Apply risk profile constraints
        Map<String, Double> adjustedWeights = applyRiskConstraints(rawWeights, validPredictions, riskProfile);
        
        // Normalize to ensure total equals 1.0
        double total = adjustedWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total > 0) {
            adjustedWeights.replaceAll((k, v) -> v / total);
        }
        
        // Calculate amounts
        Map<String, Double> amounts = new HashMap<>();
        adjustedWeights.forEach((symbol, weight) -> {
            amounts.put(symbol, weight * budget);
        });
        
        // Calculate portfolio metrics
        double totalExpectedReturn = calculatePortfolioReturn(adjustedWeights, validPredictions);
        double totalRisk = calculatePortfolioRisk(adjustedWeights, validPredictions);
        
        AllocationResponse response = new AllocationResponse();
        response.setAllocations(adjustedWeights);
        response.setAmounts(amounts);
        response.setTotalExpectedReturn(totalExpectedReturn);
        response.setTotalRisk(totalRisk);
        response.setOptimizationMethod("Proportional Return-Based with Risk Constraints");
        
        return response;
    }
    
    private Map<String, Double> calculateProportionalWeights(
            List<AllocationRequest.StockPrediction> predictions) {
        Map<String, Double> weights = new HashMap<>();
        
        // Calculate sum of expected returns (shifted to positive if needed)
        double minReturn = predictions.stream()
                .mapToDouble(p -> p.getExpectedReturn() != null ? p.getExpectedReturn() : 0.0)
                .min()
                .orElse(0.0);
        
        double shift = minReturn < 0 ? Math.abs(minReturn) + 0.01 : 0.0;
        double sumReturns = predictions.stream()
                .mapToDouble(p -> (p.getExpectedReturn() != null ? p.getExpectedReturn() : 0.0) + shift)
                .sum();
        
        // Assign weights proportional to expected returns
        for (AllocationRequest.StockPrediction prediction : predictions) {
            double expectedReturn = prediction.getExpectedReturn() != null ? 
                    prediction.getExpectedReturn() : 0.0;
            double weight = (expectedReturn + shift) / sumReturns;
            weights.put(prediction.getSymbol(), weight);
        }
        
        return weights;
    }
    
    private Map<String, Double> applyRiskConstraints(
            Map<String, Double> rawWeights,
            List<AllocationRequest.StockPrediction> predictions,
            String riskProfile) {
        Map<String, Double> adjustedWeights = new HashMap<>(rawWeights);
        
        // Get risk limits based on profile
        double maxSingleStock = getMaxSingleStockLimit(riskProfile);
        double maxHighRisk = getMaxHighRiskLimit(riskProfile);
        
        // Cap individual stock weights
        adjustedWeights.replaceAll((symbol, weight) -> Math.min(weight, maxSingleStock));
        
        // Adjust high-risk stock weights
        double highRiskTotal = 0.0;
        for (AllocationRequest.StockPrediction prediction : predictions) {
            if (prediction.getRiskScore() != null && prediction.getRiskScore() > 70) {
                highRiskTotal += adjustedWeights.getOrDefault(prediction.getSymbol(), 0.0);
            }
        }
        
        if (highRiskTotal > maxHighRisk) {
            double scaleFactor = maxHighRisk / highRiskTotal;
            for (AllocationRequest.StockPrediction prediction : predictions) {
                if (prediction.getRiskScore() != null && prediction.getRiskScore() > 70) {
                    String symbol = prediction.getSymbol();
                    adjustedWeights.put(symbol, adjustedWeights.get(symbol) * scaleFactor);
                }
            }
        }
        
        return adjustedWeights;
    }
    
    private double getMaxSingleStockLimit(String riskProfile) {
        return switch (riskProfile) {
            case "LOW" -> 0.20;
            case "MEDIUM" -> 0.30;
            case "HIGH" -> 0.50;
            default -> 0.30;
        };
    }
    
    private double getMaxHighRiskLimit(String riskProfile) {
        return switch (riskProfile) {
            case "LOW" -> 0.30;
            case "MEDIUM" -> 0.60;
            case "HIGH" -> 1.0;
            default -> 0.60;
        };
    }
    
    private double calculatePortfolioReturn(
            Map<String, Double> weights,
            List<AllocationRequest.StockPrediction> predictions) {
        double portfolioReturn = 0.0;
        
        for (AllocationRequest.StockPrediction prediction : predictions) {
            double weight = weights.getOrDefault(prediction.getSymbol(), 0.0);
            double expectedReturn = prediction.getExpectedReturn() != null ? 
                    prediction.getExpectedReturn() : 0.0;
            portfolioReturn += weight * expectedReturn;
        }
        
        return portfolioReturn;
    }
    
    private double calculatePortfolioRisk(
            Map<String, Double> weights,
            List<AllocationRequest.StockPrediction> predictions) {
        // Simplified portfolio risk calculation (weighted average volatility)
        double portfolioRisk = 0.0;
        
        for (AllocationRequest.StockPrediction prediction : predictions) {
            double weight = weights.getOrDefault(prediction.getSymbol(), 0.0);
            double volatility = prediction.getVolatility() != null ? 
                    prediction.getVolatility() : 0.0;
            portfolioRisk += weight * volatility;
        }
        
        return portfolioRisk;
    }
}

