import { NextResponse } from 'next/server';
import { and, desc, eq, gte } from 'drizzle-orm';
import { db } from '@/lib/db';
import { foodLogs } from '@/lib/schema';
import { getUserId } from "@/lib/apiUser";
import { withMongoId } from '@/lib/validate';

const isDay = (v: unknown) => typeof v === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(v);
const amount = (v: unknown, max: number) => typeof v === 'number' && Number.isFinite(v) && v >= 0 && v <= max;

// GET returns the last 60 days; older food history isn't needed on the phone.
export async function GET(request: Request) {
  try {
    const userId = await getUserId(request);
    if (!userId) {
      return NextResponse.json({ success: false, error: 'Unauthorized' }, { status: 401 });
    }

    const since = new Date(Date.now() - 60 * 86_400_000).toISOString().slice(0, 10);
    const rows = await db.select().from(foodLogs)
      .where(and(eq(foodLogs.userId, userId), gte(foodLogs.day, since)))
      .orderBy(desc(foodLogs.day));
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

    const b = await request.json();
    const valid = typeof b.clientId === 'string' && isDay(b.day) && typeof b.name === 'string' && b.name.trim() &&
      amount(b.servings, 50) && amount(b.kcal, 20_000) && amount(b.protein, 1_000) && amount(b.carbs, 2_000) && amount(b.fat, 1_000);
    if (!valid) {
      return NextResponse.json({ success: false, error: 'Invalid food entry' }, { status: 400 });
    }

    const values = { userId, clientId: b.clientId, day: b.day, name: b.name.trim(), servings: b.servings, kcal: b.kcal, protein: b.protein, carbs: b.carbs, fat: b.fat };
    const [entry] = await db.insert(foodLogs).values(values)
      .onConflictDoUpdate({ target: [foodLogs.userId, foodLogs.clientId], set: values })
      .returning();
    return NextResponse.json({ success: true, data: withMongoId(entry) }, { status: 201 });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    return NextResponse.json({ success: false, error: message }, { status: 400 });
  }
}

// DELETE /api/food?clientId=...  (the phone deletes by its own id; deleting twice is fine)
export async function DELETE(request: Request) {
  try {
    const userId = await getUserId(request);
    if (!userId) {
      return NextResponse.json({ success: false, error: 'Unauthorized' }, { status: 401 });
    }

    const clientId = new URL(request.url).searchParams.get('clientId');
    if (!clientId) {
      return NextResponse.json({ success: false, error: 'clientId required' }, { status: 400 });
    }
    await db.delete(foodLogs).where(and(eq(foodLogs.userId, userId), eq(foodLogs.clientId, clientId)));
    return NextResponse.json({ success: true, data: {} });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    return NextResponse.json({ success: false, error: message }, { status: 400 });
  }
}
