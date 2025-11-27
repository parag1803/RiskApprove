# RiskApprove – AI-Powered Risk-Compliant Investment Advisor

A complete, production-quality, locally runnable microservices-based system for generating optimized, risk-controlled, compliance-validated investment portfolios using LLM, RAG, ML, and microservices architecture.

## 🏗️ Architecture

The system consists of multiple microservices working together:

### Backend Services (Java Spring Boot)
1. **Data Service** (Port 8081) - Receives stock requests, calls ML service, returns predictions
2. **Compliance Service** (Port 8082) - Performs rule-based validation and RAG-powered compliance checking
3. **Portfolio Allocation Service** (Port 8083) - Computes optimal portfolio weights
4. **API Gateway** (Port 8080) - Single entry point, orchestrates services, provides LLM explanations

### Python Microservices
1. **ML Service** (Port 5001) - Fetches Yahoo Finance data, computes technical indicators, returns predictions
2. **RAG Compliance Engine** (Port 5002) - Loads SEBI/RBI PDFs, creates embeddings, provides document-backed compliance

### Frontend
- **React + Tailwind Dashboard** (Port 3000) - Modern UI with forms, charts, and visualizations

### Infrastructure
- **MongoDB** (Port 27017) - Data persistence

## 🚀 Quick Start

### Prerequisites

- Docker and Docker Compose installed
- At least 8GB RAM available
- Internet connection (for downloading dependencies and stock data)

### Setup Instructions

1. **Clone or navigate to the project directory:**
   ```bash
   cd /Users/paragg/Desktop/ANNA
   ```

2. **Add SEBI/RBI Regulation PDFs (Optional but Recommended):**
   - Place PDF files in the `regulations/` directory
   - The RAG service will automatically load them on startup
   - Example: Download SEBI guidelines from https://www.sebi.gov.in/legal/acts/act.html

3. **Set Environment Variables (Optional):**
   - Create a `.env` file in the root directory (optional):
     ```env
     OPENAI_API_KEY=your_openai_api_key_here
     HUGGINGFACE_API_KEY=your_huggingface_key_here
     ```
   - If not provided, the system will use free HuggingFace embeddings

4. **Build and Start All Services:**
   ```bash
   docker-compose up --build
   ```
   
   This will:
   - Build all Docker images
   - Start all microservices
   - Initialize MongoDB
   - Set up the network between services

5. **Wait for Services to Start:**
   - The first build may take 10-15 minutes (downloading dependencies)
   - Watch the logs to ensure all services start successfully
   - You should see "Started" messages for each service

6. **Access the Application:**
   - Frontend: http://localhost:3000
   - API Gateway Swagger: http://localhost:8080/swagger-ui.html
   - Data Service Swagger: http://localhost:8081/swagger-ui.html
   - Compliance Service Swagger: http://localhost:8082/swagger-ui.html
   - Portfolio Service Swagger: http://localhost:8083/swagger-ui.html

## 📖 Usage Guide

### Using the Web Interface

1. **Open the Frontend:**
   - Navigate to http://localhost:3000

2. **Fill in the Portfolio Form:**
   - **Budget**: Enter your total investment budget (e.g., 10000)
   - **Risk Profile**: Select Low, Medium, or High
   - **Investment Horizon**: Enter duration in months (e.g., 12)
   - **Select Stocks**: Choose one or more stocks from the list

3. **Generate Portfolio:**
   - Click "Generate Portfolio"
   - Wait for the system to:
     - Fetch stock data from Yahoo Finance
     - Calculate technical indicators
     - Optimize portfolio allocation
     - Check compliance
     - Generate AI explanation

4. **Review Results:**
   - **Portfolio Summary Cards**: Total return, risk, compliance status
   - **Pie Chart**: Visual allocation breakdown
   - **Bar Chart**: Risk vs expected returns comparison
   - **Stock Details Table**: Complete breakdown per stock
   - **Compliance Warnings**: Any violations or warnings
   - **Regulatory Citations**: Relevant SEBI/RBI rules
   - **AI Explanation**: Beginner-friendly explanation

### Using the API Directly

