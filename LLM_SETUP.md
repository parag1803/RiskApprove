# RiskApprove - 100% FREE LLM Setup Guide

This project uses **ONLY FREE, open-source models**. No paid APIs (OpenAI, Gemini, Claude) are required!

## 🆓 FREE Components Used

| Component | Technology | Cost |
|-----------|------------|------|
| Embeddings | HuggingFace sentence-transformers | **FREE** (local) |
| Vector Store | FAISS | **FREE** (local) |
| LLM Explanations | HuggingFace Inference API | **FREE** (30k req/month) |
| LLM (Local) | Ollama | **FREE** (runs locally) |
| Stock Data | Yahoo Finance | **FREE** |

---

## Quick Start (Zero Configuration)

The project works **out of the box** with no API keys:

```bash
docker-compose up --build
```

This uses:
- ✅ Local HuggingFace embeddings (sentence-transformers/all-MiniLM-L6-v2)
- ✅ Enhanced template-based explanations (no LLM API needed)
- ✅ FAISS vector store (local)

---

## Option 1: HuggingFace Inference API (Recommended for Cloud)

Get better AI explanations with FREE HuggingFace API:

### Step 1: Get Free API Key
1. Go to [huggingface.co](https://huggingface.co)
2. Create a free account
3. Go to Settings → Access Tokens
4. Create a new token (read access is enough)

### Step 2: Set Environment Variable
```bash
export HUGGINGFACE_API_KEY=hf_xxxxxxxxxxxxxxxxxx
```

Or add to `.env` file:
```
HUGGINGFACE_API_KEY=hf_xxxxxxxxxxxxxxxxxx
```

### Step 3: Restart Services
```bash
docker-compose up --build
```

### Free Models Used:
- `mistralai/Mistral-7B-Instruct-v0.2` (best quality)
- `HuggingFaceH4/zephyr-7b-beta` (good alternative)
- `google/flan-t5-large` (faster, smaller)

**Free Tier Limits:** ~30,000 requests/month

---

## Option 2: Ollama (Best for Local/Private)

Run LLMs completely locally with Ollama - 100% free and private!

### Step 1: Enable Ollama in docker-compose.yml

Uncomment the ollama service:

```yaml
ollama:
  image: ollama/ollama:latest
  container_name: riskapprove-ollama
  ports:
    - "11434:11434"
  volumes:
    - ollama_data:/root/.ollama
  networks:
    - riskapprove-network
```

And uncomment the volume:
```yaml
volumes:
  ollama_data:
```

### Step 2: Start Services
```bash
docker-compose up --build
```

### Step 3: Pull a Model (one-time)
```bash
docker exec riskapprove-ollama ollama pull mistral
```

**Recommended Models:**
| Model | Size | Quality | Speed |
|-------|------|---------|-------|
| `mistral` | 4GB | Good | Fast |
| `llama2` | 4GB | Good | Fast |
| `mixtral` | 26GB | Excellent | Slower |
| `phi` | 1.6GB | OK | Very Fast |

### Step 4: Configure (Optional)
In `docker-compose.yml`:
```yaml
api-gateway:
  environment:
    - OLLAMA_URL=http://ollama:11434
    - OLLAMA_MODEL=mistral
```

---

## Architecture: How FREE LLMs are Used

```
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway                             │
│                                                              │
│  LLMExplanationService                                       │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ Priority 1: Ollama (local LLM)                          ││
│  │ Priority 2: HuggingFace Inference API (free cloud)      ││
│  │ Priority 3: Enhanced Template (no LLM needed)           ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                      RAG Service                             │
│                                                              │
│  Embeddings: sentence-transformers/all-MiniLM-L6-v2 (FREE)  │
│  Vector Store: FAISS (FREE, local)                          │
│  No LLM needed for document search!                         │
└─────────────────────────────────────────────────────────────┘
```

---

## Embedding Models (All FREE)

The RAG service uses HuggingFace sentence-transformers locally:

| Model | Size | Quality | Use Case |
|-------|------|---------|----------|
| `all-MiniLM-L6-v2` | 80MB | Good | Default, fast |
| `all-mpnet-base-v2` | 420MB | Better | Higher quality |
| `multi-qa-MiniLM-L6-cos-v1` | 80MB | Good | Q&A optimized |

Change model in `docker-compose.yml`:
```yaml
rag-service:
  environment:
    - EMBEDDING_MODEL=sentence-transformers/all-mpnet-base-v2
```

---

## Comparison: Free vs Paid

| Feature | FREE (This Project) | Paid (OpenAI) |
|---------|---------------------|---------------|
| Embeddings | HuggingFace (local) | OpenAI ada-002 |
| LLM | Ollama/HuggingFace | GPT-3.5/GPT-4 |
| Cost | $0/month | $20-100+/month |
| Privacy | 100% local option | Data sent to cloud |
| Quality | Good | Excellent |
| Setup | Easy | Easy |

---

## Troubleshooting

### No LLM explanation generated?
- Check if HuggingFace API key is set: `echo $HUGGINGFACE_API_KEY`
- The system falls back to template explanations (still works!)

### Ollama not connecting?
```bash
# Check if running
docker ps | grep ollama

# Check logs
docker logs riskapprove-ollama

# Pull model manually
docker exec -it riskapprove-ollama ollama pull mistral
```

### Slow embeddings?
- First run downloads the model (~80MB)
- Subsequent runs use cached model
- Use CPU by default; GPU speeds up significantly

---

## Summary

✅ **Zero cost** - No paid API subscriptions
✅ **Privacy** - Option to run 100% locally
✅ **Easy setup** - Works out of the box
✅ **Quality** - Modern open-source models
✅ **Scalable** - Add more models as needed
