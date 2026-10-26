# Vision: one personal app for everything

Last updated: 2026-10-24. Full research report (sources, licenses, diagrams):
https://claude.ai/artifact/1MGsEKXXP5pYghPewSLLeG

## Goal

Grow this gym tracker into one personal "everything" app for a single user (the owner).
It needs award-level polish with the same liquid-glass look as MindCore. One login, one Today screen,
and one module per area of life.

## About the user (drives every feature)

- Has posture issues: forward head and rounded shoulders, so posture work is a priority.
- Trains with real equipment, including a back-row machine and more. Never assume bodyweight only.
  The app gives a full equipment checklist and plans from what is ticked.
- Has just started running.
- Eats an Indian diet, so log foods in Indian units (katori, roti) with veg/non-veg/Jain options.
- Wants a photo mode that teaches posing, checks posture, and suggests which muscles to improve.
- Wants guided stretching taught step by step.

## Modules and build order

1. **Train (gym)**, built first and the biggest module:
   - Planner: equipment + goals + posture flags + run level + diet → weekly plan.
   - Lifts, a daily 10-minute posture/stretch block, runs and meals.
   - Photo mode: posing coach, posture scan, symmetry check, progress gallery.
   - Running: C25K → easy/tempo runs, live GPS screen, pace zones.
   - Indian nutrition: INDB + IFCT foods, protein gap fixer, barcode scan.
2. **Mind (meditation):** breathing patterns, a timer and streaks. Reuses the stretch-player timer.
3. **Career:** build on `../job-serach` (Job Search OS, already exists). Ideas from JobSync (MIT, Next.js).
4. **Read:** book shelf, pages/day and highlights via the Open Library API. Highlights go to MindCore.
5. **Skin:** AM/PM routine, product shelf, ingredient-clash reminders (Open Beauty Facts), face photos.
6. **Style:** wardrobe from photos, outfit builder, style guide from body measurements.

Each module gets its own folder, routes and collections, so it could be split out later.
Shared parts: the user, the Today dashboard and the design system.

## Key open-source parts (copy freely unless noted)

| Area | Use | License |
|---|---|---|
| Exercises | free-exercise-db (870+ exercises with images, incl. stretches) | Unlicense |
| Plan/session UX | workout.cool (Next.js) | MIT |
| Pose/photo | MediaPipe Pose Landmarker | Apache-2.0 |
| Bodybuilding poses | Gladiator Pose Analyzer (HF Space) | MIT |
| Indian food | INDB (1,014 recipes, per serving) + ifct2017 (MIT versions before Apr 2025) | Credit the authors |
| Packaged food | Open Food Facts API | ODbL |
| Muscle map | body-highlighter / react-body-highlighter | Check |
| Web glass | liquid-glass-react, Motion, React Bits, GSAP | MIT / free |
| Running | OpenTracks ideas (Apache-2.0); C25K structure | — |

Ideas only, never copy code (AGPL/GPL): wger, Liftosaur, MyFit, Medito, Libre Closet, C2K, Kalimory.
Never use exercises-dataset GIFs (© Gym visual).
Commercial apps (Fitbod, Hevy, Runna, HealthifyMe, MacroFactor, Strava) are for studying features
and UX only. Never copy their code or assets.

## MindCore (the owner's second-brain app)

- Android app: `~/stash/app`. Kotlin + Jetpack Compose.
  Glass lives in `app/src/main/java/app/mindcore/ui/glass/`
  (Kyant0 Backdrop `io.github.kyant0:backdrop` 2.0.1, Apache-2.0).
- Theme: dark `#F5A9BE` on `#0B090A`, light `#A23A5A` on `#FBF7F4`.
- Backend: `~/stash/brain` (Python), `~/stash/cloud` (Cloudflare Workers/D1/Vectorize). Plan: `~/stash/PLAN.md`.
- What it does: share reels, links or screenshots → AI extracts and verifies repos, tools, courses
  and jobs → Q&A with citations. It does NOT do fitness, meditation, reading habits or skincare.
- `../habit-tracker-forher-android` already ported this glass into another Compose app.

## Platform (decided 2026-10-27)

**Native Android (Kotlin + Compose)** in `android/`, with MindCore's exact glass and Lotus theme.
The Next.js + Neon Postgres code is the backend. The app is local-first: writes go to the phone, then an
outbox replays them to the API. The app authenticates with `Authorization: Bearer $APP_TOKEN` and acts
as `OWNER_USER_ID` (see `lib/apiUser.ts`). Both env vars must be set on the server.

## Current state of this repo

- Next.js 16 + Neon Postgres (Drizzle) + next-auth (GitHub, JWT) + Recharts. Workout logging, history, dashboard and an
  overload suggestion card.
- See `implementation-plan.md` for the original phases (all done).
- `android/` (Kotlin + Compose, tabs Today / Train / Run / Eat / You):
  - Train: logging with rest timer and overload prefill, edit/delete history, free-exercise-db library
    (876 exercises, how-to sheet), rules-first weekly planner (equipment + days + goal, rear-delt slot,
    deloads, swaps).
  - Posture: guided 10-min routine with voice cues; on-device MediaPipe posture scan (side: head-forward
    and lean angles; front: shoulder/hip tilt), photos never leave the phone.
  - Run: Couch-to-5K (27 sessions) on non-lifting days; GPS run screen as a location foreground service.
  - Eat: food log in Indian units (own approximate cooked-food list + IFCT 2017 raw foods, MIT),
    diet type incl. Jain, protein target from weight + goal, protein-gap suggestions.
  - Local-first with an outbox; syncs workouts, runs, food and settings to the API.
  Build: `cd android && ./gradlew assembleDebug`.
- Design system (`android/.../ui/design`): per-module accents (Train pink, Run peach, Eat sage,
  Posture lavender, Mind sky), Instrument Serif italic phrase in headers, animated rings, count-ups,
  rise-in cards with spring press + haptics, drifting aurora. Today = 4 daily rings + week strip.
- Also built: 7-day muscle balance, posture then/now photos, barcode scan (Open Food Facts),
  Mind module v1 (box / 4-7-8 / coherent breathing).
- Still to build: live posing coach (CameraX + ghost outline), then Career / Read / Skin / Style.
  Web login needs a GitHub OAuth app for the Vercel URL.
- Database moved to Neon on 2026-10-27 (Atlas cluster was gone). See `md/database.md`.
