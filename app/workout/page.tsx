'use client';

import React, { useEffect, useState, Suspense } from 'react';
import WorkoutForm from '@/components/WorkoutForm';
import SuggestionCard from '@/components/SuggestionCard';
import RestTimer from '@/components/RestTimer';
import { getWorkoutSuggestions, Suggestion } from '@/utils/suggestions';
import { IWorkout } from '@/utils/stats';
import { useSearchParams } from 'next/navigation';
import { WorkoutCardSkeleton } from '@/components/Skeletons';

function WorkoutContent() {
  const searchParams = useSearchParams();
  const routineId = searchParams.get('routineId');
  
  const [suggestions, setSuggestions] = useState<Suggestion[]>([]);
  const [loadingSuggestions, setLoadingSuggestions] = useState(true);
  const [initialData, setInitialData] = useState<{ name: string; sets: { reps: number; weight: number }[] }[]>([]);
  const [loadingRoutine, setLoadingRoutine] = useState(!!routineId);

  useEffect(() => {
    const fetchData = async () => {
      try {
        // Fetch Suggestions from last workout
        const resWorkouts = await fetch('/api/workouts');
        const jsonWorkouts = await resWorkouts.json();
        if (jsonWorkouts.success && jsonWorkouts.data.length > 0) {
          const lastWorkout: IWorkout = jsonWorkouts.data[0];
          const calculatedSuggestions = getWorkoutSuggestions(lastWorkout);
          setSuggestions(calculatedSuggestions);
        }
        setLoadingSuggestions(false);

        // Fetch Routine if ID exists
        if (routineId) {
          const resRoutine = await fetch(`/api/routines/${routineId}`);
          const jsonRoutine = await resRoutine.json();
          if (jsonRoutine.success) {
            const exerciseNames: string[] = jsonRoutine.data.exercises;
            setInitialData(exerciseNames.map(name => ({
              name,
              sets: [{ reps: 0, weight: 0 }]
            })));
          }
          setLoadingRoutine(false);
        }
      } catch (error) {
        console.error('Failed to fetch initial workout data:', error);
        setLoadingSuggestions(false);
        setLoadingRoutine(false);
      }
    };

    fetchData();
  }, [routineId]);

  if (loadingRoutine) {
    return (
      <div className="max-w-2xl mx-auto">
        <WorkoutCardSkeleton />
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 max-w-2xl space-y-8">
      <RestTimer />
      {!loadingSuggestions && suggestions.length > 0 && (
        <SuggestionCard suggestions={suggestions} />
      )}
      <WorkoutForm initialData={initialData} />
    </div>
  );
}

export default function WorkoutPage() {
  return (
    <main className="min-h-screen bg-black py-8">
      <Suspense fallback={<div className="text-white text-center">Loading Workout...</div>}>
        <WorkoutContent />
      </Suspense>
    </main>
  );
}
