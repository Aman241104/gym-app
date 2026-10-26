import { defineConfig } from "drizzle-kit";

process.loadEnvFile?.(".env.local");

export default defineConfig({
  schema: "./lib/schema.ts",
  out: "./drizzle",
  dialect: "postgresql",
  // Direct (non-pooler) connection: migrations need session state PgBouncer doesn't keep.
  dbCredentials: { url: process.env.DATABASE_URL_UNPOOLED! },
});
