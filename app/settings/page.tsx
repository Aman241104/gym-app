'use client';

import React, { useState } from 'react';
import { useSettings } from '@/components/SettingsProvider';
import { useSession } from 'next-auth/react';
import { IWorkout } from '@/utils/stats';
import Image from 'next/image';

export default function SettingsPage() {
  const { unit, setUnit, loading: settingsLoading } = useSettings();
  const { data: session, status } = useSession();
  const [isExporting, setIsSubmitting] = useState(false);

  const handleExport = async () => {
    setIsSubmitting(true);
    try {
      const res = await fetch('/api/workouts');
      const json = await res.json();
      if (json.success) {
        const workouts: IWorkout[] = json.data;
        // Convert to CSV
        let csv = 'Date,Exercise,Set,Weight,Reps\n';
        workouts.forEach((w) => {
          const date = new Date(w.date).toLocaleDateString();
          w.exercises.forEach((ex) => {
            ex.sets.forEach((s, idx: number) => {
              csv += `${date},"${ex.name}",${idx + 1},${s.weight},${s.reps}\n`;
            });
          });
        });

        // Download
        const blob = new Blob([csv], { type: 'text/csv' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.setAttribute('hidden', '');
        a.setAttribute('href', url);
        a.setAttribute('download', `workouts_${new Date().toISOString().split('T')[0]}.csv`);
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
      }
    } catch (error) {
      console.error('Export failed:', error);
      alert('Failed to export data');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (status === 'loading' || settingsLoading) {
    return <div className="text-white text-center py-10">Loading settings...</div>;
  }

  if (!session) {
    return <div className="text-white text-center py-10">Please sign in to access settings.</div>;
  }

  return (
    <main className="container mx-auto px-4 py-8 max-w-2xl">
      <h1 className="text-3xl font-black text-white uppercase tracking-tighter mb-8">Settings</h1>

      <div className="space-y-6">
        {/* Unit Selection */}
        <section className="bg-gray-800 p-6 rounded-lg border border-gray-700 shadow-lg">
          <h2 className="text-xl font-bold mb-4">Preferences</h2>
          <div className="flex items-center justify-between">
            <div>
              <p className="font-semibold text-gray-200">Weight Unit</p>
              <p className="text-sm text-gray-400">Choose between Kilograms and Pounds</p>
            </div>
            <div className="flex bg-gray-900 rounded-lg p-1 border border-gray-700">
              <button
                onClick={() => setUnit('kg')}
                className={`px-4 py-1 rounded-md text-sm font-bold transition-all ${unit === 'kg' ? 'bg-blue-600 text-white' : 'text-gray-500 hover:text-gray-300'}`}
              >
                KG
              </button>
              <button
                onClick={() => setUnit('lbs')}
                className={`px-4 py-1 rounded-md text-sm font-bold transition-all ${unit === 'lbs' ? 'bg-blue-600 text-white' : 'text-gray-500 hover:text-gray-300'}`}
              >
                LBS
              </button>
            </div>
          </div>
        </section>

        {/* Account Info */}
        <section className="bg-gray-800 p-6 rounded-lg border border-gray-700 shadow-lg">
          <h2 className="text-xl font-bold mb-4">Account</h2>
          <div className="flex items-center space-x-4 mb-4">
            {session.user?.image && (
              <div className="relative w-12 h-12">
                <Image src={session.user.image} alt={session.user.name || ''} fill className="rounded-full border border-gray-600 object-cover" />
              </div>
            )}
            <div>
              <p className="font-bold text-white">{session.user?.name}</p>
              <p className="text-sm text-gray-400">{session.user?.email}</p>
            </div>
          </div>
        </section>

        {/* Data Management */}
        <section className="bg-gray-800 p-6 rounded-lg border border-gray-700 shadow-lg">
          <h2 className="text-xl font-bold mb-4">Data Management</h2>
          <p className="text-sm text-gray-400 mb-4">Export all your workout history to a CSV file for your own records.</p>
          <button
            onClick={handleExport}
            disabled={isExporting}
            className="w-full sm:w-auto bg-gray-700 hover:bg-gray-600 text-white px-6 py-2 rounded font-bold transition-colors disabled:opacity-50"
          >
            {isExporting ? 'Exporting...' : 'Export Workouts (CSV)'}
          </button>
        </section>
      </div>
    </main>
  );
}
