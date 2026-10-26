import { NextResponse } from 'next/server';
import { desc, eq } from 'drizzle-orm';
import { db } from '@/lib/db';
import { runs } from '@/lib/schema';
import { getUserId } from "@/lib/apiUser";
import { parseDate, withMongoId } from '@/lib/validate';

export async function GET(request: Request) {
  try {
    const userId = await getUserId(request);
    if (!userId) {
      return NextResponse.json({ success: false, error: 'Unauthorized' }, { status: 401 });
    }

    const rows = await db.select().from(runs).where(eq(runs.userId, userId)).orderBy(desc(runs.date));
    return NextResponse.json({ success: true, data: rows.map(withMongoId) });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    return NextResponse.json({ success: false, error: message }, { status: 400 });
  }
}

const count = (v: unknown, max: number) => Number.isInteger(v) && (v as number) >= 0 && (v as number) <= max;

export async function POST(request: Request) {
  try {
    const userId = await getUserId(request);
    if (!userId) {
      return NextResponse.json({ success: false, error: 'Unauthorized' }, { status: 401 });
    }

    const body = await request.json();
    const date = parseDate(body.date);
    const clientId = typeof body.clientId === 'string' ? body.clientId : null;
    const plan = body.plan === undefined || body.plan === null ? null : body.plan;
    const valid = date && clientId &&
      (plan === null || (typeof plan === 'string' && /^W\d{1,2}D\d$/.test(plan))) &&
      count(body.durationSec, 86_400) && count(body.distanceM, 500_000) && typeof body.completed === 'boolean';
    if (!valid) {
      return NextResponse.json({ success: false, error: 'Invalid run' }, { status: 400 });
    }

    const values = { userId, clientId, date, plan, durationSec: body.durationSec, distanceM: body.distanceM, completed: body.completed };
    // Upsert by clientId so the phone's offline retries never duplicate a run.
    const [run] = await db.insert(runs).values(values)
      .onConflictDoUpdate({ target: [runs.userId, runs.clientId], set: values })
      .returning();
    return NextResponse.json({ success: true, data: withMongoId(run) }, { status: 201 });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    return NextResponse.json({ success: false, error: message }, { status: 400 });
  }
}
