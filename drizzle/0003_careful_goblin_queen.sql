CREATE TABLE "food_logs" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"user_id" text NOT NULL,
	"client_id" text NOT NULL,
	"day" date NOT NULL,
	"name" text NOT NULL,
	"servings" real NOT NULL,
	"kcal" real NOT NULL,
	"protein" real NOT NULL,
	"carbs" real NOT NULL,
	"fat" real NOT NULL
);
--> statement-breakpoint
ALTER TABLE "user_settings" ADD COLUMN "body_weight_kg" real;--> statement-breakpoint
ALTER TABLE "user_settings" ADD COLUMN "diet_type" text;--> statement-breakpoint
CREATE INDEX "food_logs_user_day_idx" ON "food_logs" USING btree ("user_id","day");--> statement-breakpoint
CREATE UNIQUE INDEX "food_logs_user_client_idx" ON "food_logs" USING btree ("user_id","client_id");