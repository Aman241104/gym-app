'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useSettings } from '@/components/SettingsProvider';

interface ISet {
  reps: number;
  weight: number;
}

interface IExercise {
  name: string;
  sets: ISet[];
}

interface WorkoutFormProps {
  initialData?: IExercise[];
  initialDate?: string;
  workoutId?: string;
  isEdit?: boolean;
}

const COMMON_EXERCISES = [
  'Bench Press', 'Squat', 'Deadlift', 'Overhead Press', 'Barbell Row', 'Pull Ups', 'Dips'
];

const WorkoutForm: React.FC<WorkoutFormProps> = ({ initialData = [], initialDate, workoutId, isEdit = false }) => {
  const router = useRouter();
  const { unit } = useSettings();
  const [exercises, setExercises] = useState<IExercise[]>(initialData);
  const [date, setDate] = useState<string>(initialDate ? new Date(initialDate).toISOString().split('T')[0] : new Date().toISOString().split('T')[0]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const addExercise = (name: string = '') => {
    setExercises([...exercises, { name, sets: [{ reps: 0, weight: 0 }] }]);
  };

  const addSet = (exerciseIndex: number) => {
    const newExercises = [...exercises];
    newExercises[exerciseIndex].sets.push({ reps: 0, weight: 0 });
    setExercises(newExercises);
  };

  const updateExerciseName = (index: number, name: string) => {
    const newExercises = [...exercises];
    newExercises[index].name = name;
    setExercises(newExercises);
  };

  const updateSet = (exerciseIndex: number, setIndex: number, field: keyof ISet, value: number) => {
    const newExercises = [...exercises];
    newExercises[exerciseIndex].sets[setIndex][field] = value;
    setExercises(newExercises);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (exercises.length === 0) return;
    
    setIsSubmitting(true);
    try {
      const url = isEdit ? `/api/workouts/${workoutId}` : '/api/workouts';
      const method = isEdit ? 'PUT' : 'POST';
      
      const response = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ exercises, date }),
      });

      if (response.ok) {
        router.push('/history');
        router.refresh(); // Ensure the dashboard/history updates
      } else {
        alert('Failed to save workout');
      }
    } catch (error) {
      console.error('Error submitting workout:', error);
      alert('An error occurred');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6 max-w-2xl mx-auto p-4 bg-gray-900 text-white rounded-lg shadow-xl">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-4">
        <h2 className="text-2xl font-bold">{isEdit ? 'Edit Workout' : 'Log Workout'}</h2>
        <input 
          type="date"
          className="bg-gray-800 border border-gray-700 rounded p-2 text-sm text-blue-400 font-bold focus:ring-2 focus:ring-blue-500 outline-none w-full sm:w-auto"
          value={date}
          onChange={(e) => setDate(e.target.value)}
          required
        />
      </div>

      <div className="space-y-2">
        <p className="text-xs text-gray-500 font-bold uppercase tracking-wider">Quick Add</p>
        <div className="flex flex-wrap gap-2">
          {COMMON_EXERCISES.map(name => (
            <button
              key={name}
              type="button"
              onClick={() => addExercise(name)}
              className="text-xs bg-gray-800 hover:bg-gray-700 border border-gray-700 rounded-full px-3 py-1 text-gray-300 hover:text-white transition-colors"
            >
              + {name}
            </button>
          ))}
        </div>
      </div>
      
      {exercises.map((exercise, exIndex) => (
        <div key={exIndex} className="p-4 border border-gray-700 rounded-md bg-gray-800 space-y-4">
          <input
            type="text"
            placeholder="Exercise Name (e.g. Bench Press)"
            className="w-full bg-gray-700 border-none rounded p-2 text-white focus:ring-2 focus:ring-blue-500 outline-none"
            value={exercise.name}
            onChange={(e) => updateExerciseName(exIndex, e.target.value)}
            required
          />
          
          <div className="space-y-2">
            {exercise.sets.map((set, setIndex) => (
              <div key={setIndex} className="flex items-center space-x-4">
                <span className="w-8 text-gray-400">Set {setIndex + 1}</span>
                <div className="flex-1 flex space-x-2">
                  <div className="flex-1">
                    <label className="block text-xs text-gray-400">Weight ({unit})</label>
                    <input
                      type="number"
                      step="0.1"
                      className="w-full bg-gray-700 border-none rounded p-1 outline-none"
                      value={set.weight}
                      onChange={(e) => updateSet(exIndex, setIndex, 'weight', parseFloat(e.target.value))}
                    />
                  </div>
                  <div className="flex-1">
                    <label className="block text-xs text-gray-400">Reps</label>
                    <input
                      type="number"
                      className="w-full bg-gray-700 border-none rounded p-1 outline-none"
                      value={set.reps}
                      onChange={(e) => updateSet(exIndex, setIndex, 'reps', parseInt(e.target.value))}
                    />
                  </div>
                </div>
              </div>
            ))}
          </div>
          
          <button
            type="button"
            onClick={() => addSet(exIndex)}
            className="text-blue-400 text-sm hover:underline"
          >
            + Add Set
          </button>
        </div>
      ))}
      
      <button
        type="button"
        onClick={() => addExercise()}
        className="w-full border-2 border-dashed border-gray-600 p-4 rounded-md text-gray-400 hover:text-white hover:border-white transition-colors"
      >
        + Add Custom Exercise
      </button>
      
      <button
        type="submit"
        disabled={isSubmitting || exercises.length === 0}
        className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-gray-600 p-3 rounded font-bold transition-colors"
      >
        {isSubmitting ? 'Saving Changes...' : (isEdit ? 'Update Workout' : 'Finish Workout')}
      </button>
    </form>
  );
};

export default WorkoutForm;
