import React from 'react';

function LoadingSpinner() {
  return (
    <div className="flex justify-center items-center py-12">
      <div className="relative">
        <div className="w-16 h-16 border-4 border-indigo-200 border-t-indigo-600 rounded-full animate-spin"></div>
        <p className="mt-4 text-center text-gray-600 font-medium">
          Generating your optimized portfolio...
        </p>
      </div>
    </div>
  );
}

export default LoadingSpinner;

