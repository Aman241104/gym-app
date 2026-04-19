'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import WorkoutCard from '@/components/WorkoutCard';
import { HistorySkeleton } from '@/components/Skeletons';

interface ISet {
  reps: number;
  weight: number;
}

interface IExercise {
  name: string;
  sets: ISet[];
}

interface IWorkout {
  _id: string;
  date: string;
  exercises: IExercise[];
}

export default function HistoryPage() {
  const [workouts, setWorkouts] = useState<IWorkout[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
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

  const handleDeleteWorkout = (id: string) => {
    setWorkouts(prev => prev.filter(w => w._id !== id));
  };

  const filteredWorkouts = workouts.filter(workout => 
    workout.exercises.some(ex => 
      ex.name.toLowerCase().includes(searchQuery.toLowerCase())
    )
  );

  return (
    <main className="min-h-screen bg-black py-8">
      <div className="container mx-auto px-4 max-w-2xl">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
          <h1 className="text-3xl font-bold text-white tracking-tight">Workout History</h1>
          <div className="relative w-full sm:w-64">
            <input
              type="text"
              placeholder="Search exercise..."
              className="w-full bg-gray-900 border border-gray-800 rounded-full py-2 px-10 text-sm text-gray-200 focus:ring-2 focus:ring-blue-500 outline-none transition-all"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
            <svg className="absolute left-3 top-2.5 text-gray-500" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
          </div>
        </div>

        {loading ? (
          <HistorySkeleton />
        ) : filteredWorkouts.length === 0 ? (
          <div className="text-center py-12 bg-gray-900/30 rounded-xl border border-gray-800 border-dashed">
            <p className="text-gray-500 mb-4">{searchQuery ? 'No workouts match your search.' : 'No training history found.'}</p>
            {!searchQuery && (
              <Link href="/workout" className="text-blue-500 font-bold hover:text-blue-400">
                Go to Gym &rarr;
              </Link>
            )}
            {searchQuery && (
              <button onClick={() => setSearchQuery('')} className="text-blue-500 font-bold">Clear Search</button>
            )}
          </div>
        ) : (
          filteredWorkouts.map((workout: IWorkout) => (
            <WorkoutCard 
              key={workout._id} 
              workout={workout} 
              onDelete={handleDeleteWorkout}
            />
          ))
        )}
      </div>
    </main>
  );
}
