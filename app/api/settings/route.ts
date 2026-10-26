import { NextResponse } from 'next/server';
import { eq } from 'drizzle-orm';
import { db } from '@/lib/db';
import { userSettings } from '@/lib/schema';
import { getUserId } from "@/lib/apiUser";
import { parseStringList } from '@/lib/validate';

export async function GET(request: Request) {
  try {
    const userId = await getUserId(request);
    if (!userId) {
      return NextResponse.json({ success: false, error: 'Unauthorized' }, { status: 401 });
    }

    const [settings] = await db.select().from(userSettings).where(eq(userSettings.userId, userId));
    return NextResponse.json({ success: true, data: settings ?? { userId, unit: 'kg', equipment: [], daysPerWeek: 3, goal: 'Muscle', planStart: null, bodyWeightKg: null, dietType: null } });
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
    const unit = body.unit;
    const equipment = body.equipment === undefined ? undefined : parseStringList(body.equipment);
    const daysPerWeek = body.daysPerWeek;
    const goal = body.goal;
    const planStart = body.planStart;
    const bodyWeightKg = body.bodyWeightKg;
    const dietType = body.dietType;
    const valid =
      (unit === undefined || unit === 'kg' || unit === 'lbs') &&
      equipment !== null &&
      (daysPerWeek === undefined || (Number.isInteger(daysPerWeek) && daysPerWeek >= 2 && daysPerWeek <= 6)) &&
      (goal === undefined || ['Strength', 'Muscle', 'FatLoss', 'General'].includes(goal)) &&
      (planStart === undefined || planStart === null || /^\d{4}-\d{2}-\d{2}$/.test(planStart)) &&
      (bodyWeightKg === undefined || bodyWeightKg === null || (typeof bodyWeightKg === 'number' && bodyWeightKg >= 25 && bodyWeightKg <= 300)) &&
      (dietType === undefined || dietType === null || ['veg', 'egg', 'nonveg', 'jain'].includes(dietType));
    if (!valid) {
      return NextResponse.json({ success: false, error: 'Invalid settings' }, { status: 400 });
    }

    // Partial update: the web settings page only sends `unit`, the phone sends everything.
    const changes = Object.fromEntries(
      Object.entries({ unit, equipment, daysPerWeek, goal, planStart, bodyWeightKg, dietType }).filter(([, v]) => v !== undefined)
    );
    const [settings] = await db.insert(userSettings).values({ userId, ...changes })
      .onConflictDoUpdate({ target: userSettings.userId, set: Object.keys(changes).length ? changes : { userId } })
      .returning();
    return NextResponse.json({ success: true, data: settings });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    return NextResponse.json({ success: false, error: message }, { status: 400 });
  }
}
