"""
RAG Compliance Engine - RiskApprove
Loads SEBI/RBI regulation PDFs, creates embeddings, and provides compliance checking
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
    from langchain_openai import OpenAIEmbeddings
    from langchain_community.vectorstores import FAISS
    from langchain.schema import Document
except ImportError:
    # Fallback for older langchain versions
    from langchain.document_loaders import PyPDFLoader
    from langchain.text_splitter import RecursiveCharacterTextSplitter
    from langchain.embeddings import OpenAIEmbeddings, HuggingFaceEmbeddings
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
OPENAI_API_KEY = os.getenv('OPENAI_API_KEY')
HUGGINGFACE_API_KEY = os.getenv('HUGGINGFACE_API_KEY')

# Global vector store
vector_store = None


def load_pdf_documents(directory: str) -> List[Document]:
    """Load all PDF files from the regulations directory"""
    documents = []
    pdf_dir = Path(directory)
    
    if not pdf_dir.exists():
        logger.warning(f"Regulations directory {directory} does not exist")
        return documents
    
    pdf_files = list(pdf_dir.glob('*.pdf'))
    logger.info(f"Found {len(pdf_files)} PDF files")
    
    for pdf_file in pdf_files:
        try:
            logger.info(f"Loading {pdf_file.name}")
            loader = PyPDFLoader(str(pdf_file))
            docs = loader.load()
            documents.extend(docs)
        except Exception as e:
            logger.error(f"Error loading {pdf_file}: {str(e)}")
    
    return documents


def initialize_embeddings():
    """Initialize embeddings model (OpenAI or HuggingFace)"""
    if OPENAI_API_KEY:
        logger.info("Using OpenAI embeddings")
        return OpenAIEmbeddings(openai_api_key=OPENAI_API_KEY)
    else:
        logger.info("Using HuggingFace embeddings (sentence-transformers)")
        # Use a free, local embedding model
        return HuggingFaceEmbeddings(
            model_name="sentence-transformers/all-MiniLM-L6-v2"
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
            vector_store = FAISS.load_local(str(index_path), embeddings)
            logger.info("Vector store loaded successfully")
            return
        except Exception as e:
            logger.warning(f"Error loading vector store: {str(e)}. Recreating...")
    
    # Create new vector store
    logger.info("Creating new vector store from PDFs")
    documents = load_pdf_documents(REGULATIONS_DIR)
    
    if not documents:
        logger.warning("No documents found. Creating empty vector store.")
        # Create a dummy document to initialize the store
        documents = [Document(page_content="No regulations loaded.")]
    
    # Split documents into chunks
    text_splitter = RecursiveCharacterTextSplitter(
        chunk_size=CHUNK_SIZE,
        chunk_overlap=CHUNK_OVERLAP,
        length_function=len,
    )
    
    chunks = text_splitter.split_documents(documents)
    logger.info(f"Created {len(chunks)} document chunks")
    
    # Create embeddings and vector store
    embeddings = initialize_embeddings()
    vector_store = FAISS.from_documents(chunks, embeddings)
    
    # Save vector store
    vector_store.save_local(str(index_path))
    logger.info(f"Vector store saved to {index_path}")


def check_compliance(portfolio: Dict, risk_profile: str, risk_metrics: Dict) -> Dict:
    """
    Check portfolio compliance against regulations using RAG
    
    Args:
        portfolio: Portfolio allocation dictionary
        risk_profile: User's risk profile (LOW, MEDIUM, HIGH)
        risk_metrics: Risk metrics from ML service
    
    Returns:
        Compliance check results with violations and citations
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
        f"Risk profile: {risk_profile}",
        f"Portfolio allocation: {json.dumps(portfolio)}",
        f"Risk metrics: {json.dumps(risk_metrics)}"
    ]
    query = " ".join(query_parts)
    
    # Search for relevant regulations
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
                "text": content[:500],  # First 500 chars
                "source": Path(source).name if source != 'Unknown' else source,
                "relevance_score": float(score)
            })
            
            # Simple rule extraction (can be enhanced with LLM)
            content_lower = content.lower()
            
            # Check for high-risk asset limits
            if risk_profile.upper() == "LOW" and "high risk" in content_lower:
                if "limit" in content_lower or "maximum" in content_lower:
                    warnings.append({
                        "type": "RISK_LIMIT",
                        "message": "Low risk profile may have restrictions on high-risk assets",
                        "source": Path(source).name
                    })
            
            # Check for concentration limits
            if "concentration" in content_lower or "single stock" in content_lower:
                max_allocation = max(portfolio.values()) if portfolio else 0
                if max_allocation > 0.25:  # 25% threshold
                    violations.append({
                        "type": "CONCENTRATION",
                        "message": f"Single stock allocation ({max_allocation*100:.1f}%) may exceed concentration limits",
                        "severity": "HIGH",
                        "source": Path(source).name
                    })
        
        # Rule-based checks
        total_allocation = sum(portfolio.values()) if portfolio else 0
        if abs(total_allocation - 1.0) > 0.01:
            violations.append({
                "type": "ALLOCATION",
                "message": f"Total allocation ({total_allocation*100:.1f}%) does not equal 100%",
                "severity": "MEDIUM",
                "source": "System"
            })
        
        # Risk profile specific checks
        if risk_profile.upper() == "LOW":
            high_risk_stocks = [
                symbol for symbol, metrics in risk_metrics.items()
                if metrics.get('riskScore', 0) > 70
            ]
            if high_risk_stocks:
                warnings.append({
                    "type": "RISK_PROFILE",
                    "message": f"Low risk profile may not be suitable for high-risk stocks: {', '.join(high_risk_stocks)}",
                    "source": "System"
                })
        
        return {
            "compliant": len(violations) == 0,
            "violations": violations,
            "warnings": warnings,
            "citations": citations[:3]  # Top 3 most relevant
        }
    
    except Exception as e:
        logger.error(f"Error in compliance check: {str(e)}")
        return {
            "compliant": False,
            "violations": [{"type": "SYSTEM_ERROR", "message": str(e)}],
            "warnings": [],
            "citations": []
        }


@app.route('/health', methods=['GET'])
def health():
    """Health check endpoint"""
    status = "healthy" if vector_store is not None else "initializing"
    return jsonify({"status": status, "service": "rag-service"}), 200


@app.route('/compliance/check', methods=['POST'])
def compliance_check():
    """
    Check portfolio compliance against regulations
    
    Request body:
    {
        "portfolio": {"AAPL": 0.3, "GOOGL": 0.4, "MSFT": 0.3},
        "riskProfile": "MEDIUM",
        "riskMetrics": {
            "AAPL": {"riskScore": 45, "volatility": 0.25},
            "GOOGL": {"riskScore": 50, "volatility": 0.28}
        }
    }
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
        return jsonify({"message": "Regulations reloaded successfully"}), 200
    except Exception as e:
        logger.error(f"Error reloading regulations: {str(e)}")
        return jsonify({"error": str(e)}), 500


if __name__ == '__main__':
    # Initialize on startup
    create_vector_store()
    
    port = int(os.environ.get('PORT', 5002))
    app.run(host='0.0.0.0', port=port, debug=False)

