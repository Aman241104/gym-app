import { NextResponse } from 'next/server';
import dbConnect from '@/lib/db';
import Workout from '@/lib/models/Workout';
import { getServerSession } from "next-auth/next";
import { authOptions } from "@/lib/auth";

export async function GET(
  request: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ success: false, error: 'Unauthorized' }, { status: 401 });
    }

    const { id } = await params;
    await dbConnect();
    const workout = await Workout.findOne({ _id: id, userId: session.user.id });
    
    if (!workout) {
      return NextResponse.json({ success: false, error: 'Workout not found' }, { status: 404 });
    }
    return NextResponse.json({ success: true, data: workout });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    return NextResponse.json({ success: false, error: message }, { status: 400 });
  }
}

export async function PUT(
  request: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ success: false, error: 'Unauthorized' }, { status: 401 });
    }

    const { id } = await params;
    await dbConnect();
    const body = await request.json();
    
    if (body.date) {
      body.date = new Date(body.date);
    }
    
    // Ensure the workout belongs to the user
    const workout = await Workout.findOneAndUpdate(
      { _id: id, userId: session.user.id },
      body,
      {
        new: true,
        runValidators: true,
      }
    );

    if (!workout) {
      return NextResponse.json({ success: false, error: 'Workout not found or unauthorized' }, { status: 404 });
    }
    return NextResponse.json({ success: true, data: workout });
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
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ success: false, error: 'Unauthorized' }, { status: 401 });
    }

    const { id } = await params;
    await dbConnect();
    
    // Ensure the workout belongs to the user
    const workout = await Workout.findOneAndDelete({ _id: id, userId: session.user.id });

    if (!workout) {
      return NextResponse.json({ success: false, error: 'Workout not found or unauthorized' }, { status: 404 });
    }
    return NextResponse.json({ success: true, data: {} });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Unknown error';
    return NextResponse.json({ success: false, error: message }, { status: 400 });
  }
}
