"""
RAG Compliance Engine - RiskApprove
Uses ONLY FREE open-source models for embeddings and compliance checking

FREE Models Used:
- Embeddings: sentence-transformers/all-MiniLM-L6-v2 (local, free)
- Vector Store: FAISS (local, free)
- No paid APIs required!
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import os
import logging
from pathlib import Path
from typing import List, Dict
import json

# LangChain imports
try:
    from langchain_community.document_loaders import PyPDFLoader
    from langchain.text_splitter import RecursiveCharacterTextSplitter
    from langchain_community.embeddings import HuggingFaceEmbeddings
    from langchain_community.vectorstores import FAISS
    from langchain.schema import Document
except ImportError:
    # Fallback for older langchain versions
    from langchain.document_loaders import PyPDFLoader
    from langchain.text_splitter import RecursiveCharacterTextSplitter
    from langchain.embeddings import HuggingFaceEmbeddings
    from langchain.vectorstores import FAISS
    from langchain.schema import Document

app = Flask(__name__)
CORS(app)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Configuration
REGULATIONS_DIR = os.getenv('REGULATIONS_DIR', '/app/regulations')
EMBEDDINGS_DIR = os.getenv('EMBEDDINGS_DIR', '/app/embeddings')
CHUNK_SIZE = 400
CHUNK_OVERLAP = 80

# FREE embedding model options (no API key needed!)
EMBEDDING_MODEL = os.getenv('EMBEDDING_MODEL', 'sentence-transformers/all-MiniLM-L6-v2')

# Alternative FREE models you can use:
# - sentence-transformers/all-mpnet-base-v2 (better quality, larger)
# - sentence-transformers/paraphrase-MiniLM-L6-v2 (good for paraphrase)
# - sentence-transformers/multi-qa-MiniLM-L6-cos-v1 (optimized for Q&A)

# Global vector store
vector_store = None


def load_pdf_documents(directory: str) -> List[Document]:
    """Load all PDF and text files from the regulations directory"""
    documents = []
    reg_dir = Path(directory)
    
    if not reg_dir.exists():
        logger.warning(f"Regulations directory {directory} does not exist")
        return documents
    
    # Load PDF files
    pdf_files = list(reg_dir.glob('*.pdf'))
    logger.info(f"Found {len(pdf_files)} PDF files")
    
    for pdf_file in pdf_files:
        try:
            logger.info(f"Loading PDF: {pdf_file.name}")
            loader = PyPDFLoader(str(pdf_file))
            docs = loader.load()
            documents.extend(docs)
        except Exception as e:
            logger.error(f"Error loading {pdf_file}: {str(e)}")
    
    # Also load text files for easier testing
    txt_files = list(reg_dir.glob('*.txt'))
    logger.info(f"Found {len(txt_files)} text files")
    
    for txt_file in txt_files:
        try:
            logger.info(f"Loading text file: {txt_file.name}")
            with open(txt_file, 'r', encoding='utf-8') as f:
                content = f.read()
                doc = Document(
                    page_content=content,
                    metadata={"source": str(txt_file)}
                )
                documents.append(doc)
        except Exception as e:
            logger.error(f"Error loading {txt_file}: {str(e)}")
    
    return documents


def initialize_embeddings():
    """
    Initialize FREE HuggingFace embeddings model
    Runs completely locally - no API key or internet needed after download!
    """
    logger.info(f"Using FREE HuggingFace embeddings: {EMBEDDING_MODEL}")
    
    # This downloads the model once and caches it locally
    return HuggingFaceEmbeddings(
        model_name=EMBEDDING_MODEL,
        model_kwargs={'device': 'cpu'},  # Use 'cuda' if GPU available
        encode_kwargs={'normalize_embeddings': True}
    )


def create_vector_store():
    """Create or load FAISS vector store from PDF documents"""
    global vector_store
    
    embeddings_dir = Path(EMBEDDINGS_DIR)
    embeddings_dir.mkdir(parents=True, exist_ok=True)
    
    index_path = embeddings_dir / "faiss_index"
    
    # Check if vector store already exists
    if index_path.exists() and (index_path / "index.faiss").exists():
        logger.info("Loading existing vector store")
        try:
            embeddings = initialize_embeddings()
            vector_store = FAISS.load_local(
                str(index_path), 
                embeddings,
                allow_dangerous_deserialization=True  # Safe since we created it
            )
            logger.info("Vector store loaded successfully")
            return
        except Exception as e:
            logger.warning(f"Error loading vector store: {str(e)}. Recreating...")
    
    # Create new vector store
    logger.info("Creating new vector store from documents")
    documents = load_pdf_documents(REGULATIONS_DIR)
    
    if not documents:
        logger.warning("No documents found. Creating with sample regulation.")
        # Create a helpful sample document
        sample_content = """
        Sample Investment Regulation Guidelines
        
        Section 1: Risk Profile Guidelines
        
        1.1 Low Risk Profile:
        - Maximum allocation to high-risk assets: 20% of total portfolio
        - Maximum single stock concentration: 15% of total portfolio
        - Recommended investment horizon: Minimum 24 months
        
        1.2 Medium Risk Profile:
        - Maximum allocation to high-risk assets: 40% of total portfolio
        - Maximum single stock concentration: 25% of total portfolio
        - Recommended investment horizon: Minimum 12 months
        
        1.3 High Risk Profile:
        - Maximum allocation to high-risk assets: 70% of total portfolio
        - Maximum single stock concentration: 35% of total portfolio
        - Recommended investment horizon: Minimum 6 months
        
        Section 2: Portfolio Concentration Limits
        
        2.1 Diversification Requirements:
        - Portfolios should contain minimum 3 different securities
        - No single sector should exceed 50% of total allocation
        - Maximum single stock concentration varies by risk profile
        
        Section 3: Compliance Requirements
        
        3.1 All portfolios must be reviewed for:
        - Concentration risk
        - Sector exposure
        - Risk profile alignment
        - Regulatory compliance
        """
        documents = [Document(page_content=sample_content, metadata={"source": "sample_regulation.txt"})]
    
    # Split documents into chunks
    text_splitter = RecursiveCharacterTextSplitter(
        chunk_size=CHUNK_SIZE,
        chunk_overlap=CHUNK_OVERLAP,
        length_function=len,
    )
    
    chunks = text_splitter.split_documents(documents)
    logger.info(f"Created {len(chunks)} document chunks")
    
    # Create embeddings and vector store (FREE!)
    embeddings = initialize_embeddings()
    vector_store = FAISS.from_documents(chunks, embeddings)
    
    # Save vector store for future use
    vector_store.save_local(str(index_path))
    logger.info(f"Vector store saved to {index_path}")


def check_compliance(portfolio: Dict, risk_profile: str, risk_metrics: Dict) -> Dict:
    """
    Check portfolio compliance against regulations using RAG
    Uses FREE local embeddings for similarity search
    """
    if vector_store is None:
        return {
            "compliant": True,
            "violations": [],
            "warnings": ["Vector store not initialized"],
            "citations": []
        }
    
    # Build query from portfolio and risk profile
    query_parts = [
        f"Investment portfolio compliance check",
        f"Risk profile: {risk_profile}",
        f"Portfolio allocation percentages: {json.dumps(portfolio)}",
        "concentration limits single stock maximum allocation"
    ]
    query = " ".join(query_parts)
    
    # Search for relevant regulations using FREE local embeddings
    try:
        docs = vector_store.similarity_search_with_score(query, k=5)
        
        violations = []
        warnings = []
        citations = []
        
        # Analyze retrieved documents for compliance rules
        for doc, score in docs:
            content = doc.page_content
            source = doc.metadata.get('source', 'Unknown')
            
            citations.append({
                "text": content[:500],
                "source": Path(source).name if source != 'Unknown' else source,
                "relevance_score": float(1 / (1 + score))  # Convert distance to similarity
            })
            
            content_lower = content.lower()
            
            # Check for concentration limits based on risk profile
            max_allocation = max(portfolio.values()) if portfolio else 0
            
            if "concentration" in content_lower or "single stock" in content_lower:
                # Risk profile specific thresholds
                thresholds = {
                    "LOW": 0.15,
                    "MEDIUM": 0.25,
                    "HIGH": 0.35
                }
                threshold = thresholds.get(risk_profile.upper(), 0.25)
                
                if max_allocation > threshold:
                    violations.append({
                        "type": "CONCENTRATION",
                        "message": f"Single stock allocation ({max_allocation*100:.1f}%) exceeds {risk_profile} risk profile limit ({threshold*100:.0f}%)",
                        "severity": "HIGH",
                        "source": Path(source).name
                    })
            
            # Check for high-risk asset limits
            if risk_profile.upper() == "LOW" and "high risk" in content_lower:
                if "limit" in content_lower or "maximum" in content_lower:
                    warnings.append({
                        "type": "RISK_LIMIT",
                        "message": "Low risk profile has restrictions on high-risk assets (max 20%)",
                        "source": Path(source).name
                    })
        
        # Rule-based checks (always applied)
        total_allocation = sum(portfolio.values()) if portfolio else 0
        if abs(total_allocation - 1.0) > 0.01:
            violations.append({
                "type": "ALLOCATION",
                "message": f"Total allocation ({total_allocation*100:.1f}%) does not equal 100%",
                "severity": "MEDIUM",
                "source": "System"
            })
        
        # Minimum diversification check
        if len(portfolio) < 3:
            warnings.append({
                "type": "DIVERSIFICATION",
                "message": f"Portfolio has only {len(portfolio)} stocks. Consider adding more for diversification.",
                "source": "System"
            })
        
        # Risk profile specific checks
        if risk_profile.upper() == "LOW":
            high_risk_stocks = [
                symbol for symbol, metrics in risk_metrics.items()
                if isinstance(metrics, dict) and metrics.get('riskScore', 0) > 70
            ]
            if high_risk_stocks:
                warnings.append({
                    "type": "RISK_PROFILE",
                    "message": f"Low risk profile may not be suitable for high-risk stocks: {', '.join(high_risk_stocks)}",
                    "source": "System"
                })
        
        # Remove duplicate violations
        seen_messages = set()
        unique_violations = []
        for v in violations:
            if v['message'] not in seen_messages:
                seen_messages.add(v['message'])
                unique_violations.append(v)
        
        return {
            "compliant": len(unique_violations) == 0,
            "violations": unique_violations,
            "warnings": warnings,
            "citations": citations[:3]  # Top 3 most relevant
        }
    
    except Exception as e:
        logger.error(f"Error in compliance check: {str(e)}")
        return {
            "compliant": False,
            "violations": [{"type": "SYSTEM_ERROR", "message": str(e), "severity": "HIGH", "source": "System"}],
            "warnings": [],
            "citations": []
        }


@app.route('/health', methods=['GET'])
def health():
    """Health check endpoint"""
    status = "healthy" if vector_store is not None else "initializing"
    return jsonify({
        "status": status, 
        "service": "rag-service",
        "embedding_model": EMBEDDING_MODEL,
        "cost": "FREE (no paid APIs)"
    }), 200


@app.route('/compliance/check', methods=['POST'])
def compliance_check():
    """
    Check portfolio compliance against regulations
    Uses FREE local embeddings - no API costs!
    """
    try:
        data = request.get_json()
        
        if not data:
            return jsonify({"error": "Missing request body"}), 400
        
        portfolio = data.get('portfolio', {})
        risk_profile = data.get('riskProfile', 'MEDIUM')
        risk_metrics = data.get('riskMetrics', {})
        
        result = check_compliance(portfolio, risk_profile, risk_metrics)
        
        return jsonify(result), 200
    
    except Exception as e:
        logger.error(f"Error in /compliance/check: {str(e)}")
        return jsonify({"error": str(e)}), 500


@app.route('/regulations/reload', methods=['POST'])
def reload_regulations():
    """Reload regulations and rebuild vector store"""
    try:
        create_vector_store()
        return jsonify({
            "message": "Regulations reloaded successfully",
            "embedding_model": EMBEDDING_MODEL
        }), 200
    except Exception as e:
        logger.error(f"Error reloading regulations: {str(e)}")
        return jsonify({"error": str(e)}), 500


@app.route('/info', methods=['GET'])
def info():
    """Service information - shows FREE status"""
    return jsonify({
        "service": "RAG Compliance Engine",
        "version": "2.0.0",
        "embedding_model": EMBEDDING_MODEL,
        "vector_store": "FAISS (local)",
        "cost": "100% FREE",
        "paid_apis": "NONE",
        "features": [
            "PDF/TXT document loading",
            "Semantic search with embeddings",
            "Compliance rule checking",
            "Citation retrieval"
        ]
    }), 200


if __name__ == '__main__':
    logger.info("=" * 50)
    logger.info("RAG Service - 100% FREE (No Paid APIs)")
    logger.info(f"Embedding Model: {EMBEDDING_MODEL}")
    logger.info("=" * 50)
    
    # Initialize on startup
    create_vector_store()
    
    port = int(os.environ.get('PORT', 5002))
    app.run(host='0.0.0.0', port=port, debug=False)
