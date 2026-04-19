import { NextResponse } from 'next/server';
import dbConnect from '@/lib/db';
import Workout from '@/lib/models/Workout';
import { getServerSession } from "next-auth/next";
import { authOptions } from "@/lib/auth";

export async function GET() {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ success: false, error: 'Unauthorized' }, { status: 401 });
    }

    await dbConnect();
    const workouts = await Workout.find({ userId: session.user.id }).sort({ date: -1 });
    return NextResponse.json({ success: true, data: workouts });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    return NextResponse.json({ success: false, error: message }, { status: 400 });
  }
}

export async function POST(request: Request) {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ success: false, error: 'Unauthorized' }, { status: 401 });
    }

    await dbConnect();
    const body = await request.json();
    const workoutData = {
      ...body,
      userId: session.user.id,
      date: body.date ? new Date(body.date) : new Date()
    };
    const workout = await Workout.create(workoutData);
    return NextResponse.json({ success: true, data: workout }, { status: 201 });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    return NextResponse.json({ success: false, error: message }, { status: 400 });
  }
}
