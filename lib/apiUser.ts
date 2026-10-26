import { timingSafeEqual } from "crypto";
import { getServerSession } from "next-auth/next";
import { authOptions } from "@/lib/auth";

// Single-user app: the Android app sends `Authorization: Bearer <APP_TOKEN>` and acts as OWNER_USER_ID.
// The website keeps using its next-auth session.
export async function getUserId(request: Request): Promise<string | null> {
  const token = process.env.APP_TOKEN;
  const owner = process.env.OWNER_USER_ID;
  const header = request.headers.get("authorization") ?? "";
  if (token && owner && header.startsWith("Bearer ")) {
    const given = Buffer.from(header.slice(7));
    const expected = Buffer.from(token);
    return given.length === expected.length && timingSafeEqual(given, expected) ? owner : null;
  }
  const session = await getServerSession(authOptions);
  return session?.user?.id ?? null;
}
