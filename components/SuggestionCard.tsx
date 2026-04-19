import React from 'react';
import { Suggestion } from '@/utils/suggestions';

interface SuggestionCardProps {
  suggestions: Suggestion[];
}

const SuggestionCard: React.FC<SuggestionCardProps> = ({ suggestions }) => {
  if (suggestions.length === 0) return null;

  return (
    <div className="bg-blue-900/30 border border-blue-500/50 rounded-lg p-6 mb-8 text-white shadow-lg backdrop-blur-sm">
      <div className="flex items-center space-x-2 mb-4">
        <span className="text-2xl">🤖</span>
        <h3 className="text-xl font-bold text-blue-400">AI Coach Suggestions</h3>
      </div>
      <div className="grid grid-cols-1 gap-4">
        {suggestions.map((s, idx) => (
          <div key={idx} className="bg-blue-950/40 p-4 rounded-md border border-blue-800">
            <div className="flex justify-between items-start mb-2">
              <h4 className="font-bold text-lg text-blue-200">{s.exerciseName}</h4>
              <div className="bg-blue-600 text-xs px-2 py-1 rounded-full font-bold">
                Target: {s.suggestedWeight}kg × {s.suggestedReps}
              </div>
            </div>
            <p className="text-sm text-blue-100/80 italic">&quot;{s.reason}&quot;</p>
          </div>
        ))}
      </div>
    </div>
  );
};

export default SuggestionCard;
