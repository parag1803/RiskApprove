import React, { useState } from 'react';
import axios from 'axios';
import PortfolioForm from './components/PortfolioForm';
import PortfolioResults from './components/PortfolioResults';
import LoadingSpinner from './components/LoadingSpinner';

// Use relative path so nginx can proxy to api-gateway
const API_URL = process.env.REACT_APP_API_URL || '';

function App() {
  const [portfolioData, setPortfolioData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (formData) => {
    setLoading(true);
    setError(null);
    setPortfolioData(null);

    try {
      const response = await axios.post(`${API_URL}/api/portfolio/generate`, formData);
      setPortfolioData(response.data);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to generate portfolio');
      console.error('Error generating portfolio:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleReset = () => {
    setPortfolioData(null);
    setError(null);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100">
      <div className="container mx-auto px-4 py-8">
        <header className="text-center mb-8">
          <h1 className="text-4xl font-bold text-gray-800 mb-2">
            RiskApprove
          </h1>
          <p className="text-lg text-gray-600">
            AI-Powered Risk-Compliant Investment Advisor
          </p>
        </header>

        {!portfolioData ? (
          <div className="max-w-2xl mx-auto">
            <PortfolioForm onSubmit={handleSubmit} />
            {loading && <LoadingSpinner />}
            {error && (
              <div className="mt-4 p-4 bg-red-100 border border-red-400 text-red-700 rounded-lg">
                <p className="font-semibold">Error:</p>
                <p>{error}</p>
              </div>
            )}
          </div>
        ) : (
          <PortfolioResults data={portfolioData} onReset={handleReset} />
        )}
      </div>
    </div>
  );
}

export default App;

