# Gym Tracker - Database Knowledge

The application uses MongoDB via Mongoose for data persistence.

### Data Model:
- **Workout**:
  - `date`: Date (default: now)
  - `exercises`: Array of Exercise objects
- **Exercise**:
  - `name`: String (e.g., "Bench Press")
  - `sets`: Array of Set objects
- **Set**:
  - `reps`: Number
  - `weight`: Number
  - `isCompleted`: Boolean (default: true)

### Mongoose Schema Design:
- **Workout Schema**:
  - `date`: { type: Date, default: Date.now }
  - `exercises`: [ExerciseSchema]
- **Exercise Schema**:
  - `name`: String
  - `sets`: [SetSchema]
- **Set Schema**:
  - `reps`: Number
  - `weight`: Number

### Connection Management:
- Use a cached Mongoose connection to prevent multiple connections during development hot-reloads in Next.js.
