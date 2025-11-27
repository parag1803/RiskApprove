package com.riskapprove.portfolioservice.dto;

import java.util.Map;

public class AllocationResponse {
    private Map<String, Double> allocations; // Symbol -> weight percentage
    private Map<String, Double> amounts;    // Symbol -> dollar amount
    private Double totalExpectedReturn;
    private Double totalRisk;
    private String optimizationMethod;

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

    public String getOptimizationMethod() {
        return optimizationMethod;
    }

    public void setOptimizationMethod(String optimizationMethod) {
        this.optimizationMethod = optimizationMethod;
    }
}

