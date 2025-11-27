package com.riskapprove.apigateway.service;

import com.riskapprove.apigateway.dto.PortfolioRequest;
import com.riskapprove.apigateway.dto.PortfolioResponse;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LLMExplanationService {
    private static final Logger logger = LoggerFactory.getLogger(LLMExplanationService.class);
    
    @Value("${openai.api.key:}")
    private String openaiApiKey;
    
    @Value("${huggingface.api.key:}")
    private String huggingfaceApiKey;
    
    private final RestTemplate restTemplate;
    
    public LLMExplanationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String generateExplanation(
            PortfolioRequest request,
            Map<String, Double> allocations,
            PortfolioResponse.StockPrediction[] predictions,
            Map<String, Object> complianceResult) {
        
        // Build prompt for LLM
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a financial advisor explaining a portfolio allocation to a beginner investor.\n\n");
        prompt.append("Portfolio Details:\n");
        prompt.append("- Budget: $").append(String.format("%.2f", request.getBudget())).append("\n");
        prompt.append("- Risk Profile: ").append(request.getRiskProfile()).append("\n");
        prompt.append("- Investment Horizon: ").append(request.getInvestmentHorizon()).append(" months\n\n");
        
        prompt.append("Stock Allocations:\n");
        for (PortfolioResponse.StockPrediction pred : predictions) {
            double allocation = allocations.getOrDefault(pred.getSymbol(), 0.0);
            prompt.append(String.format("- %s: %.1f%% (Expected Return: %.2f%%, Risk Score: %.0f, Trend: %s)\n",
                pred.getSymbol(), allocation * 100,
                pred.getExpectedReturn() != null ? pred.getExpectedReturn() * 100 : 0.0,
                pred.getRiskScore() != null ? pred.getRiskScore() : 0.0,
                pred.getTrend() != null ? pred.getTrend() : "N/A"));
        }
        
        prompt.append("\nCompliance Status: ");
        prompt.append((Boolean) complianceResult.get("compliant") ? "Compliant" : "Has Violations").append("\n");
        
        List<PortfolioResponse.ComplianceViolation> violations = 
                (List<PortfolioResponse.ComplianceViolation>) complianceResult.get("violations");
        if (violations != null && !violations.isEmpty()) {
            prompt.append("Violations: ").append(violations.size()).append("\n");
        }
        
        prompt.append("\nPlease provide a beginner-friendly explanation (2-3 paragraphs) covering:\n");
        prompt.append("1. Why each stock was chosen and its allocation percentage\n");
        prompt.append("2. How risk was balanced based on the risk profile\n");
        prompt.append("3. Why the portfolio is compliant (or what needs attention)\n");
        prompt.append("4. Overall portfolio strategy and expected outcomes\n");
        
        String userPrompt = prompt.toString();
        
        // Try HuggingFace Inference API first (FREE)
        if (huggingfaceApiKey != null && !huggingfaceApiKey.isEmpty()) {
            try {
                String explanation = callHuggingFaceAPI(userPrompt);
                if (explanation != null && !explanation.isEmpty()) {
                    logger.info("Successfully generated explanation using HuggingFace");
                    return explanation;
                }
            } catch (Exception e) {
                logger.warn("HuggingFace API failed: {}", e.getMessage());
            }
        }
        
        // Try OpenAI API if key is provided
        if (openaiApiKey != null && !openaiApiKey.isEmpty()) {
            try {
                OpenAiService service = new OpenAiService(openaiApiKey, Duration.ofSeconds(30));
                
                List<ChatMessage> messages = new ArrayList<>();
                messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), 
                    "You are a financial advisor explaining portfolio allocations to beginner investors. " +
                    "Use simple, clear language. Be concise (2-3 paragraphs)."));
                messages.add(new ChatMessage(ChatMessageRole.USER.value(), userPrompt));
                
                ChatCompletionRequest chatRequest = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .messages(messages)
                    .temperature(0.7)
                    .maxTokens(500)
                    .build();
                
                String explanation = service.createChatCompletion(chatRequest)
                    .getChoices().get(0).getMessage().getContent();
                
                logger.info("Successfully generated LLM explanation using OpenAI");
                return explanation;
            } catch (Exception e) {
                logger.warn("OpenAI API failed: {}", e.getMessage());
            }
        }
        
        // Fallback to template-based explanation
        logger.info("No LLM API keys available, using template explanation");
        return generateTemplateExplanation(request, allocations, predictions, complianceResult);
    }
    
    private String generateTemplateExplanation(
            PortfolioRequest request,
            Map<String, Double> allocations,
            PortfolioResponse.StockPrediction[] predictions,
            Map<String, Object> complianceResult) {
        
        StringBuilder explanation = new StringBuilder();
        
        explanation.append("Portfolio Overview:\n\n");
        explanation.append("This portfolio has been optimized for a ").append(request.getRiskProfile())
                  .append(" risk profile with a budget of $")
                  .append(String.format("%.2f", request.getBudget()))
                  .append(" and an investment horizon of ")
                  .append(request.getInvestmentHorizon())
                  .append(" months.\n\n");
        
        explanation.append("Stock Selection:\n");
        for (PortfolioResponse.StockPrediction pred : predictions) {
            double allocation = allocations.getOrDefault(pred.getSymbol(), 0.0);
            explanation.append(String.format("- %s (%.1f%%): ", pred.getSymbol(), allocation * 100));
            
            if (pred.getExpectedReturn() != null && pred.getExpectedReturn() > 0) {
                explanation.append("Selected for its positive expected return of ")
                          .append(String.format("%.2f%%", pred.getExpectedReturn() * 100));
            }
            
            if (pred.getTrend() != null) {
                explanation.append(" with a ").append(pred.getTrend().toLowerCase()).append(" trend");
            }
            
            if (pred.getRiskScore() != null) {
                if (pred.getRiskScore() > 70) {
                    explanation.append(". This is a higher-risk stock");
                } else if (pred.getRiskScore() < 40) {
                    explanation.append(". This is a lower-risk stock");
                }
            }
            
            explanation.append(".\n");
        }
        
        explanation.append("\nRisk Management:\n");
        explanation.append("The portfolio has been balanced to align with your ").append(request.getRiskProfile())
                  .append(" risk tolerance. ");
        
        boolean hasHighRisk = false;
        for (PortfolioResponse.StockPrediction pred : predictions) {
            if (pred.getRiskScore() != null && pred.getRiskScore() > 70) {
                hasHighRisk = true;
                break;
            }
        }
        
        if (hasHighRisk && request.getRiskProfile().equalsIgnoreCase("LOW")) {
            explanation.append("Note: Some high-risk stocks are included, which may not be ideal for a low-risk profile.");
        } else {
            explanation.append("Stock allocations have been adjusted to respect risk limits based on your profile.");
        }
        
        explanation.append("\n\nCompliance:\n");
        Boolean compliant = (Boolean) complianceResult.get("compliant");
        if (compliant != null && compliant) {
            explanation.append("The portfolio is compliant with regulatory requirements and risk limits.");
        } else {
            List<PortfolioResponse.ComplianceViolation> violations = 
                    (List<PortfolioResponse.ComplianceViolation>) complianceResult.get("violations");
            if (violations != null && !violations.isEmpty()) {
                explanation.append("Please review the following compliance issues:\n");
                for (PortfolioResponse.ComplianceViolation violation : violations) {
                    explanation.append("- ").append(violation.getMessage()).append("\n");
                }
            }
        }
        
        return explanation.toString();
    }
    
    /**
     * Call HuggingFace Inference API (FREE)
     * Uses models like meta-llama/Llama-2-7b-chat-hf or mistralai/Mistral-7B-Instruct-v0.1
     */
    private String callHuggingFaceAPI(String prompt) {
        try {
            // Use HuggingFace Inference API with a free model
            // Using google/flan-t5-large as it's free and available
            String apiUrl = "https://api-inference.huggingface.co/models/google/flan-t5-large";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(huggingfaceApiKey);
            
            // Format prompt for the model
            String formattedPrompt = "You are a financial advisor. Explain the following portfolio allocation in simple terms (2-3 paragraphs):\n\n" 
                + prompt;
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("inputs", formattedPrompt);
            requestBody.put("parameters", Map.of(
                "max_length", 500,
                "temperature", 0.7,
                "return_full_text", false
            ));
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // HuggingFace returns different formats depending on the model
                Object body = response.getBody();
                
                // Try array format first
                if (body instanceof List) {
                    List<Map<String, Object>> results = (List<Map<String, Object>>) body;
                    if (results != null && !results.isEmpty()) {
                        Object generatedTextObj = results.get(0).get("generated_text");
                        if (generatedTextObj != null) {
                            String generatedText = generatedTextObj.toString().trim();
                            return generatedText;
                        }
                    }
                }
                // Try direct map format
                else if (body instanceof Map) {
                    Map<String, Object> result = (Map<String, Object>) body;
                    Object generatedTextObj = result.get("generated_text");
                    if (generatedTextObj != null) {
                        String generatedText = generatedTextObj.toString().trim();
                        return generatedText;
                    }
                }
            }
            
            logger.warn("HuggingFace API returned unexpected response");
            return null;
        } catch (Exception e) {
            logger.error("Error calling HuggingFace API: {}", e.getMessage(), e);
            return null;
        }
    }
}

