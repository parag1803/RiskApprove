import React, { useState } from 'react';

const STOCKS = [
  'AAPL', 'GOOGL', 'MSFT', 'AMZN', 'TSLA', 'META', 'NVDA', 'JPM', 'V', 'JNJ',
  'WMT', 'PG', 'MA', 'DIS', 'NFLX', 'BAC', 'XOM', 'CSCO', 'PFE', 'INTC'
];

function PortfolioForm({ onSubmit }) {
  const [formData, setFormData] = useState({
    budget: '',
    riskProfile: 'MEDIUM',
    stocks: [],
    investmentHorizon: ''
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleStockToggle = (stock) => {
    setFormData(prev => ({
      ...prev,
      stocks: prev.stocks.includes(stock)
        ? prev.stocks.filter(s => s !== stock)
        : [...prev.stocks, stock]
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    
    if (formData.stocks.length === 0) {
      alert('Please select at least one stock');
      return;
    }

    const submitData = {
      budget: parseFloat(formData.budget),
      riskProfile: formData.riskProfile,
      stocks: formData.stocks,
      investmentHorizon: parseInt(formData.investmentHorizon)
    };

    onSubmit(submitData);
  };

  return (
    <div className="bg-white rounded-lg shadow-lg p-6">
      <h2 className="text-2xl font-bold text-gray-800 mb-6">Portfolio Configuration</h2>
      
      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Total Budget ($)
          </label>
          <input
            type="number"
            name="budget"
            value={formData.budget}
            onChange={handleChange}
            required
            min="100"
            step="100"
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            placeholder="e.g., 10000"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Risk Profile
          </label>
          <select
            name="riskProfile"
            value={formData.riskProfile}
            onChange={handleChange}
            required
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
          >
            <option value="LOW">Low Risk</option>
            <option value="MEDIUM">Medium Risk</option>
            <option value="HIGH">High Risk</option>
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Investment Horizon (months)
          </label>
          <input
            type="number"
            name="investmentHorizon"
            value={formData.investmentHorizon}
            onChange={handleChange}
            required
            min="1"
            max="120"
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
            placeholder="e.g., 12"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Select Stocks (Select multiple)
          </label>
          <div className="grid grid-cols-4 gap-2 max-h-64 overflow-y-auto border border-gray-300 rounded-lg p-4">
            {STOCKS.map(stock => (
              <label
                key={stock}
                className="flex items-center space-x-2 cursor-pointer hover:bg-gray-50 p-2 rounded"
              >
                <input
                  type="checkbox"
                  checked={formData.stocks.includes(stock)}
                  onChange={() => handleStockToggle(stock)}
                  className="w-4 h-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded"
                />
                <span className="text-sm font-medium">{stock}</span>
              </label>
            ))}
          </div>
          <p className="mt-2 text-sm text-gray-500">
            Selected: {formData.stocks.length} stock(s)
          </p>
        </div>

        <button
          type="submit"
          className="w-full bg-indigo-600 text-white py-3 px-4 rounded-lg font-semibold hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 transition-colors"
        >
          Generate Portfolio
        </button>
      </form>
    </div>
  );
}

export default PortfolioForm;

