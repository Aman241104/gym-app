import { NextResponse } from 'next/server';
import { and, eq } from 'drizzle-orm';
import { db } from '@/lib/db';
import { workouts } from '@/lib/schema';
import { getUserId } from "@/lib/apiUser";
import { parseDate, parseExercises, withMongoId } from '@/lib/validate';

const own = (id: string, userId: string) => and(eq(workouts.id, id), eq(workouts.userId, userId));

export async function GET(
  request: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const userId = await getUserId(request);
    if (!userId) {
      return NextResponse.json({ success: false, error: 'Unauthorized' }, { status: 401 });
    }

    const { id } = await params;
    const [workout] = await db.select().from(workouts).where(own(id, userId));

    if (!workout) {
      return NextResponse.json({ success: false, error: 'Workout not found' }, { status: 404 });
    }
    return NextResponse.json({ success: true, data: withMongoId(workout) });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    return NextResponse.json({ success: false, error: message }, { status: 400 });
  }
}

export async function PUT(
  request: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const userId = await getUserId(request);
    if (!userId) {
      return NextResponse.json({ success: false, error: 'Unauthorized' }, { status: 401 });
    }

    const { id } = await params;
    const body = await request.json();
    const exercises = parseExercises(body.exercises);
    const date = body.date === undefined ? undefined : parseDate(body.date);
    if (!exercises || date === null) {
      return NextResponse.json({ success: false, error: 'Invalid workout' }, { status: 400 });
    }

    const [workout] = await db.update(workouts).set({ exercises, ...(date && { date }) }).where(own(id, userId)).returning();

    if (!workout) {
      return NextResponse.json({ success: false, error: 'Workout not found or unauthorized' }, { status: 404 });
    }
    return NextResponse.json({ success: true, data: withMongoId(workout) });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    return NextResponse.json({ success: false, error: message }, { status: 400 });
  }
}

export async function DELETE(
  request: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const userId = await getUserId(request);
    if (!userId) {
      return NextResponse.json({ success: false, error: 'Unauthorized' }, { status: 401 });
    }

    const { id } = await params;
    const [workout] = await db.delete(workouts).where(own(id, userId)).returning({ id: workouts.id });

    if (!workout) {
      return NextResponse.json({ success: false, error: 'Workout not found or unauthorized' }, { status: 404 });
    }
    return NextResponse.json({ success: true, data: {} });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    return NextResponse.json({ success: false, error: message }, { status: 400 });
  }
}
