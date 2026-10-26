# Database

Neon Postgres (project `withered-shape-18344792`, aws-ap-southeast-1) via Drizzle ORM + node-postgres.

- Schema: `lib/schema.ts`. Tables `workouts` (exercises as jsonb), `routines`, `user_settings`.
- Client: `lib/db.ts`, pooled `DATABASE_URL`, `attachDatabasePool` for Vercel Fluid compute.
- Migrations: edit the schema, then `npm run db:generate` and `npm run db:migrate`
  (runs over the direct `DATABASE_URL_UNPOOLED`).
- API responses keep the Mongo-era `_id` field (`withMongoId` in `lib/validate.ts`) so the web UI
  and the Android app didn't need changes.
- Users are GitHub account ids (next-auth JWT `token.sub`); the Android app's bearer token maps to
  `OWNER_USER_ID`, the owner's GitHub id.
