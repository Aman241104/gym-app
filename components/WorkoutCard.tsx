import React from 'react';
import Link from 'next/link';
import { useSettings } from '@/components/SettingsProvider';

interface ISet {
  reps: number;
  weight: number;
}

interface IExercise {
  name: string;
  sets: ISet[];
}

interface WorkoutCardProps {
  workout: {
    _id: string;
    date: string;
    exercises: IExercise[];
  };
  onDelete?: (id: string) => void;
}

const WorkoutCard: React.FC<WorkoutCardProps> = ({ workout, onDelete }) => {
  const { unit } = useSettings();
  const date = new Date(workout.date).toLocaleDateString(undefined, {
    weekday: 'short',
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });

  const handleDelete = async () => {
    if (!confirm('Are you sure you want to delete this workout?')) return;
    
    try {
      const res = await fetch(`/api/workouts/${workout._id}`, {
        method: 'DELETE',
      });
      if (res.ok && onDelete) {
        onDelete(workout._id);
      }
    } catch (error) {
      console.error('Failed to delete workout:', error);
    }
  };

  return (
    <div className="bg-gray-800 rounded-lg shadow-md p-4 mb-4 text-white border border-gray-700 group">
      <div className="flex justify-between items-center mb-3">
        <h3 className="text-lg font-bold text-blue-400">{date}</h3>
        <div className="flex items-center space-x-2 opacity-0 group-hover:opacity-100 transition-all">
          <Link 
            href={`/workout/edit/${workout._id}`}
            className="text-gray-500 hover:text-blue-500 p-1"
            title="Edit Workout"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
          </Link>
          <button 
            onClick={handleDelete}
            className="text-gray-500 hover:text-red-500 p-1"
            title="Delete Workout"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/><line x1="10" x2="10" y1="11" y2="17"/><line x1="14" x2="14" y1="11" y2="17"/></svg>
          </button>
        </div>
      </div>
      
      <div className="space-y-4">
        {workout.exercises.map((exercise, idx) => (
          <div key={idx} className="border-t border-gray-700 pt-3">
            <h4 className="font-semibold text-gray-200">{exercise.name}</h4>
            <div className="grid grid-cols-4 gap-2 mt-2">
              {exercise.sets.map((set, sIdx) => (
                <div key={sIdx} className="text-sm bg-gray-900 rounded p-1 text-center">
                  <span className="block text-gray-400 text-xs">Set {sIdx + 1}</span>
                  <span>{set.weight}{unit} × {set.reps}</span>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default WorkoutCard;
