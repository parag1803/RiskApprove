# Regulations Directory

Place SEBI (Securities and Exchange Board of India) and RBI (Reserve Bank of India) regulation PDFs in this directory.

## How to Add Regulations

1. Download PDF files from:
   - SEBI: https://www.sebi.gov.in/legal/acts/act.html
   - RBI: https://www.rbi.org.in/scripts/BS_ViewMasCirculardetails.aspx

2. Copy PDF files to this directory:
   ```bash
   cp /path/to/sebi-regulation.pdf regulations/
   ```

3. Restart the RAG service:
   ```bash
   docker-compose restart rag-service
   ```

   Or call the reload API:
   ```bash
   curl -X POST http://localhost:5002/regulations/reload
   ```

## Supported Format

- PDF files (.pdf)
- Files will be automatically processed on service startup
- Chunk size: 400 tokens with 80 token overlap
- Embeddings stored in FAISS vector store

## Example Files

You can add files like:
- SEBI Investment Advisers Regulations
- SEBI Portfolio Managers Regulations
- RBI Guidelines on Investment Products
- Any other relevant financial regulations

