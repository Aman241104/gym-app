'use client';

import React, { useEffect, useState } from 'react';
import Dashboard from '@/components/Dashboard';
import Link from 'next/link';
import { IWorkout } from '@/utils/stats';
import { DashboardSkeleton } from '@/components/Skeletons';

export default function Home() {
  const [workouts, setWorkouts] = useState<IWorkout[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchWorkouts = async () => {
      try {
        const res = await fetch('/api/workouts');
        const json = await res.json();
        if (json.success) {
          setWorkouts(json.data);
        }
      } catch (error) {
        console.error('Failed to fetch workouts:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchWorkouts();
  }, []);

  return (
    <main className="container mx-auto px-4 py-8 max-w-4xl">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-4xl font-black text-white uppercase tracking-tighter">My Progress</h1>
          <p className="text-gray-400 font-medium">Keep pushing your limits.</p>
        </div>
        <Link 
          href="/workout" 
          className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-3 rounded-full font-bold transition-all hover:scale-105"
        >
          + Log Workout
        </Link>
      </div>
      
      {loading ? (
        <DashboardSkeleton />
      ) : workouts.length === 0 ? (
        <div className="text-center py-16 bg-gray-900/50 rounded-2xl border border-gray-800 shadow-inner">
          <div className="text-6xl mb-6">🏋️‍♂️</div>
          <h2 className="text-2xl font-bold text-white mb-2">No Workouts Yet</h2>
          <p className="text-gray-400 mb-8 max-w-sm mx-auto">
            Your fitness journey starts here. Log your first workout to see your progress and analytics!
          </p>
          <Link 
            href="/workout" 
            className="inline-block bg-blue-600 hover:bg-blue-700 text-white px-8 py-3 rounded-full font-bold transition-all shadow-lg hover:shadow-blue-900/20"
          >
            Start First Workout
          </Link>
        </div>
      ) : (
        <Dashboard workouts={workouts} />
      )}
    </main>
  );
}