#### Generate Portfolio
```bash
curl -X POST http://localhost:8080/api/portfolio/generate \
  -H "Content-Type: application/json" \
  -d '{
    "budget": 10000,
    "riskProfile": "MEDIUM",
    "stocks": ["AAPL", "GOOGL", "MSFT"],
    "investmentHorizon": 12
  }'
```

#### Get Stock Predictions
```bash
curl -X POST http://localhost:8081/api/data/predictions \
  -H "Content-Type: application/json" \
  -d '{
    "stocks": ["AAPL", "GOOGL"]
  }'
```

#### Check Compliance
```bash
curl -X POST http://localhost:8082/api/compliance/check \
  -H "Content-Type: application/json" \
  -d '{
    "portfolio": {"AAPL": 0.3, "GOOGL": 0.4, "MSFT": 0.3},
    "riskProfile": "MEDIUM",
    "riskMetrics": {
      "AAPL": {"riskScore": 45, "volatility": 0.25},
      "GOOGL": {"riskScore": 50, "volatility": 0.28}
    }
  }'
```

## 🔧 Adding New SEBI PDF Rules

1. **Download SEBI/RBI Regulation PDFs:**
   - Visit https://www.sebi.gov.in/legal/acts/act.html
   - Download relevant PDFs

2. **Place PDFs in Regulations Directory:**
   ```bash
   cp /path/to/sebi-regulation.pdf regulations/
   ```

3. **Reload Regulations:**
   - Option 1: Restart the RAG service:
     ```bash
     docker-compose restart rag-service
     ```
   - Option 2: Call the reload API:
     ```bash
     curl -X POST http://localhost:5002/regulations/reload
     ```

4. **Verify:**
   - Check RAG service logs to see loaded documents
   - Test compliance check to see citations from new documents

## 🧪 Testing the System

### Example Test Cases

#### Test 1: Low Risk Portfolio
```json
{
  "budget": 5000,
  "riskProfile": "LOW",
  "stocks": ["JNJ", "PG", "WMT"],
  "investmentHorizon": 24
}
```

#### Test 2: High Risk Portfolio
```json
{
  "budget": 20000,
  "riskProfile": "HIGH",
  "stocks": ["TSLA", "NVDA", "META"],
  "investmentHorizon": 6
}
```

#### Test 3: Medium Risk Diversified Portfolio
```json
{
  "budget": 15000,
  "riskProfile": "MEDIUM",
  "stocks": ["AAPL", "GOOGL", "MSFT", "AMZN", "V"],
  "investmentHorizon": 12
}
```

### Health Checks

Check service health:
```bash
curl http://localhost:8080/api/health
curl http://localhost:8081/api/data/health
curl http://localhost:8082/api/compliance/health
curl http://localhost:8083/api/portfolio/health
curl http://localhost:5001/health
curl http://localhost:5002/health
```

## 📁 Project Structure

```
RiskApprove/
├── docker-compose.yml          # Orchestration configuration
├── README.md                   # This file
├── api-gateway/                # API Gateway service
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── data-service/               # Data Service
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── compliance-service/         # Compliance Service
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── portfolio-service/          # Portfolio Allocation Service
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── ml-service/                 # Python ML Service
│   ├── app.py
│   ├── requirements.txt
│   └── Dockerfile
├── rag-service/                # Python RAG Service
│   ├── app.py
│   ├── requirements.txt
│   └── Dockerfile
├── frontend/                   # React Frontend
│   ├── src/
│   ├── package.json
│   ├── Dockerfile
│   └── nginx.conf
└── regulations/                # SEBI/RBI PDFs directory
    └── .gitkeep
```

## 🔍 Key Features

### 1. ML-Powered Stock Analysis
- Fetches real-time data from Yahoo Finance
- Calculates technical indicators:
  - Daily returns
  - Volatility (annualized standard deviation)
  - Expected return (annualized mean)
  - Trend classification (SMA20 vs SMA50)
  - Bullish/Bearish signals

### 2. Portfolio Optimization
- Proportional return-based weighting
- Risk-adjusted allocation
- Respects user risk profile constraints
- Maximum single-stock limits
- High-risk asset caps

### 3. Compliance Engine
- **Rule-Based Validation:**
  - Total allocation checks
  - Single-stock concentration limits
  - High-risk asset limits
  - Risk profile-specific rules

