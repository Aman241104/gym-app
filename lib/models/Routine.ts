import { Schema, model, models, Document } from 'mongoose';

export interface IRoutine extends Document {
  userId: string;
  name: string;
  exercises: string[];
}

const RoutineSchema = new Schema<IRoutine>({
  userId: { type: String, required: true, index: true },
  name: { type: String, required: true },
  exercises: [{ type: String, required: true }],
}, {
  timestamps: true
});

const Routine = models.Routine || model<IRoutine>('Routine', RoutineSchema);

export default Routine;
