import { attachDatabasePool } from "@vercel/functions";
import { drizzle } from "drizzle-orm/node-postgres";
import { Pool } from "pg";
import * as schema from "./schema";

// Pooled Neon URL for app traffic; migrations use DATABASE_URL_UNPOOLED (see drizzle.config.ts).
const pool = new Pool({ connectionString: process.env.DATABASE_URL });
attachDatabasePool(pool);

export const db = drizzle(pool, { schema });
