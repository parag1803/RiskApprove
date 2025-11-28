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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM Explanation Service - Uses ONLY FREE open-source models
 * 
 * Supported FREE options:
 * 1. Ollama (Local) - Best for privacy, runs locally
 * 2. HuggingFace Inference API (Free tier) - Cloud-based, free models
 * 3. Template fallback - No LLM needed
 */
@Service
public class LLMExplanationService {
    private static final Logger logger = LoggerFactory.getLogger(LLMExplanationService.class);
    
    // HuggingFace API key (FREE - just needs account at huggingface.co)
    @Value("${huggingface.api.key:}")
    private String huggingfaceApiKey;
    
    // Ollama URL for local LLM (completely FREE and private)
    @Value("${ollama.url:http://ollama:11434}")
    private String ollamaUrl;
    
    // Ollama model to use (default: mistral - good balance of speed/quality)
    @Value("${ollama.model:mistral}")
    private String ollamaModel;
    
    private final RestTemplate restTemplate;
    
    // List of FREE HuggingFace models to try (in order of preference)
    private static final String[] FREE_HF_MODELS = {
        "mistralai/Mistral-7B-Instruct-v0.2",      // Best quality, free
        "HuggingFaceH4/zephyr-7b-beta",             // Good alternative
        "google/flan-t5-large",                     // Smaller, faster
        "tiiuae/falcon-7b-instruct"                 // Another option
    };
    
    public LLMExplanationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String generateExplanation(
            PortfolioRequest request,
            Map<String, Double> allocations,
            PortfolioResponse.StockPrediction[] predictions,
            Map<String, Object> complianceResult) {
        
        // Build prompt
        String prompt = buildPrompt(request, allocations, predictions, complianceResult);
        
        // Try Ollama first (local, completely free, private)
        String explanation = tryOllama(prompt);
        if (explanation != null && !explanation.isEmpty()) {
            logger.info("Generated explanation using Ollama (local LLM)");
            return explanation;
        }
        
        // Try HuggingFace Inference API (free tier)
        if (huggingfaceApiKey != null && !huggingfaceApiKey.isEmpty()) {
            explanation = tryHuggingFace(prompt);
            if (explanation != null && !explanation.isEmpty()) {
                logger.info("Generated explanation using HuggingFace (free)");
                return explanation;
            }
        }
        
        // Fallback to enhanced template (always works, no LLM needed)
        logger.info("Using enhanced template explanation (no LLM)");
        return generateEnhancedTemplateExplanation(request, allocations, predictions, complianceResult);
    }
    
    private String buildPrompt(
            PortfolioRequest request,
            Map<String, Double> allocations,
            PortfolioResponse.StockPrediction[] predictions,
            Map<String, Object> complianceResult) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a financial advisor explaining a portfolio to a beginner investor. ");
        prompt.append("Be concise and use simple language.\n\n");
        
        prompt.append("Portfolio Details:\n");
        prompt.append("- Budget: $").append(String.format("%.2f", request.getBudget())).append("\n");
        prompt.append("- Risk Profile: ").append(request.getRiskProfile()).append("\n");
        prompt.append("- Investment Horizon: ").append(request.getInvestmentHorizon()).append(" months\n\n");
        
        prompt.append("Stock Allocations:\n");
        for (PortfolioResponse.StockPrediction pred : predictions) {
            double allocation = allocations.getOrDefault(pred.getSymbol(), 0.0);
            prompt.append(String.format("- %s: %.1f%% (Return: %.2f%%, Risk: %.0f, Trend: %s)\n",
                pred.getSymbol(), allocation * 100,
                pred.getExpectedReturn() != null ? pred.getExpectedReturn() * 100 : 0.0,
                pred.getRiskScore() != null ? pred.getRiskScore() : 0.0,
                pred.getTrend() != null ? pred.getTrend() : "N/A"));
        }
        
