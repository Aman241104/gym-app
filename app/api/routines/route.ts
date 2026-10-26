import { NextResponse } from 'next/server';
import dbConnect from '@/lib/db';
import Routine from '@/lib/models/Routine';
import { getUserId } from "@/lib/apiUser";

export async function GET(request: Request) {
  try {
    const userId = await getUserId(request);
    if (!userId) {
      return NextResponse.json({ success: false, error: 'Unauthorized' }, { status: 401 });
    }

    await dbConnect();
    const routines = await Routine.find({ userId: userId }).sort({ createdAt: -1 });
    return NextResponse.json({ success: true, data: routines });
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

    await dbConnect();
    const body = await request.json();
    const routineData = {
      ...body,
      userId: userId
    };
    const routine = await Routine.create(routineData);
    return NextResponse.json({ success: true, data: routine }, { status: 201 });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    return NextResponse.json({ success: false, error: message }, { status: 400 });
  }
}