- **RAG-Powered Document Analysis:**
  - Loads SEBI/RBI regulation PDFs
  - Creates embeddings (OpenAI or HuggingFace)
  - FAISS vector store for fast retrieval
  - Returns relevant citations and violations

### 4. AI Explanation Layer
- Generates beginner-friendly explanations
- Explains stock selection rationale
- Describes risk balancing strategy
- Highlights compliance status
- Uses template-based or LLM-generated explanations

### 5. Modern Frontend
- Clean, professional UI with Tailwind CSS
- Interactive forms with validation
- Visual charts (Pie, Bar)
- Real-time loading states
- Error handling
- Responsive design

## 🛠️ Development

### Running Individual Services

#### Java Services
```bash
cd data-service
mvn spring-boot:run
```

#### Python Services
```bash
cd ml-service
pip install -r requirements.txt
python app.py
```

#### Frontend
```bash
cd frontend
npm install
npm start
```

### Building Docker Images

```bash
# Build individual service
docker build -t riskapprove-data-service ./data-service

# Build all services
docker-compose build
```

### Viewing Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f api-gateway
docker-compose logs -f ml-service
docker-compose logs -f rag-service
```

### Stopping Services

```bash
docker-compose down

# Remove volumes (clears MongoDB data)
docker-compose down -v
```

## 🔐 Configuration

### Environment Variables

- `OPENAI_API_KEY`: For OpenAI embeddings (optional)
- `HUGGINGFACE_API_KEY`: For HuggingFace API (optional)
- `MONGODB_HOST`: MongoDB host (default: mongodb)
- `MONGODB_PORT`: MongoDB port (default: 27017)

### Service URLs

All service URLs are configured in `docker-compose.yml` and can be overridden via environment variables.

## 📊 API Documentation

Swagger UI is available for all Java services:
- API Gateway: http://localhost:8080/swagger-ui.html
- Data Service: http://localhost:8081/swagger-ui.html
- Compliance Service: http://localhost:8082/swagger-ui.html
- Portfolio Service: http://localhost:8083/swagger-ui.html

## 🐛 Troubleshooting

### Services Not Starting

1. **Check Docker resources:**
   - Ensure Docker has enough memory (8GB+ recommended)
   - Check disk space

2. **Check logs:**
   ```bash
   docker-compose logs
   ```

3. **Rebuild services:**
   ```bash
   docker-compose up --build --force-recreate
   ```

### ML Service Errors

- **Yahoo Finance API issues:** The service may fail if Yahoo Finance is unavailable
- **Stock symbol errors:** Ensure valid stock symbols (e.g., AAPL, not APPL)

### RAG Service Issues

- **No PDFs loaded:** Place PDFs in `regulations/` directory
- **Embedding errors:** Check API keys if using OpenAI
- **FAISS errors:** The service will recreate the index if corrupted

### Frontend Not Loading

- **Check API URL:** Ensure `REACT_APP_API_URL` is correct
- **CORS issues:** All services have CORS enabled
- **Network issues:** Ensure API Gateway is running on port 8080

### MongoDB Connection Issues

- **Check MongoDB logs:**
  ```bash
  docker-compose logs mongodb
  ```
- **Verify network:** All services should be on `riskapprove-network`

## 📝 Notes

- The system uses free HuggingFace embeddings by default (no API key required)
- OpenAI API key is optional but recommended for better embeddings
- Stock data is fetched in real-time from Yahoo Finance
- First-time startup may take longer due to dependency downloads
- MongoDB data persists in Docker volumes

## 🎯 Future Enhancements

- Real-time stock price updates
- Historical portfolio tracking
- User authentication and portfolio history
- Advanced ML models (LSTM, Transformer)
- More sophisticated portfolio optimization algorithms
- Integration with actual trading APIs
- Email notifications for compliance violations

## 📄 License

This project is for educational and demonstration purposes.

## 🤝 Contributing

This is a demonstration project. For production use, consider:
- Adding authentication and authorization
- Implementing rate limiting
- Adding comprehensive error handling
- Setting up monitoring and logging
- Implementing caching strategies
- Adding unit and integration tests

---

**Built with ❤️ using Spring Boot, Flask, React, and Docker**

