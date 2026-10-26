import { Schema, Document, model, models } from 'mongoose';

export interface ISet {
  reps: number;
  weight: number;
}

export interface IExercise {
  name: string;
  sets: ISet[];
}

export interface IWorkout extends Document {
  userId: string;
  clientId?: string;
  date: Date;
  exercises: IExercise[];
}

const SetSchema = new Schema<ISet>({
  reps: { type: Number, required: true },
  weight: { type: Number, required: true },
});

const ExerciseSchema = new Schema<IExercise>({
  name: { type: String, required: true },
  sets: [SetSchema],
});

const WorkoutSchema = new Schema<IWorkout>({
  userId: { type: String, required: true, index: true },
  // Set by the Android app so a retried offline save updates instead of duplicating.
  clientId: { type: String },
  date: { type: Date, default: Date.now },
  exercises: [ExerciseSchema],
});

WorkoutSchema.index({ userId: 1, clientId: 1 }, { unique: true, partialFilterExpression: { clientId: { $type: 'string' } } });

const Workout = models.Workout || model<IWorkout>('Workout', WorkoutSchema);

export default Workout;
