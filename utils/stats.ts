import { EXERCISE_MUSCLE_MAP } from './exerciseMap';

export interface IWorkout {
  _id?: string;
  date: string;
  exercises: {
    name: string;
    sets: { reps: number; weight: number }[];
  }[];
}

export const calculateTotalVolume = (workouts: IWorkout[]) => {
  return workouts.reduce((total, workout) => {
    const workoutVolume = workout.exercises.reduce((exTotal, ex) => {
      const exVolume = ex.sets.reduce((setTotal, set) => setTotal + (set.reps * set.weight), 0);
      return exTotal + exVolume;
    }, 0);
    return total + workoutVolume;
  }, 0);
};

export const getPersonalRecords = (workouts: IWorkout[]) => {
  const prs: Record<string, number> = {};
  workouts.forEach(workout => {
    workout.exercises.forEach(ex => {
      ex.sets.forEach(set => {
        if (!prs[ex.name] || set.weight > prs[ex.name]) {
          prs[ex.name] = set.weight;
        }
      });
    });
  });
  return prs;
};

export const getWeeklyProgress = (workouts: IWorkout[]) => {
  // Group volume by date
  const progress = workouts.map(w => {
    const volume = w.exercises.reduce((acc, ex) => {
      return acc + ex.sets.reduce((sAcc, s) => sAcc + (s.reps * s.weight), 0);
    }, 0);
    return {
      date: new Date(w.date).toLocaleDateString(undefined, { month: 'short', day: 'numeric' }),
      volume
    };
  }).reverse(); // Chronological order
  
  return progress;
};

export const calculateStreak = (workouts: IWorkout[]) => {
  if (workouts.length === 0) return 0;

  // Sort by date descending
  const sortedDates = workouts
    .map(w => new Date(w.date).setHours(0, 0, 0, 0))
    .sort((a, b) => b - a);

  // Remove duplicates
  const uniqueDates = Array.from(new Set(sortedDates));

  const today = new Date().setHours(0, 0, 0, 0);
  const yesterday = today - 86400000;

  // If the last workout was not today or yesterday, streak is 0
  if (uniqueDates[0] < yesterday) return 0;

  let streak = 0;
  let currentDate = today;

  // If latest workout is today, we check backwards from today.
  // If latest workout is yesterday, we check backwards from yesterday.
  if (uniqueDates[0] === yesterday) {
    currentDate = yesterday;
  } else if (uniqueDates[0] === today) {
    currentDate = today;
  } else {
    return 0;
  }

  for (let i = 0; i < uniqueDates.length; i++) {
    if (uniqueDates[i] === currentDate) {
      streak++;
      currentDate -= 86400000;
    } else {
      break;
    }
  }

  return streak;
};

export const getExerciseProgress = (workouts: IWorkout[], exerciseName: string) => {
  return workouts
    .map(w => {
      const exercise = w.exercises.find(ex => ex.name.toLowerCase() === exerciseName.toLowerCase());
      if (!exercise) return null;
      
      const maxWeight = Math.max(...exercise.sets.map(s => s.weight));
      return {
        date: new Date(w.date).toLocaleDateString(undefined, { month: 'short', day: 'numeric' }),
        weight: maxWeight
      };
    })
    .filter(item => item !== null)
    .reverse();
};

export const calculateVolumeByMuscleGroup = (workouts: IWorkout[]) => {
  const muscleGroups: Record<string, number> = {
    'Chest': 0,
    'Back': 0,
    'Legs': 0,
    'Shoulders': 0,
    'Arms': 0,
    'Core': 0,
    'Other': 0
  };

  workouts.forEach(workout => {
    workout.exercises.forEach(ex => {
      const muscleGroup = EXERCISE_MUSCLE_MAP[ex.name.toLowerCase()] || 'Other';
      const volume = ex.sets.reduce((total, set) => total + (set.reps * set.weight), 0);
      muscleGroups[muscleGroup] += volume;
    });
  });

  return Object.entries(muscleGroups)
    .filter(([, value]) => value > 0)
    .map(([name, value]) => ({ name, value }));
};
