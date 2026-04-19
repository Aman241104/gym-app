import { IWorkout } from './stats';

export interface Suggestion {
  exerciseName: string;
  suggestedWeight: number;
  suggestedReps: number;
  reason: string;
}

export const getWorkoutSuggestions = (lastWorkout: IWorkout): Suggestion[] => {
  return lastWorkout.exercises.map(ex => {
    // Find the max weight used in the last workout for this exercise
    const maxWeight = Math.max(...ex.sets.map(s => s.weight));
    const totalReps = ex.sets.reduce((sum, s) => sum + s.reps, 0);
    const avgReps = totalReps / ex.sets.length;

    let suggestedWeight = maxWeight;
    let suggestedReps = Math.round(avgReps);
    let reason = "Maintain current intensity to build consistency.";

    if (avgReps >= 10) {
      suggestedWeight = maxWeight + 2.5;
      reason = "Great job! Increasing weight by 2.5kg due to high rep volume.";
    } else if (avgReps < 5 && maxWeight > 0) {
      suggestedWeight = Math.max(0, maxWeight - 2.5);
      reason = "Focus on form. Reducing weight slightly to hit higher rep ranges.";
    } else if (avgReps >= 8) {
      suggestedReps = Math.min(12, Math.round(avgReps + 1));
      reason = "Good progress. Try to push for one extra rep per set.";
    }

    return {
      exerciseName: ex.name,
      suggestedWeight,
      suggestedReps,
      reason
    };
  });
};
