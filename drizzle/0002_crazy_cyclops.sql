CREATE TABLE "runs" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"user_id" text NOT NULL,
	"client_id" text,
	"date" timestamp with time zone NOT NULL,
	"plan" text,
	"duration_sec" integer NOT NULL,
	"distance_m" integer NOT NULL,
	"completed" boolean NOT NULL
);
--> statement-breakpoint
CREATE INDEX "runs_user_date_idx" ON "runs" USING btree ("user_id","date");--> statement-breakpoint
CREATE UNIQUE INDEX "runs_user_client_idx" ON "runs" USING btree ("user_id","client_id");