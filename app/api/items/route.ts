import { NextResponse } from 'next/server';
import { and, eq } from 'drizzle-orm';
import { db } from '@/lib/db';
import { items } from '@/lib/schema';
import { getUserId } from "@/lib/apiUser";

// Modules the apps may store; anything else is rejected so typos don't create junk collections.
const MODULES = ['read', 'skin', 'career', 'style'];
const isModule = (m: unknown): m is string => typeof m === 'string' && MODULES.includes(m);
const MAX_BYTES = 20_000;

const unauthorized = () => NextResponse.json({ success: false, error: 'Unauthorized' }, { status: 401 });
const bad = (error: string) => NextResponse.json({ success: false, error }, { status: 400 });

// GET /api/items?module=read
export async function GET(request: Request) {
  try {
    const userId = await getUserId(request);
    if (!userId) return unauthorized();
    const mod = new URL(request.url).searchParams.get('module');
    if (!isModule(mod)) return bad('Unknown module');

    const rows = await db.select().from(items).where(and(eq(items.userId, userId), eq(items.module, mod)));
    return NextResponse.json({ success: true, data: rows.map(r => ({ clientId: r.clientId, data: r.data })) });
  } catch (error: unknown) {
    return bad(error instanceof Error ? error.message : 'Unknown error');
  }
}

// POST { module, clientId, data } upserts one document.
export async function POST(request: Request) {
  try {
    const userId = await getUserId(request);
    if (!userId) return unauthorized();
    const body = await request.json();
    if (!isModule(body.module) || typeof body.clientId !== 'string' || !body.clientId ||
        typeof body.data !== 'object' || body.data === null || Array.isArray(body.data) ||
        JSON.stringify(body.data).length > MAX_BYTES) {
      return bad('Invalid item');
    }

    const values = { userId, module: body.module, clientId: body.clientId, data: body.data, updatedAt: new Date() };
    await db.insert(items).values(values)
      .onConflictDoUpdate({ target: [items.userId, items.module, items.clientId], set: { data: body.data, updatedAt: new Date() } });
    return NextResponse.json({ success: true, data: { clientId: body.clientId, data: body.data } }, { status: 201 });
  } catch (error: unknown) {
    return bad(error instanceof Error ? error.message : 'Unknown error');
  }
}

// DELETE /api/items?module=read&clientId=...
export async function DELETE(request: Request) {
  try {
    const userId = await getUserId(request);
    if (!userId) return unauthorized();
    const params = new URL(request.url).searchParams;
    const mod = params.get('module');
    const clientId = params.get('clientId');
    if (!isModule(mod) || !clientId) return bad('module and clientId required');

    await db.delete(items).where(and(eq(items.userId, userId), eq(items.module, mod), eq(items.clientId, clientId)));
    return NextResponse.json({ success: true, data: {} });
  } catch (error: unknown) {
    return bad(error instanceof Error ? error.message : 'Unknown error');
  }
}
