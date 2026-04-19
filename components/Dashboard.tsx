'use client';

import React from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend } from 'recharts';
import { calculateTotalVolume, getPersonalRecords, getWeeklyProgress, IWorkout, calculateStreak, calculateVolumeByMuscleGroup } from '@/utils/stats';
import { useSettings } from '@/components/SettingsProvider';
import ConsistencyCalendar from '@/components/ConsistencyCalendar';

import Link from 'next/link';

const COLORS = ['#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899', '#6B7280'];

interface DashboardProps {
  workouts: IWorkout[];
}

const Dashboard: React.FC<DashboardProps> = ({ workouts }) => {
  const { unit } = useSettings();
  const totalVolume = calculateTotalVolume(workouts);
  const prs = getPersonalRecords(workouts);
  const progressData = getWeeklyProgress(workouts);
  const streak = calculateStreak(workouts);
  const muscleGroupData = calculateVolumeByMuscleGroup(workouts);

  return (
    <div className="space-y-8 text-white">
      {/* Top Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-gray-800 p-6 rounded-lg border border-gray-700 shadow-lg">
          <h3 className="text-gray-400 text-sm uppercase font-bold mb-1">Total Volume</h3>
          <p className="text-4xl font-extrabold text-blue-500">{totalVolume.toLocaleString()} <span className="text-lg">{unit}</span></p>
        </div>
        <div className="bg-gray-800 p-6 rounded-lg border border-gray-700 shadow-lg">
          <h3 className="text-gray-400 text-sm uppercase font-bold mb-1">Total Workouts</h3>
          <p className="text-4xl font-extrabold text-green-500">{workouts.length}</p>
        </div>
        <div className="bg-gray-800 p-6 rounded-lg border border-gray-700 shadow-lg flex flex-col justify-center items-center text-center">
          <h3 className="text-gray-400 text-sm uppercase font-bold mb-1">Current Streak</h3>
          <p className="text-4xl font-extrabold text-orange-500 flex items-center">
            {streak} <span className="text-2xl ml-2">🔥</span>
          </p>
        </div>
      </div>

      {/* Main Progress Chart */}
      <div className="bg-gray-800 p-6 rounded-lg border border-gray-700 shadow-lg">
        <h3 className="text-xl font-bold mb-6 tracking-tighter uppercase">Volume Progress ({unit})</h3>
        <div className="h-64 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={progressData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#374151" />
              <XAxis dataKey="date" stroke="#9CA3AF" />
              <YAxis stroke="#9CA3AF" />
              <Tooltip 
                contentStyle={{ backgroundColor: '#1F2937', border: 'none', borderRadius: '8px' }}
                itemStyle={{ color: '#3B82F6' }}
              />
              <Line type="monotone" dataKey="volume" stroke="#3B82F6" strokeWidth={3} dot={{ fill: '#3B82F6' }} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Consistency and Distribution */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        <ConsistencyCalendar workouts={workouts} />
        
        <div className="bg-gray-800 p-6 rounded-lg border border-gray-700 shadow-lg">
          <h3 className="text-xl font-bold mb-6 tracking-tighter uppercase">Training Distribution</h3>
          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={muscleGroupData}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={80}
                  paddingAngle={5}
                  dataKey="value"
                >
                  {muscleGroupData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip 
                  contentStyle={{ backgroundColor: '#1F2937', border: 'none', borderRadius: '8px' }}
                  itemStyle={{ color: '#fff' }}
                />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* PRs */}
      <div className="bg-gray-800 p-6 rounded-lg border border-gray-700 shadow-lg">
        <h3 className="text-xl font-bold mb-4 tracking-tighter uppercase">Personal Records</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 max-h-[20rem] overflow-y-auto pr-2 custom-scrollbar">
          {Object.entries(prs).map(([name, weight]) => (
            <Link 
              key={name} 
              href={`/analytics/${encodeURIComponent(name)}`}
              className="flex justify-between items-center p-4 bg-gray-900 rounded-lg border border-gray-700 hover:border-blue-500 transition-all group"
            >
              <span className="font-bold group-hover:text-blue-400">{name}</span>
              <span className="text-blue-400 font-black">{weight} {unit}</span>
            </Link>
          ))}
          {Object.entries(prs).length === 0 && <p className="text-gray-500 italic">No records yet.</p>}
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
