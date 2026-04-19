'use client';

import React, { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import WorkoutForm from '@/components/WorkoutForm';
import { WorkoutCardSkeleton } from '@/components/Skeletons';
import { IWorkout } from '@/utils/stats';

export default function EditWorkoutPage() {
  const params = useParams();
  const router = useRouter();
  const [workout, setWorkout] = useState<IWorkout | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchWorkout = async () => {
      try {
        const id = Array.isArray(params.id) ? params.id[0] : params.id;
        const res = await fetch(`/api/workouts/${id}`);
        const json = await res.json();
        if (json.success) {
          setWorkout(json.data);
        } else {
          alert('Workout not found');
          router.push('/history');
        }
      } catch (error) {
        console.error('Failed to fetch workout:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchWorkout();
  }, [params.id, router]);

  if (loading) {
    return (
      <main className="min-h-screen bg-black py-8">
        <div className="container mx-auto px-4 max-w-2xl">
          <WorkoutCardSkeleton />
        </div>
      </main>
    );
  }

  if (!workout) return null;

  return (
    <main className="min-h-screen bg-black py-8">
      <div className="container mx-auto px-4">
        <WorkoutForm 
          initialData={workout.exercises} 
          initialDate={workout.date}
          workoutId={workout._id} 
          isEdit={true} 
        />
      </div>
    </main>
  );
}
