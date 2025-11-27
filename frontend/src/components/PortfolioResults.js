import React from 'react';
import { PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

const COLORS = ['#4F46E5', '#7C3AED', '#EC4899', '#F59E0B', '#10B981', '#3B82F6', '#EF4444', '#8B5CF6'];

function PortfolioResults({ data, onReset }) {
  // Prepare data for pie chart
  const pieData = Object.entries(data.allocations || {}).map(([symbol, value]) => ({
    name: symbol,
    value: (value * 100).toFixed(1)
  }));

  // Prepare data for bar chart (risk vs return)
  const barData = (data.predictions || []).map(pred => ({
    symbol: pred.symbol,
    'Expected Return (%)': pred.expectedReturn ? (pred.expectedReturn * 100).toFixed(2) : 0,
    'Risk Score': pred.riskScore ? pred.riskScore.toFixed(0) : 0
  }));

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-3xl font-bold text-gray-800">Portfolio Analysis</h2>
        <button
          onClick={onReset}
          className="px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors"
        >
          New Portfolio
        </button>
      </div>

      {/* Portfolio Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-white rounded-lg shadow p-4">
          <p className="text-sm text-gray-600">Total Expected Return</p>
          <p className="text-2xl font-bold text-green-600">
            {(data.totalExpectedReturn * 100).toFixed(2)}%
          </p>
        </div>
        <div className="bg-white rounded-lg shadow p-4">
          <p className="text-sm text-gray-600">Total Risk</p>
          <p className="text-2xl font-bold text-orange-600">
            {(data.totalRisk * 100).toFixed(2)}%
          </p>
        </div>
        <div className="bg-white rounded-lg shadow p-4">
          <p className="text-sm text-gray-600">Compliance Status</p>
          <p className={`text-2xl font-bold ${data.compliant ? 'text-green-600' : 'text-red-600'}`}>
            {data.compliant ? 'Compliant' : 'Issues'}
          </p>
        </div>
        <div className="bg-white rounded-lg shadow p-4">
          <p className="text-sm text-gray-600">Number of Stocks</p>
          <p className="text-2xl font-bold text-indigo-600">
            {Object.keys(data.allocations || {}).length}
          </p>
        </div>
      </div>

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Pie Chart */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-xl font-bold text-gray-800 mb-4">Portfolio Allocation</h3>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={pieData}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                outerRadius={100}
                fill="#8884d8"
                dataKey="value"
              >
                {pieData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>

        {/* Bar Chart */}
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-xl font-bold text-gray-800 mb-4">Risk vs Expected Returns</h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={barData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="symbol" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Bar dataKey="Expected Return (%)" fill="#4F46E5" />
              <Bar dataKey="Risk Score" fill="#EC4899" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Stock Details Table */}
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-xl font-bold text-gray-800 mb-4">Stock Details</h3>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Symbol</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Allocation</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Amount</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Expected Return</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Risk Score</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Trend</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Signal</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {data.predictions?.map((pred, index) => {
                const allocation = data.allocations?.[pred.symbol] || 0;
                const amount = data.amounts?.[pred.symbol] || 0;
                return (
                  <tr key={index}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {pred.symbol}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {(allocation * 100).toFixed(2)}%
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      ${amount.toFixed(2)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {pred.expectedReturn ? (pred.expectedReturn * 100).toFixed(2) + '%' : 'N/A'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {pred.riskScore ? pred.riskScore.toFixed(0) : 'N/A'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                      <span className={`px-2 py-1 rounded ${
                        pred.trend === 'BULLISH' ? 'bg-green-100 text-green-800' :
                        pred.trend === 'BEARISH' ? 'bg-red-100 text-red-800' :
                        'bg-gray-100 text-gray-800'
                      }`}>
                        {pred.trend || 'N/A'}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                      <span className={`px-2 py-1 rounded ${
                        pred.signal === 'BUY' ? 'bg-green-100 text-green-800' :
                        pred.signal === 'SELL' ? 'bg-red-100 text-red-800' :
                        'bg-yellow-100 text-yellow-800'
                      }`}>
                        {pred.signal || 'N/A'}
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Compliance Warnings */}
      {data.violations && data.violations.length > 0 && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-6">
          <h3 className="text-xl font-bold text-red-800 mb-4">Compliance Violations</h3>
          <ul className="space-y-2">
            {data.violations.map((violation, index) => (
              <li key={index} className="text-red-700">
                <span className="font-semibold">{violation.type}:</span> {violation.message}
                {violation.severity && (
                  <span className="ml-2 px-2 py-1 bg-red-200 text-red-800 text-xs rounded">
                    {violation.severity}
                  </span>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Warnings */}
      {data.warnings && data.warnings.length > 0 && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-6">
          <h3 className="text-xl font-bold text-yellow-800 mb-4">Warnings</h3>
          <ul className="space-y-2">
            {data.warnings.map((warning, index) => (
              <li key={index} className="text-yellow-700">
                <span className="font-semibold">{warning.type}:</span> {warning.message}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Citations */}
      {data.citations && data.citations.length > 0 && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
          <h3 className="text-xl font-bold text-blue-800 mb-4">Regulatory Citations</h3>
          <div className="space-y-4">
            {data.citations.map((citation, index) => (
              <div key={index} className="bg-white rounded p-4 border border-blue-200">
                <p className="text-sm text-gray-700 mb-2">{citation.text}</p>
                <p className="text-xs text-gray-500">
                  Source: {citation.source}
                  {citation.relevanceScore && (
                    <span className="ml-2">(Relevance: {citation.relevanceScore.toFixed(3)})</span>
                  )}
                </p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* AI Explanation */}
      {data.aiExplanation && (
        <div className="bg-gradient-to-r from-indigo-50 to-purple-50 border border-indigo-200 rounded-lg p-6">
          <h3 className="text-xl font-bold text-indigo-800 mb-4">AI Explanation</h3>
          <div className="bg-white rounded p-4 border border-indigo-200">
            <pre className="whitespace-pre-wrap text-sm text-gray-700 font-sans">
              {data.aiExplanation}
            </pre>
          </div>
        </div>
      )}
    </div>
  );
}

export default PortfolioResults;

