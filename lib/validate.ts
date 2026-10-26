import type { Exercise } from "./schema";

// Request bodies come from the web UI and the phone; check them before they reach the database.
export function parseExercises(value: unknown): Exercise[] | null {
  if (!Array.isArray(value)) return null;
  const out: Exercise[] = [];
  for (const e of value) {
    if (typeof e?.name !== "string" || !e.name.trim() || !Array.isArray(e.sets)) return null;
    const sets = [];
    for (const s of e.sets) {
      const reps = Number(s?.reps), weight = Number(s?.weight);
      if (!Number.isFinite(reps) || !Number.isFinite(weight) || reps < 0 || weight < 0) return null;
      sets.push({ reps, weight });
    }
    out.push({ name: e.name.trim(), sets });
  }
  return out;
}

export function parseDate(value: unknown): Date | null {
  if (value === undefined || value === null) return new Date();
  const d = new Date(value as string);
  return Number.isNaN(d.getTime()) ? null : d;
}

export function parseStringList(value: unknown): string[] | null {
  return Array.isArray(value) && value.every((v) => typeof v === "string") ? value : null;
}

/** Both clients read `_id` (the Mongo-era field name), so rows go out with it. */
export function withMongoId<T extends { id: string }>({ id, ...rest }: T) {
  return { _id: id, ...rest };
}
