ALTER TABLE "user_settings" ADD COLUMN "days_per_week" integer DEFAULT 3 NOT NULL;--> statement-breakpoint
ALTER TABLE "user_settings" ADD COLUMN "goal" text DEFAULT 'Muscle' NOT NULL;--> statement-breakpoint
ALTER TABLE "user_settings" ADD COLUMN "plan_start" date;