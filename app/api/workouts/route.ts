import { NextResponse } from 'next/server';
import dbConnect from '@/lib/db';
import Workout from '@/lib/models/Workout';
import { getUserId } from "@/lib/apiUser";

export async function GET(request: Request) {
  try {
    const userId = await getUserId(request);
    if (!userId) {
      return NextResponse.json({ success: false, error: 'Unauthorized' }, { status: 401 });
    }

    await dbConnect();
    const workouts = await Workout.find({ userId: userId }).sort({ date: -1 });
    return NextResponse.json({ success: true, data: workouts });
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
    const workoutData = {
      ...body,
      userId: userId,
      date: body.date ? new Date(body.date) : new Date()
    };
    const workout = body.clientId
      ? await Workout.findOneAndUpdate(
          { userId, clientId: body.clientId },
          workoutData,
          { new: true, upsert: true, runValidators: true }
        )
      : await Workout.create(workoutData);
    return NextResponse.json({ success: true, data: workout }, { status: 201 });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    return NextResponse.json({ success: false, error: message }, { status: 400 });
  }
}
