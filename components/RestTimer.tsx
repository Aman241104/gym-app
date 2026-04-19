'use client';

import React, { useState, useEffect, useRef } from 'react';

const RestTimer: React.FC = () => {
  const [seconds, setSeconds] = useState(0);
  const [isActive, setIsActive] = useState(false);
  const timerId = useRef<NodeJS.Timeout | null>(null);

  const hasTimeLeft = seconds > 0;

  useEffect(() => {
    if (isActive && hasTimeLeft) {
      timerId.current = setInterval(() => {
        setSeconds((prev) => {
          if (prev <= 1) {
            if (timerId.current) clearInterval(timerId.current);
            setIsActive(false);
            // Alert in next tick to avoid blocking the state update
            setTimeout(() => {
              if (typeof window !== 'undefined') alert('Rest finished!');
            }, 0);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    }
    
    return () => {
      if (timerId.current) clearInterval(timerId.current);
    };
  }, [isActive, hasTimeLeft]);

  const startTimer = (secs: number) => {
    if (timerId.current) clearInterval(timerId.current);
    setSeconds(secs);
    setIsActive(true);
  };

  const stopTimer = () => {
    setIsActive(false);
    setSeconds(0);
    if (timerId.current) clearInterval(timerId.current);
  };

  return (
    <div className="bg-gray-800 p-4 rounded-lg border border-gray-700 shadow-md text-center">
      <h3 className="text-gray-400 text-sm font-bold mb-2">Rest Timer</h3>
      <div className="text-3xl font-mono mb-4 text-blue-500">
        {Math.floor(seconds / 60)}:{(seconds % 60).toString().padStart(2, '0')}
      </div>
      <div className="flex justify-center space-x-2">
        <button
          onClick={() => startTimer(60)}
          className="bg-gray-700 hover:bg-gray-600 px-3 py-1 rounded text-xs"
        >
          60s
        </button>
        <button
          onClick={() => startTimer(90)}
          className="bg-gray-700 hover:bg-gray-600 px-3 py-1 rounded text-xs"
        >
          90s
        </button>
        <button
          onClick={() => startTimer(120)}
          className="bg-gray-700 hover:bg-gray-600 px-3 py-1 rounded text-xs"
        >
          2m
        </button>
        <button
          onClick={stopTimer}
          className="bg-red-900/50 hover:bg-red-800/50 px-3 py-1 rounded text-xs text-red-400"
        >
          Reset
        </button>
      </div>
    </div>
  );
};

export default RestTimer;
