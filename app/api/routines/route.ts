import { NextResponse } from 'next/server';
import { desc, eq } from 'drizzle-orm';
import { db } from '@/lib/db';
import { routines } from '@/lib/schema';
import { getUserId } from "@/lib/apiUser";
import { parseStringList, withMongoId } from '@/lib/validate';

export async function GET(request: Request) {
  try {
    const userId = await getUserId(request);
    if (!userId) {
      return NextResponse.json({ success: false, error: 'Unauthorized' }, { status: 401 });
    }

    const rows = await db.select().from(routines).where(eq(routines.userId, userId)).orderBy(desc(routines.createdAt));
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
    const exercises = parseStringList(body.exercises);
    if (typeof body.name !== 'string' || !body.name.trim() || !exercises) {
      return NextResponse.json({ success: false, error: 'Invalid routine' }, { status: 400 });
    }

    const [routine] = await db.insert(routines).values({ userId, name: body.name.trim(), exercises }).returning();
    return NextResponse.json({ success: true, data: withMongoId(routine) }, { status: 201 });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    return NextResponse.json({ success: false, error: message }, { status: 400 });
  }
}
