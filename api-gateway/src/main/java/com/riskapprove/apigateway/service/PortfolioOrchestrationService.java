package com.riskapprove.apigateway.service;

import com.riskapprove.apigateway.dto.PortfolioRequest;
import com.riskapprove.apigateway.dto.PortfolioResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class PortfolioOrchestrationService {
    private static final Logger logger = LoggerFactory.getLogger(PortfolioOrchestrationService.class);
    
    private final RestTemplate restTemplate;
    private final String dataServiceUrl;
    private final String complianceServiceUrl;
    private final String portfolioServiceUrl;
    private final LLMExplanationService llmExplanationService;

    public PortfolioOrchestrationService(
            RestTemplate restTemplate,
            @Value("${services.data.url}") String dataServiceUrl,
            @Value("${services.compliance.url}") String complianceServiceUrl,
            @Value("${services.portfolio.url}") String portfolioServiceUrl,
            LLMExplanationService llmExplanationService) {
        this.restTemplate = restTemplate;
        this.dataServiceUrl = dataServiceUrl;
        this.complianceServiceUrl = complianceServiceUrl;
        this.portfolioServiceUrl = portfolioServiceUrl;
        this.llmExplanationService = llmExplanationService;
    }

    public PortfolioResponse generatePortfolio(PortfolioRequest request) {
        logger.info("Orchestrating portfolio generation for risk profile: {}", request.getRiskProfile());
        
        try {
            // Step 1: Get predictions from Data Service
            PortfolioResponse.StockPrediction[] predictions = getPredictions(request.getStocks());
            
            // Step 2: Calculate allocation from Portfolio Service
            Map<String, Double> allocations = getAllocation(request, predictions);
            Map<String, Double> amounts = calculateAmounts(allocations, request.getBudget());
            
            // Step 3: Check compliance from Compliance Service
            Map<String, Object> complianceResult = checkCompliance(
                    request, allocations, predictions);
            
            // Step 4: Generate AI explanation
            String aiExplanation = llmExplanationService.generateExplanation(
                    request, allocations, predictions, complianceResult);
            
            // Build response
            PortfolioResponse response = new PortfolioResponse();
            response.setAllocations(allocations);
            response.setAmounts(amounts);
            response.setPredictions(Arrays.asList(predictions));
            response.setCompliant((Boolean) complianceResult.get("compliant"));
            response.setViolations((List<PortfolioResponse.ComplianceViolation>) complianceResult.get("violations"));
            response.setWarnings((List<PortfolioResponse.ComplianceWarning>) complianceResult.get("warnings"));
            response.setCitations((List<PortfolioResponse.Citation>) complianceResult.get("citations"));
            response.setAiExplanation(aiExplanation);
            
            // Calculate portfolio metrics
            double totalReturn = calculatePortfolioReturn(allocations, predictions);
            double totalRisk = calculatePortfolioRisk(allocations, predictions);
            response.setTotalExpectedReturn(totalReturn);
            response.setTotalRisk(totalRisk);
            
            return response;
        } catch (Exception e) {
            logger.error("Error orchestrating portfolio generation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate portfolio", e);
        }
    }
    
    private PortfolioResponse.StockPrediction[] getPredictions(List<String> stocks) {
        String url = dataServiceUrl + "/api/data/predictions";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> body = new HashMap<>();
        body.put("stocks", stocks);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            List<Map<String, Object>> predictionsList = (List<Map<String, Object>>) response.getBody().get("predictions");
            return predictionsList.stream()
                    .map(this::mapToPrediction)
                    .toArray(PortfolioResponse.StockPrediction[]::new);
        }
        
        throw new RuntimeException("Failed to get predictions");
    }
    
    private PortfolioResponse.StockPrediction mapToPrediction(Map<String, Object> map) {
        PortfolioResponse.StockPrediction prediction = new PortfolioResponse.StockPrediction();
        prediction.setSymbol((String) map.get("symbol"));
        prediction.setExpectedReturn(map.get("expectedReturn") != null ? 
                ((Number) map.get("expectedReturn")).doubleValue() : null);
        prediction.setVolatility(map.get("volatility") != null ? 
                ((Number) map.get("volatility")).doubleValue() : null);
        prediction.setRiskScore(map.get("riskScore") != null ? 
                ((Number) map.get("riskScore")).doubleValue() : null);
        prediction.setTrend((String) map.get("trend"));
        prediction.setSignal((String) map.get("signal"));
        return prediction;
    }
    
    private Map<String, Double> getAllocation(
            PortfolioRequest request, 
            PortfolioResponse.StockPrediction[] predictions) {
        String url = portfolioServiceUrl + "/api/portfolio/allocate";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> body = new HashMap<>();
        body.put("budget", request.getBudget());
        body.put("riskProfile", request.getRiskProfile());
        body.put("stocks", request.getStocks());
        body.put("investmentHorizon", request.getInvestmentHorizon());
        
        List<Map<String, Object>> predictionsList = new ArrayList<>();
        for (PortfolioResponse.StockPrediction pred : predictions) {
            Map<String, Object> predMap = new HashMap<>();
            predMap.put("symbol", pred.getSymbol());
            predMap.put("expectedReturn", pred.getExpectedReturn());
            predMap.put("volatility", pred.getVolatility());
            predMap.put("riskScore", pred.getRiskScore());
            predMap.put("trend", pred.getTrend());
            predMap.put("signal", pred.getSignal());
            predictionsList.add(predMap);
        }
        body.put("predictions", predictionsList);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Number> allocationsMap = (Map<String, Number>) response.getBody().get("allocations");
            Map<String, Double> allocations = new HashMap<>();
            allocationsMap.forEach((k, v) -> allocations.put(k, v.doubleValue()));
            return allocations;
        }
        
        throw new RuntimeException("Failed to get allocation");
    }
    
    private Map<String, Double> calculateAmounts(Map<String, Double> allocations, Double budget) {
        Map<String, Double> amounts = new HashMap<>();
        allocations.forEach((symbol, weight) -> {
            amounts.put(symbol, weight * budget);
        });
        return amounts;
    }
    
    private Map<String, Object> checkCompliance(
            PortfolioRequest request,
            Map<String, Double> allocations,
            PortfolioResponse.StockPrediction[] predictions) {
        String url = complianceServiceUrl + "/api/compliance/check";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> body = new HashMap<>();
        body.put("portfolio", allocations);
        body.put("riskProfile", request.getRiskProfile());
        
        Map<String, Map<String, Double>> riskMetrics = new HashMap<>();
        for (PortfolioResponse.StockPrediction pred : predictions) {
            Map<String, Double> metrics = new HashMap<>();
            if (pred.getRiskScore() != null) {
                metrics.put("riskScore", pred.getRiskScore());
            }
            if (pred.getVolatility() != null) {
                metrics.put("volatility", pred.getVolatility());
            }
            riskMetrics.put(pred.getSymbol(), metrics);
        }
        body.put("riskMetrics", riskMetrics);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> result = response.getBody();
            
            // Convert violations
            List<Map<String, Object>> violationsList = (List<Map<String, Object>>) result.get("violations");
            List<PortfolioResponse.ComplianceViolation> violations = new ArrayList<>();
            if (violationsList != null) {
                for (Map<String, Object> v : violationsList) {
                    PortfolioResponse.ComplianceViolation violation = new PortfolioResponse.ComplianceViolation();
                    violation.setType((String) v.get("type"));
                    violation.setMessage((String) v.get("message"));
                    violation.setSeverity((String) v.get("severity"));
                    violation.setSource((String) v.get("source"));
                    violations.add(violation);
                }
            }
            
            // Convert warnings
            List<Map<String, Object>> warningsList = (List<Map<String, Object>>) result.get("warnings");
            List<PortfolioResponse.ComplianceWarning> warnings = new ArrayList<>();
            if (warningsList != null) {
                for (Map<String, Object> w : warningsList) {
                    PortfolioResponse.ComplianceWarning warning = new PortfolioResponse.ComplianceWarning();
                    warning.setType((String) w.get("type"));
                    warning.setMessage((String) w.get("message"));
                    warning.setSource((String) w.get("source"));
                    warnings.add(warning);
                }
            }
            
            // Convert citations
            List<Map<String, Object>> citationsList = (List<Map<String, Object>>) result.get("citations");
            List<PortfolioResponse.Citation> citations = new ArrayList<>();
            if (citationsList != null) {
                for (Map<String, Object> c : citationsList) {
                    PortfolioResponse.Citation citation = new PortfolioResponse.Citation();
                    citation.setText((String) c.get("text"));
                    citation.setSource((String) c.get("source"));
                    if (c.get("relevanceScore") != null) {
                        citation.setRelevanceScore(((Number) c.get("relevanceScore")).doubleValue());
                    }
                    citations.add(citation);
                }
            }
            
            Map<String, Object> complianceResult = new HashMap<>();
            complianceResult.put("compliant", result.get("compliant"));
            complianceResult.put("violations", violations);
            complianceResult.put("warnings", warnings);
            complianceResult.put("citations", citations);
            
            return complianceResult;
        }
        
        // Return default compliant result on error
        Map<String, Object> defaultResult = new HashMap<>();
        defaultResult.put("compliant", true);
        defaultResult.put("violations", new ArrayList<>());
        defaultResult.put("warnings", new ArrayList<>());
        defaultResult.put("citations", new ArrayList<>());
        return defaultResult;
    }
    
    private double calculatePortfolioReturn(
            Map<String, Double> allocations,
            PortfolioResponse.StockPrediction[] predictions) {
        double totalReturn = 0.0;
        for (PortfolioResponse.StockPrediction pred : predictions) {
            double weight = allocations.getOrDefault(pred.getSymbol(), 0.0);
            double expectedReturn = pred.getExpectedReturn() != null ? pred.getExpectedReturn() : 0.0;
            totalReturn += weight * expectedReturn;
        }
        return totalReturn;
    }
    
    private double calculatePortfolioRisk(
            Map<String, Double> allocations,
            PortfolioResponse.StockPrediction[] predictions) {
        double totalRisk = 0.0;
        for (PortfolioResponse.StockPrediction pred : predictions) {
            double weight = allocations.getOrDefault(pred.getSymbol(), 0.0);
            double volatility = pred.getVolatility() != null ? pred.getVolatility() : 0.0;
            totalRisk += weight * volatility;
        }
        return totalRisk;
    }
}

