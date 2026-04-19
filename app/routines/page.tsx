'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useSession } from 'next-auth/react';
import { HistorySkeleton } from '@/components/Skeletons';

interface IRoutine {
  _id: string;
  name: string;
  exercises: string[];
}

export default function RoutinesPage() {
  const { status } = useSession();
  const [routines, setRoutines] = useState<IRoutine[]>([]);
  const [dataFetched, setDataFetched] = useState(false);
  const [newName, setNewName] = useState('');
  const [newExercises, setNewExercises] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (status === 'authenticated' && !dataFetched) {
      fetch('/api/routines')
        .then(res => res.json())
        .then(json => {
          if (json.success) setRoutines(json.data);
        })
        .finally(() => setDataFetched(true));
    }
  }, [status, dataFetched]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newName || !newExercises) return;

    setIsSubmitting(true);
    try {
      const exercises = newExercises.split(',').map(s => s.trim()).filter(s => s !== '');
      const res = await fetch('/api/routines', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: newName, exercises }),
      });
      if (res.ok) {
        setNewName('');
        setNewExercises('');
        // Refetch
        const refetchRes = await fetch('/api/routines');
        const json = await refetchRes.json();
        if (json.success) setRoutines(json.data);
      }
    } catch (error) {
      console.error('Failed to create routine:', error);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Are you sure?')) return;
    try {
      const res = await fetch(`/api/routines/${id}`, { method: 'DELETE' });
      if (res.ok) {
        setRoutines(prev => prev.filter(r => r._id !== id));
      }
    } catch (error) {
      console.error('Failed to delete routine:', error);
    }
  };

  const handleImportPPL = async () => {
    const pplRoutines = [
      {
        name: "Push Day 1 (Chest Focused)",
        exercises: ["Bench Press", "Incline Bench Press", "Dumbbell Flies", "Overhead Press", "Lateral Raise", "Cable Pushdowns", "Skullcrushers"]
      },
      {
        name: "Pull Day 1 (Vertical Pulling)",
        exercises: ["Weighted Pullups", "Straight Arm Pulldowns", "Meadows Row", "Shrugs", "Strict Curl", "Hammer Curl", "Preacher Curl", "Bent Over Rear Delt Flies"]
      },
      {
        name: "Leg Day 1 (Quad Focused)",
        exercises: ["Squat", "Lunges", "Leg Extensions", "RDL", "Calf Raise"]
      },
      {
        name: "Push Day 2 (Shoulder Focused)",
        exercises: ["Overhead Press", "Lateral Raise", "Bench Press", "Incline Bench Press", "Cable Flies", "Cable Pushdowns", "Skullcrushers"]
      },
      {
        name: "Pull Day 2 (Horizontal Pulling)",
        exercises: ["Barbell Rows", "Seated Cable Row", "Lat Pulldown", "Shrugs Behind the Back", "Strict Curl", "Cable Hammer Curl", "Concentration Curl", "Facepulls"]
      },
      {
        name: "Leg Day 2 (Glute/Ham Focused)",
        exercises: ["Hip Thrust", "Glute Kickbacks", "Goodmornings", "Hamstring Curls", "Leg Press", "Calf Raise"]
      }
    ];

    setIsSubmitting(true);
    try {
      for (const routine of pplRoutines) {
        await fetch('/api/routines', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(routine),
        });
      }
      fetchRoutines();
      alert('PPL Routine imported successfully!');
    } catch (error) {
      console.error('Failed to import routines:', error);
    } finally {
      setIsSubmitting(false);
    }
  };

  const isLoading = status === 'loading' || (status === 'authenticated' && !dataFetched);

  if (isLoading) {
    return (
      <main className="container mx-auto px-4 py-8 max-w-2xl text-white">
        <h1 className="text-3xl font-black uppercase mb-8">My Routines</h1>
        <HistorySkeleton />
      </main>
    );
  }

  if (status === 'unauthenticated') {
    return <div className="text-white text-center py-10">Please sign in to manage routines.</div>;
  }

  return (
    <main className="container mx-auto px-4 py-8 max-w-2xl text-white">
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-black uppercase tracking-tighter">My Routines</h1>
        <button
          onClick={handleImportPPL}
          disabled={isSubmitting}
          className="text-xs bg-blue-900/30 hover:bg-blue-800/40 text-blue-400 border border-blue-800 px-3 py-2 rounded-lg transition-all font-bold"
        >
          {isSubmitting ? 'Importing...' : 'Import My PPL Routine'}
        </button>
      </div>

      {/* Create Routine Form */}
      <section className="bg-gray-900 p-6 rounded-xl border border-gray-800 shadow-xl mb-12">
        <h2 className="text-xl font-bold mb-4">Create New Template</h2>
        <form onSubmit={handleCreate} className="space-y-4">
          <div>
            <label className="block text-xs text-gray-500 font-bold uppercase mb-1">Routine Name</label>
            <input
              type="text"
              placeholder="e.g. Upper Body Power"
              className="w-full bg-gray-800 border border-gray-700 rounded p-2 outline-none focus:ring-2 focus:ring-blue-500"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              required
            />
          </div>
          <div>
            <label className="block text-xs text-gray-500 font-bold uppercase mb-1">Exercises (comma separated)</label>
            <textarea
              placeholder="Bench Press, Squat, Pull Ups..."
              className="w-full bg-gray-800 border border-gray-700 rounded p-2 outline-none focus:ring-2 focus:ring-blue-500 h-20"
              value={newExercises}
              onChange={(e) => setNewExercises(e.target.value)}
              required
            />
          </div>
          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full bg-blue-600 hover:bg-blue-700 p-2 rounded font-bold transition-colors disabled:opacity-50"
          >
            {isSubmitting ? 'Saving...' : 'Save Routine'}
          </button>
        </form>
      </section>

      {/* Routine List */}
      <div className="space-y-4">
        <h2 className="text-xl font-bold mb-4">Saved Templates</h2>
        {routines.length === 0 ? (
          <p className="text-gray-500 italic">No routines saved yet.</p>
        ) : (
          routines.map(routine => (
            <div key={routine._id} className="bg-gray-800 p-4 rounded-lg border border-gray-700 flex justify-between items-center group">
              <div>
                <h3 className="font-bold text-blue-400">{routine.name}</h3>
                <p className="text-xs text-gray-400 mt-1">{routine.exercises.join(' • ')}</p>
              </div>
              <div className="flex items-center space-x-2">
                <Link
                  href={`/workout?routineId=${routine._id}`}
                  className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded text-xs font-bold transition-all"
                >
                  Start
                </Link>
                <button
                  onClick={() => handleDelete(routine._id)}
                  className="text-gray-500 hover:text-red-500 p-2 opacity-0 group-hover:opacity-100 transition-opacity"
                >
                  <svg width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/></svg>
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </main>
  );
}
