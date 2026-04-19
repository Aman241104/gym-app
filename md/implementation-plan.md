# Gym Tracker - Implementation Plan

### Phase 1: Foundation & Setup
- [x] Initialize Next.js 14 app with TypeScript and Tailwind CSS.
- [x] Setup folder structure (`app/`, `components/`, `lib/`, `utils/`, `md/`).
- [x] Create documentation files in `md/`.

### Phase 2: Database & Backend
- [x] Configure MongoDB connection in `lib/db.ts`.
- [x] Define Mongoose models in `lib/models/Workout.ts`.
- [x] Create API routes:
  - `GET /api/workouts`
  - `POST /api/workouts`
  - `PUT /api/workouts/[id]`
  - `DELETE /api/workouts/[id]`

### Phase 3: Core UI Components
- [x] Create `WorkoutForm` component with dynamic exercise and set management.
- [x] Create `WorkoutCard` for history display.
- [x] Setup `History` page to fetch and list workouts.

### Phase 4: Analytics & Logic
- [x] Implement utility functions for volume and PR calculation in `utils/`.
- [x] Build the `Dashboard` with Recharts for visual progress.
- [x] Implement the "AI Coach" suggestion logic for progressive overload.

### Phase 5: Polishing & Validation
- [x] Apply dark theme and mobile responsiveness.
- [x] Add loading states and error handling.
- [x] Verify functionality through manual and basic automated tests (Linting passed).
- [x] Integrate AI Coach suggestions into the UI (SuggestionCard in Workout page).
- [x] Add Bonus: Rest Timer component (Integrated in Workout page).

### Phase 6: Advanced Features
- [x] **Exercise Analysis**: Dedicated page to view progress charts for specific exercises.
- [x] **Streak System**: Calculate and display current workout streak on the dashboard.
- [x] **Favorite Exercises**: "Quick Add" buttons for common movements in the WorkoutForm.
- [x] **Authentication (NextAuth)**: Secure the app and support multiple users.

### Phase 7: UI/UX Refinement
- [x] **Delete functionality**: Add delete buttons to WorkoutCards in History.
- [x] **Empty States**: Better empty state UI for Dashboard and History.
- [x] **Loading Skeletons**: Implement skeletons for a smoother loading experience.
- [x] **Edit Workout**: Ability to modify past entries.
- [x] **Muscle Group Analysis**: Visual breakdown of training distribution by muscle group.

### Phase 8: Personalization & Data Portability
- [x] **Global Units**: KG/LBS toggle in Settings.
- [x] **CSV Export**: Allow users to download their workout history.
- [x] **Settings Page**: Dedicated UI for user preferences and account management.

### Phase 9: Workout Routines & Templates
- [x] **Routine Model**: Database schema for saved workouts.
- [x] **API Routes**: GET/POST/DELETE for routines.
- [x] **Routine UI**: Dedicated page to manage and start workouts from templates.
- [x] **PPL Import**: Specific button to import user's PPL routine from images.

### Phase 10: Mobile Optimization & Consistency Tracking
- [x] **Mobile Bottom Nav**: Icon-based navigation for smartphone users.
- [x] **Consistency Calendar**: Visual heatmap of workout frequency on the dashboard.
