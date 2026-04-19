# Gym Tracker

A full-stack fitness tracking application built with Next.js 14, MongoDB, and Tailwind CSS.

## Features

- **Workout Logging**: Easily log exercises, sets, reps, and weights.
- **Dashboard**: Visualize your progress with total volume charts and personal records.
- **AI Coach Suggestions**: Get algorithmic advice on progressive overload based on your last workout.
- **Rest Timer**: Integrated timer to manage your recovery periods.
- **Streak System**: Stay motivated with a visual workout streak.
- **Exercise Analytics**: Detailed charts for individual exercises to track strength gains.
- **Authentication**: Secure multi-user support via NextAuth.js.

## Getting Started

### 1. Prerequisites

- Node.js 18+
- MongoDB instance (Local or Atlas)
- GitHub OAuth App (for authentication)

### 2. Environment Variables

Create a `.env.local` file in the root directory and add the following:

```env
MONGODB_URI=mongodb://localhost:27017/gym-tracker
NEXTAUTH_SECRET=your_nextauth_secret_here
NEXTAUTH_URL=http://localhost:3000

# GitHub OAuth (Optional - required for Login)
GITHUB_ID=your_github_client_id
GITHUB_SECRET=your_github_client_secret
```

*Note: You can generate a random secret for `NEXTAUTH_SECRET` using `openssl rand -base64 32`.*

### 3. Installation

```bash
npm install --legacy-peer-deps
```

### 4. Development

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) with your browser.

## Technologies

- **Framework**: [Next.js 14 (App Router)](https://nextjs.org/)
- **Database**: [MongoDB](https://www.mongodb.com/) with [Mongoose](https://mongoosejs.com/)
- **Authentication**: [NextAuth.js](https://next-auth.js.org/)
- **Styling**: [Tailwind CSS](https://tailwindcss.com/)
- **Charts**: [Recharts](https://recharts.org/)
- **Icons**: [Lucide React](https://lucide.dev/) (optional)

## Project Structure

- `app/`: Routing and Pages
- `components/`: Reusable UI components
- `lib/`: Database config, models, and auth options
- `utils/`: Calculation logic and analytics
- `md/`: Project documentation and implementation plans
