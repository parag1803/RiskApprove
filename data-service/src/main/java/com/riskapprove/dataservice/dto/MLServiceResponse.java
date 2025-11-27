package com.riskapprove.dataservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MLServiceResponse {
    private List<StockPrediction> predictions;
    private String timestamp;

    public List<StockPrediction> getPredictions() {
        return predictions;
    }

    public void setPredictions(List<StockPrediction> predictions) {
        this.predictions = predictions;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}

