import { NextResponse } from 'next/server';
import dbConnect from '@/lib/db';
import Routine from '@/lib/models/Routine';
import { getUserId } from "@/lib/apiUser";

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
    await dbConnect();
    const routine = await Routine.findOne({ _id: id, userId: userId });
    
    if (!routine) {
      return NextResponse.json({ success: false, error: 'Routine not found' }, { status: 404 });
    }
    return NextResponse.json({ success: true, data: routine });
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
    await dbConnect();
    const routine = await Routine.findOneAndDelete({ _id: id, userId: userId });

    if (!routine) {
      return NextResponse.json({ success: false, error: 'Routine not found or unauthorized' }, { status: 404 });
    }
    return NextResponse.json({ success: true, data: {} });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    return NextResponse.json({ success: false, error: message }, { status: 400 });
  }
}
