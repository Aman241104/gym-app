'use client';

import React, { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { getExerciseProgress } from '@/utils/stats';

export default function ExerciseAnalytics() {
  const params = useParams();
  const router = useRouter();
  const exerciseName = decodeURIComponent(params.exercise as string);
  const [data, setData] = useState<{ date: string; weight: number }[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchWorkouts = async () => {
      try {
        const res = await fetch('/api/workouts');
        const json = await res.json();
        if (json.success) {
          const progress = getExerciseProgress(json.data, exerciseName);
          setData(progress);
        }
      } catch (error) {
        console.error('Failed to fetch workouts:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchWorkouts();
  }, [exerciseName]);

  if (loading) return <div className="text-white text-center py-10">Loading Analytics...</div>;

  return (
    <main className="container mx-auto px-4 py-8 max-w-4xl">
      <button 
        onClick={() => router.back()}
        className="text-blue-500 hover:underline mb-4 flex items-center"
      >
        &larr; Back
      </button>
      
      <h1 className="text-3xl font-black text-white uppercase tracking-tighter mb-8">
        {exerciseName} <span className="text-blue-500">Progress</span>
      </h1>

      {data.length === 0 ? (
        <p className="text-gray-400">No data found for this exercise.</p>
      ) : (
        <div className="bg-gray-800 p-6 rounded-lg border border-gray-700 shadow-lg">
          <div className="h-96 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={data}>
                <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
                <XAxis dataKey="date" stroke="#9CA3AF" />
                <YAxis label={{ value: 'Weight (kg)', angle: -90, position: 'insideLeft', fill: '#9CA3AF' }} stroke="#9CA3AF" />
                <Tooltip 
                  contentStyle={{ backgroundColor: '#1F2937', border: 'none', borderRadius: '8px' }}
                  itemStyle={{ color: '#3B82F6' }}
                />
                <Line 
                  type="monotone" 
                  dataKey="weight" 
                  stroke="#3B82F6" 
                  strokeWidth={4} 
                  dot={{ r: 6, fill: '#3B82F6' }} 
                  activeDot={{ r: 8 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}
    </main>
  );
}
