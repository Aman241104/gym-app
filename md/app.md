# Gym Tracker - App Knowledge

The application is built using Next.js 14 with the App Router architecture.

### Architecture:
- **Framework**: Next.js (TypeScript)
- **Styling**: Tailwind CSS
- **Visualization**: Recharts for progress graphs.
- **API**: Next.js Route Handlers (`/app/api/workouts`).

### Folder Structure:
- `app/`: Routing and pages (Dashboard, Workout Log, History).
- `components/`: UI components (WorkoutForm, WorkoutCard, Dashboard summary).
- `lib/`: Database configuration and models.
- `utils/`: Logic for calculations (volume, PRs, suggestions).

### Component Highlights:
- `WorkoutForm`: Dynamic form with "Add Exercise" and "Add Set" functionality.
- `Dashboard`: Summarizes metrics like "Total Volume" and "Weekly Progress".
- `WorkoutCard`: Used in history to display a single workout session.
- `AI Coach`: Utility function that suggests weight/rep adjustments based on previous performance.
