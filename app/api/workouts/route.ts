import { NextResponse } from 'next/server';
import { desc, eq } from 'drizzle-orm';
import { db } from '@/lib/db';
import { workouts } from '@/lib/schema';
import { getUserId } from "@/lib/apiUser";
import { parseDate, parseExercises, withMongoId } from '@/lib/validate';

export async function GET(request: Request) {
  try {
    const userId = await getUserId(request);
    if (!userId) {
      return NextResponse.json({ success: false, error: 'Unauthorized' }, { status: 401 });
    }

    const rows = await db.select().from(workouts).where(eq(workouts.userId, userId)).orderBy(desc(workouts.date));
    return NextResponse.json({ success: true, data: rows.map(withMongoId) });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    return NextResponse.json({ success: false, error: message }, { status: 400 });
  }
}

export async function POST(request: Request) {
  try {
    const userId = await getUserId(request);
    if (!userId) {
      return NextResponse.json({ success: false, error: 'Unauthorized' }, { status: 401 });
    }

    const body = await request.json();
    const exercises = parseExercises(body.exercises);
    const date = parseDate(body.date);
    const clientId = typeof body.clientId === 'string' ? body.clientId : null;
    if (!exercises || !date) {
      return NextResponse.json({ success: false, error: 'Invalid workout' }, { status: 400 });
    }

    const values = { userId, clientId, date, exercises };
    const [workout] = clientId
      ? await db.insert(workouts).values(values)
          .onConflictDoUpdate({ target: [workouts.userId, workouts.clientId], set: { date, exercises } })
          .returning()
      : await db.insert(workouts).values(values).returning();
    return NextResponse.json({ success: true, data: withMongoId(workout) }, { status: 201 });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    return NextResponse.json({ success: false, error: message }, { status: 400 });
  }
}

