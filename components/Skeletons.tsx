import React from 'react';

export const Skeleton: React.FC<{ className?: string }> = ({ className }) => (
  <div className={`animate-pulse bg-gray-700 rounded ${className}`} />
);

export const DashboardSkeleton = () => (
  <div className="space-y-8">
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
      <Skeleton className="h-28" />
      <Skeleton className="h-28" />
      <Skeleton className="h-28" />
    </div>
    <Skeleton className="h-80 w-full" />
    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
      <Skeleton className="h-12" />
      <Skeleton className="h-12" />
      <Skeleton className="h-12" />
      <Skeleton className="h-12" />
    </div>
  </div>
);

export const WorkoutCardSkeleton = () => (
  <div className="bg-gray-800 rounded-lg p-4 mb-4 border border-gray-700 space-y-4">
    <div className="flex justify-between">
      <Skeleton className="h-6 w-1/3" />
    </div>
    <div className="space-y-3">
      <Skeleton className="h-20 w-full" />
      <Skeleton className="h-20 w-full" />
    </div>
  </div>
);

export const HistorySkeleton = () => (
  <div className="space-y-4">
    <WorkoutCardSkeleton />
    <WorkoutCardSkeleton />
    <WorkoutCardSkeleton />
  </div>
);
