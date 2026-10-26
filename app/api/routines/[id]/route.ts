import { NextResponse } from 'next/server';
import { and, eq } from 'drizzle-orm';
import { db } from '@/lib/db';
import { routines } from '@/lib/schema';
import { getUserId } from "@/lib/apiUser";
import { withMongoId } from '@/lib/validate';

const own = (id: string, userId: string) => and(eq(routines.id, id), eq(routines.userId, userId));

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
    const [routine] = await db.select().from(routines).where(own(id, userId));

    if (!routine) {
      return NextResponse.json({ success: false, error: 'Routine not found' }, { status: 404 });
    }
    return NextResponse.json({ success: true, data: withMongoId(routine) });
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
    const [routine] = await db.delete(routines).where(own(id, userId)).returning({ id: routines.id });

    if (!routine) {
      return NextResponse.json({ success: false, error: 'Routine not found or unauthorized' }, { status: 404 });
    }
    return NextResponse.json({ success: true, data: {} });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    return NextResponse.json({ success: false, error: message }, { status: 400 });
  }
}
