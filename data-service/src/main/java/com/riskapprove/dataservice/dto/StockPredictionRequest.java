package com.riskapprove.dataservice.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class StockPredictionRequest {
    @NotEmpty(message = "Stocks list cannot be empty")
    private List<String> stocks;

    public List<String> getStocks() {
        return stocks;
    }

    public void setStocks(List<String> stocks) {
        this.stocks = stocks;
    }
}

