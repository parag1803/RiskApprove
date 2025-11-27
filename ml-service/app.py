"""
ML Service - RiskApprove
Fetches historical stock data from Yahoo Finance and computes technical indicators
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import yfinance as yf
import pandas as pd
import numpy as np
from datetime import datetime, timedelta
import logging
import os

app = Flask(__name__)
CORS(app)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def calculate_sma(data, window):
    """Calculate Simple Moving Average"""
    return data.rolling(window=window).mean()


def generate_mock_prediction(symbol):
    """
    Generate mock prediction data when yfinance fails
    This allows the system to function for demonstration purposes
    """
    import random
    
    # Generate realistic-looking mock data
    base_return = random.uniform(0.05, 0.15)  # 5-15% annual return
    base_volatility = random.uniform(0.15, 0.35)  # 15-35% volatility
    risk_score = min(100, max(0, base_volatility * 200))  # Scale to 0-100
    
    # Random trend
    trends = ["BULLISH", "BEARISH", "NEUTRAL"]
    trend = random.choice(trends)
    
    # Signal based on trend
    if trend == "BULLISH":
        signal = "BUY"
    elif trend == "BEARISH":
        signal = "SELL"
    else:
        signal = "HOLD"
    
    # Mock price
    mock_price = random.uniform(50, 200)
    
    return {
        "symbol": symbol,
        "expectedReturn": float(base_return),
        "volatility": float(base_volatility),
        "riskScore": float(risk_score),
        "trend": trend,
        "signal": signal,
        "currentPrice": float(mock_price),
        "sma20": float(mock_price * 0.98),
        "sma50": float(mock_price * 0.95),
        "dataPoints": 252,
        "note": "Mock data - yfinance unavailable"
    }


def calculate_technical_indicators(symbol, period_days=252):
    """
    Fetch stock data and calculate technical indicators
    
    Args:
        symbol: Stock ticker symbol
        period_days: Number of days of historical data to fetch
    
    Returns:
        Dictionary with predictions and metrics
    """
    try:
        # Fetch historical data - try multiple methods
        ticker = yf.Ticker(symbol)
        hist = None
        
        # Try using period parameter first (more reliable)
        try:
            hist = ticker.history(period="1y", interval='1d')
            if hist.empty:
                raise ValueError("Empty data with period")
        except Exception as e1:
            logger.info(f"Period method failed for {symbol}, trying date range: {str(e1)}")
            try:
                # Fallback to date range
                end_date = datetime.now()
                start_date = end_date - timedelta(days=period_days + 50)
                hist = ticker.history(start=start_date, end=end_date, interval='1d')
            except Exception as e2:
                logger.error(f"Date range method also failed for {symbol}: {str(e2)}")
                # Last resort: try with a shorter period
                try:
                    hist = ticker.history(period="6mo", interval='1d')
                except Exception as e3:
                    logger.error(f"All methods failed for {symbol}: {str(e3)}")
                    return None
        
        if hist is None or hist.empty:
            logger.warning(f"No data found for {symbol}, using mock data")
            return generate_mock_prediction(symbol)
        
        # Calculate daily returns
        hist['DailyReturn'] = hist['Close'].pct_change()
        
        # Calculate volatility (standard deviation of returns)
        volatility = hist['DailyReturn'].std() * np.sqrt(252)  # Annualized volatility
        
        # Calculate expected return (mean of daily returns)
        expected_return = hist['DailyReturn'].mean() * 252  # Annualized return
        
        # Calculate SMAs
        hist['SMA20'] = calculate_sma(hist['Close'], 20)
        hist['SMA50'] = calculate_sma(hist['Close'], 50)
        
        # Get latest values
        latest_close = hist['Close'].iloc[-1]
        latest_sma20 = hist['SMA20'].iloc[-1]
        latest_sma50 = hist['SMA50'].iloc[-1]
        
        # Trend classification
        if pd.isna(latest_sma20) or pd.isna(latest_sma50):
            trend = "NEUTRAL"
        elif latest_sma20 > latest_sma50:
            trend = "BULLISH"
        else:
            trend = "BEARISH"
        
        # Risk score (0-100, higher = riskier)
        risk_score = min(100, max(0, volatility * 100))
        
        # Signal generation
        if trend == "BULLISH" and expected_return > 0.05:
            signal = "BUY"
        elif trend == "BEARISH" or expected_return < -0.05:
            signal = "SELL"
        else:
            signal = "HOLD"
        
        return {
            "symbol": symbol,
            "expectedReturn": float(expected_return),
            "volatility": float(volatility),
            "riskScore": float(risk_score),
            "trend": trend,
            "signal": signal,
            "currentPrice": float(latest_close),
            "sma20": float(latest_sma20) if not pd.isna(latest_sma20) else None,
            "sma50": float(latest_sma50) if not pd.isna(latest_sma50) else None,
            "dataPoints": len(hist)
        }
    
    except Exception as e:
        logger.error(f"Error processing {symbol}: {str(e)}")
        # Fallback: Generate mock data for demonstration
        logger.warning(f"Using mock data for {symbol} due to yfinance failure")
        return generate_mock_prediction(symbol)


@app.route('/health', methods=['GET'])
def health():
    """Health check endpoint"""
    return jsonify({"status": "healthy", "service": "ml-service"}), 200


@app.route('/predict', methods=['POST'])
def predict():
    """
    Predict returns and risk metrics for a list of stocks
    
    Request body:
    {
        "stocks": ["AAPL", "GOOGL", "MSFT"]
    }
    """
    try:
        data = request.get_json()
        
        if not data or 'stocks' not in data:
            return jsonify({"error": "Missing 'stocks' field in request"}), 400
        
        stocks = data['stocks']
        
        if not isinstance(stocks, list) or len(stocks) == 0:
            return jsonify({"error": "Stocks must be a non-empty list"}), 400
        
        results = []
        for symbol in stocks:
            logger.info(f"Processing stock: {symbol}")
            prediction = calculate_technical_indicators(symbol)
            
            if prediction:
                results.append(prediction)
            else:
                results.append({
                    "symbol": symbol,
                    "error": "Failed to fetch or process data"
                })
        
        return jsonify({
            "predictions": results,
            "timestamp": datetime.now().isoformat()
        }), 200
    
    except Exception as e:
        logger.error(f"Error in /predict endpoint: {str(e)}")
        return jsonify({"error": str(e)}), 500


@app.route('/predict/single', methods=['POST'])
def predict_single():
    """
    Predict returns and risk metrics for a single stock
    
    Request body:
    {
        "symbol": "AAPL"
    }
    """
    try:
        data = request.get_json()
        
        if not data or 'symbol' not in data:
            return jsonify({"error": "Missing 'symbol' field in request"}), 400
        
        symbol = data['symbol']
        prediction = calculate_technical_indicators(symbol)
        
        if prediction:
            return jsonify(prediction), 200
        else:
            return jsonify({"error": f"Failed to process {symbol}"}), 404
    
    except Exception as e:
        logger.error(f"Error in /predict/single endpoint: {str(e)}")
        return jsonify({"error": str(e)}), 500


if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5001))
    app.run(host='0.0.0.0', port=port, debug=False)

