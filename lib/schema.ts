import { index, jsonb, pgTable, text, timestamp, uniqueIndex, uuid } from "drizzle-orm/pg-core";

export type SetEntry = { reps: number; weight: number };
export type Exercise = { name: string; sets: SetEntry[] };

// Sessions keep the same nested shape the Mongo documents had, so the exercises live in one jsonb column.
export const workouts = pgTable("workouts", {
  id: uuid("id").primaryKey().defaultRandom(),
  userId: text("user_id").notNull(),
  // Set by the Android app so a retried offline save updates instead of duplicating.
  clientId: text("client_id"),
  date: timestamp("date", { withTimezone: true }).notNull().defaultNow(),
  exercises: jsonb("exercises").$type<Exercise[]>().notNull(),
}, (t) => [
  index("workouts_user_date_idx").on(t.userId, t.date),
  uniqueIndex("workouts_user_client_idx").on(t.userId, t.clientId),
]);

export const routines = pgTable("routines", {
  id: uuid("id").primaryKey().defaultRandom(),
  userId: text("user_id").notNull(),
  name: text("name").notNull(),
  exercises: jsonb("exercises").$type<string[]>().notNull(),
  createdAt: timestamp("created_at", { withTimezone: true }).notNull().defaultNow(),
}, (t) => [index("routines_user_idx").on(t.userId)]);

export const userSettings = pgTable("user_settings", {
  userId: text("user_id").primaryKey(),
  unit: text("unit", { enum: ["kg", "lbs"] }).notNull().default("kg"),
  equipment: jsonb("equipment").$type<string[]>().notNull().default([]),
});
