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
    return NextResponse.json({ success: true, data: settings ?? { userId, unit: 'kg', equipment: [] } });
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
    const unit = body.unit === undefined ? undefined : body.unit;
    const equipment = body.equipment === undefined ? undefined : parseStringList(body.equipment);
    if ((unit !== undefined && unit !== 'kg' && unit !== 'lbs') || equipment === null) {
      return NextResponse.json({ success: false, error: 'Invalid settings' }, { status: 400 });
    }

    // Partial update: the web settings page only sends `unit`, the phone sends both.
    const changes = { ...(unit && { unit }), ...(equipment && { equipment }) };
    const [settings] = await db.insert(userSettings).values({ userId, ...changes })
      .onConflictDoUpdate({ target: userSettings.userId, set: changes.unit || changes.equipment ? changes : { userId } })
      .returning();
    return NextResponse.json({ success: true, data: settings });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    return NextResponse.json({ success: false, error: message }, { status: 400 });
  }
}
