'use client';

import React from 'react';
import { IWorkout } from '@/utils/stats';

interface ConsistencyCalendarProps {
  workouts: IWorkout[];
}

const ConsistencyCalendar: React.FC<ConsistencyCalendarProps> = ({ workouts }) => {
  const today = new Date();
  const year = today.getFullYear();
  const month = today.getMonth();

  // Get first day of month and total days
  const firstDayOfMonth = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();

  // Create set of workout dates (string format YYYY-MM-DD)
  const workoutDates = new Set(
    workouts.map(w => new Date(w.date).toISOString().split('T')[0])
  );

  const monthName = today.toLocaleString('default', { month: 'long' });

  // Generate calendar grid
  const days = [];
  // Add empty slots for days before the first day of the month
  for (let i = 0; i < (firstDayOfMonth === 0 ? 6 : firstDayOfMonth - 1); i++) {
    days.push(<div key={`empty-${i}`} className="h-8 w-8" />);
  }

  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = new Date(year, month, d + 1).toISOString().split('T')[0];
    const isToday = d === today.getDate();
    const hasWorkout = workoutDates.has(dateStr);

    days.push(
      <div 
        key={d} 
        className={`h-8 w-8 flex items-center justify-center rounded-md text-xs font-bold transition-all relative
          ${hasWorkout ? 'bg-blue-600 text-white shadow-lg shadow-blue-900/40 scale-110 z-10' : 'bg-gray-900 text-gray-500 border border-gray-800'}
          ${isToday ? 'border-2 border-white' : ''}
        `}
        title={hasWorkout ? `Workout logged on ${dateStr}` : `No workout on ${dateStr}`}
      >
        {d}
        {hasWorkout && <span className="absolute -top-1 -right-1 flex h-2 w-2">
          <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-400 opacity-75"></span>
          <span className="relative inline-flex rounded-full h-2 w-2 bg-blue-500"></span>
        </span>}
      </div>
    );
  }

  const dayLabels = ['M', 'T', 'W', 'T', 'F', 'S', 'S'];

  return (
    <div className="bg-gray-800 p-6 rounded-xl border border-gray-700 shadow-lg h-full flex flex-col">
      <div className="flex justify-between items-center mb-6">
        <h3 className="text-xl font-bold text-white uppercase tracking-tighter">{monthName}</h3>
        <div className="flex items-center space-x-2 text-[10px] text-gray-500 font-bold uppercase">
          <div className="flex items-center space-x-1">
            <div className="h-2 w-2 bg-gray-900 border border-gray-800 rounded"></div>
            <span>Rest</span>
          </div>
          <div className="flex items-center space-x-1">
            <div className="h-2 w-2 bg-blue-600 rounded"></div>
            <span>Training</span>
          </div>
        </div>
      </div>
      
      <div className="grid grid-cols-7 gap-2 flex-grow place-items-center">
        {dayLabels.map((label, i) => (
          <div key={`label-${i}`} className="text-[10px] font-black text-gray-600 uppercase mb-2">{label}</div>
        ))}
        {days}
      </div>
    </div>
  );
};

export default ConsistencyCalendar;
