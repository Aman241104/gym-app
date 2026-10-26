import { NextResponse } from 'next/server';
import dbConnect from '@/lib/db';
import UserSettings from '@/lib/models/UserSettings';
import { getUserId } from "@/lib/apiUser";

export async function GET(request: Request) {
  try {
    const userId = await getUserId(request);
    if (!userId) {
      return NextResponse.json({ success: false, error: 'Unauthorized' }, { status: 401 });
    }

    await dbConnect();
    let settings = await UserSettings.findOne({ userId: userId });
    
    if (!settings) {
      settings = await UserSettings.create({ userId: userId, unit: 'kg' });
    }

    return NextResponse.json({ success: true, data: settings });
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
    
    const settings = await UserSettings.findOneAndUpdate(
      { userId: userId },
      { ...body, userId: userId },
      { new: true, upsert: true, runValidators: true }
    );

    return NextResponse.json({ success: true, data: settings });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    return NextResponse.json({ success: false, error: message }, { status: 400 });
  }
}