        Boolean compliant = (Boolean) complianceResult.get("compliant");
        prompt.append("\nCompliance: ").append(compliant ? "Compliant" : "Has Issues").append("\n");
        
        prompt.append("\nExplain in 2-3 short paragraphs: why each stock was chosen, ");
        prompt.append("how risk is balanced, and the overall strategy.");
        
        return prompt.toString();
    }
    
    /**
     * Try Ollama for local LLM inference (completely FREE)
     * Requires Ollama to be running locally or in Docker
     */
    private String tryOllama(String prompt) {
        try {
            String apiUrl = ollamaUrl + "/api/generate";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", ollamaModel);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            requestBody.put("options", Map.of(
                "temperature", 0.7,
                "num_predict", 500
            ));
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String generatedText = (String) response.getBody().get("response");
                if (generatedText != null && !generatedText.trim().isEmpty()) {
                    return generatedText.trim();
                }
            }
        } catch (Exception e) {
            logger.debug("Ollama not available: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Try HuggingFace Inference API with FREE models
     * Free tier allows ~30,000 requests/month
     */
    private String tryHuggingFace(String prompt) {
        for (String model : FREE_HF_MODELS) {
            try {
                String explanation = callHuggingFaceModel(model, prompt);
                if (explanation != null && !explanation.trim().isEmpty()) {
                    return explanation;
                }
            } catch (Exception e) {
                logger.debug("HuggingFace model {} failed: {}", model, e.getMessage());
            }
        }
        return null;
    }
    
    private String callHuggingFaceModel(String model, String prompt) {
        try {
            String apiUrl = "https://api-inference.huggingface.co/models/" + model;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(huggingfaceApiKey);
            
            // Format for instruction-tuned models
            String formattedPrompt;
            if (model.contains("Mistral") || model.contains("zephyr")) {
                formattedPrompt = "<s>[INST] " + prompt + " [/INST]";
            } else if (model.contains("falcon")) {
                formattedPrompt = "User: " + prompt + "\nAssistant:";
            } else {
                formattedPrompt = prompt;
            }
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("inputs", formattedPrompt);
            requestBody.put("parameters", Map.of(
                "max_new_tokens", 500,
                "temperature", 0.7,
                "return_full_text", false,
                "do_sample", true
            ));
            // Wait for model to load if needed
            requestBody.put("options", Map.of("wait_for_model", true));
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Object> response = restTemplate.postForEntity(apiUrl, entity, Object.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object body = response.getBody();
                
                // Handle array response format
                if (body instanceof List) {
                    List<Map<String, Object>> results = (List<Map<String, Object>>) body;
                    if (!results.isEmpty()) {
                        Object text = results.get(0).get("generated_text");
                        if (text != null) {
                            return cleanResponse(text.toString());
                        }
                    }
                }
                // Handle direct map response
                else if (body instanceof Map) {
                    Map<String, Object> result = (Map<String, Object>) body;
                    Object text = result.get("generated_text");
                    if (text != null) {
                        return cleanResponse(text.toString());
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error calling HuggingFace model {}: {}", model, e.getMessage());
        }
        return null;
    }
    
    private String cleanResponse(String response) {
        // Remove instruction markers if present
        response = response.replaceAll("\\[/INST\\]", "").trim();
        response = response.replaceAll("<s>", "").trim();
        response = response.replaceAll("</s>", "").trim();
        response = response.replaceAll("^Assistant:\\s*", "").trim();
        return response;
    }
    
    /**
     * Enhanced template-based explanation (no LLM required)
     * This is a sophisticated fallback that works without any API
     */
    private String generateEnhancedTemplateExplanation(
            PortfolioRequest request,
            Map<String, Double> allocations,
            PortfolioResponse.StockPrediction[] predictions,
            Map<String, Object> complianceResult) {
        
        StringBuilder explanation = new StringBuilder();
        
        // Portfolio Overview
        explanation.append("📊 Portfolio Overview\n\n");
        explanation.append("Your ").append(request.getRiskProfile().toLowerCase())
                  .append("-risk portfolio of $")
                  .append(String.format("%,.2f", request.getBudget()))
                  .append(" has been optimized for a ")
                  .append(request.getInvestmentHorizon())
                  .append("-month investment horizon.\n\n");
        
        // Stock Analysis
        explanation.append("📈 Stock Selection Analysis\n\n");
        
        // Sort by allocation
        List<PortfolioResponse.StockPrediction> sortedPreds = new ArrayList<>();
        for (PortfolioResponse.StockPrediction pred : predictions) {
            sortedPreds.add(pred);
        }
        sortedPreds.sort((a, b) -> {
            double allocA = allocations.getOrDefault(a.getSymbol(), 0.0);
            double allocB = allocations.getOrDefault(b.getSymbol(), 0.0);
            return Double.compare(allocB, allocA);
        });
        
        for (PortfolioResponse.StockPrediction pred : sortedPreds) {
            double allocation = allocations.getOrDefault(pred.getSymbol(), 0.0);
            double amount = allocation * request.getBudget();
            
            explanation.append("• ").append(pred.getSymbol())
                      .append(" (").append(String.format("%.1f%%", allocation * 100))
                      .append(" = $").append(String.format("%,.2f", amount)).append(")\n");
            
            // Add insight based on metrics
            if (pred.getExpectedReturn() != null) {
                explanation.append("  Expected return: ")
                          .append(String.format("%.2f%%", pred.getExpectedReturn() * 100));
                
                if (pred.getExpectedReturn() > 0.10) {
                    explanation.append(" (strong growth potential)");
                } else if (pred.getExpectedReturn() > 0.05) {
                    explanation.append(" (moderate growth)");
                } else {
                    explanation.append(" (stable)");
                }
                explanation.append("\n");
            }
            
            if (pred.getTrend() != null) {
                String trendEmoji = pred.getTrend().equals("BULLISH") ? "🟢" : 
                                   pred.getTrend().equals("BEARISH") ? "🔴" : "🟡";
                explanation.append("  Trend: ").append(trendEmoji).append(" ")
                          .append(pred.getTrend().toLowerCase()).append("\n");
            }
            
            if (pred.getSignal() != null) {
                explanation.append("  Signal: ").append(pred.getSignal()).append("\n");
            }
            
            explanation.append("\n");
        }
        
        // Risk Management
        explanation.append("⚖️ Risk Management\n\n");
        
        double avgRisk = 0;
        int count = 0;
        for (PortfolioResponse.StockPrediction pred : predictions) {
            if (pred.getRiskScore() != null) {
                avgRisk += pred.getRiskScore() * allocations.getOrDefault(pred.getSymbol(), 0.0);
                count++;
            }
        }
        
        if (count > 0) {
            explanation.append("Portfolio weighted risk score: ")
                      .append(String.format("%.0f", avgRisk)).append("/100\n");
            
            String riskLevel = avgRisk < 40 ? "conservative" : avgRisk < 60 ? "moderate" : "aggressive";
            explanation.append("This is a ").append(riskLevel)
                      .append(" portfolio, aligned with your ")
                      .append(request.getRiskProfile().toLowerCase())
                      .append(" risk preference.\n\n");
        }
        
        // Compliance Status
        explanation.append("✅ Compliance Status\n\n");
        Boolean compliant = (Boolean) complianceResult.get("compliant");
        
        if (compliant != null && compliant) {
            explanation.append("Your portfolio meets all regulatory compliance requirements.\n");
        } else {
            explanation.append("Please review the compliance warnings above. ");
            List<PortfolioResponse.ComplianceViolation> violations = 
                    (List<PortfolioResponse.ComplianceViolation>) complianceResult.get("violations");
            if (violations != null && !violations.isEmpty()) {
                explanation.append("There are ").append(violations.size())
                          .append(" item(s) that may need attention.\n");
            }
        }
        
        return explanation.toString();
    }
}
