import { Schema, model, models, Document } from 'mongoose';

export interface IUserSettings extends Document {
  userId: string;
  unit: 'kg' | 'lbs';
}

const UserSettingsSchema = new Schema<IUserSettings>({
  userId: { type: String, required: true, unique: true, index: true },
  unit: { type: String, enum: ['kg', 'lbs'], default: 'kg' },
});

const UserSettings = models.UserSettings || model<IUserSettings>('UserSettings', UserSettingsSchema);

export default UserSettings;
