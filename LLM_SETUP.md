# Free LLM Setup Guide

This project supports **FREE LLM** options for generating portfolio explanations!

## 🆓 Free LLM Options

### Option 1: HuggingFace Inference API (Recommended - FREE)

**HuggingFace offers a free tier** with access to powerful open-source models like:
- Mistral-7B-Instruct
- Llama-2-7b-chat
- And many others

#### Setup Steps:

1. **Create a free HuggingFace account:**
   - Go to https://huggingface.co/join
   - Sign up (completely free)

2. **Get your API token:**
   - Go to https://huggingface.co/settings/tokens
   - Click "New token"
   - Name it (e.g., "riskapprove")
   - Select "Read" permission
   - Copy the token

3. **Set the environment variable:**
   ```bash
   export HUGGINGFACE_API_KEY=your_token_here
   ```
   
   Or add to `.env` file:
   ```
   HUGGINGFACE_API_KEY=your_token_here
   ```

4. **Restart the API Gateway:**
   ```bash
   docker-compose restart api-gateway
   ```

### Option 2: OpenAI (Paid)

If you have an OpenAI API key, you can use GPT-3.5-turbo:
```bash
export OPENAI_API_KEY=your_openai_key_here
```

### Option 3: Template-Based (No API Key Needed)

If no API keys are provided, the system automatically falls back to template-based explanations (still works, just not AI-generated).

## 🎯 Priority Order

The system tries LLMs in this order:
1. **HuggingFace** (if `HUGGINGFACE_API_KEY` is set) - FREE
2. **OpenAI** (if `OPENAI_API_KEY` is set) - Paid
3. **Template-based** (if no keys) - Always works

## 📝 Example Usage

Once you set `HUGGINGFACE_API_KEY`, the system will automatically:
- Use HuggingFace's free inference API
- Generate AI-powered explanations
- Fall back gracefully if the API is unavailable

## 🔍 Testing

After setting the API key, test it:
```bash
curl -X POST http://localhost:8080/api/portfolio/generate \
  -H "Content-Type: application/json" \
  -d '{
    "budget": 10000,
    "riskProfile": "MEDIUM",
    "stocks": ["AAPL", "GOOGL"],
    "investmentHorizon": 12
  }'
```

Check the `aiExplanation` field in the response - it should contain AI-generated text!

## 💡 Tips

- HuggingFace free tier has rate limits but is sufficient for testing/demos
- The system automatically handles API failures and falls back
- No credit card required for HuggingFace free tier

