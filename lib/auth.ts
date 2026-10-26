import { NextAuthOptions } from "next-auth"
import GithubProvider from "next-auth/providers/github"

// JWT sessions: no auth tables needed. The user id is the GitHub account id (token.sub),
// which is also what OWNER_USER_ID holds for the Android app's token.
export const authOptions: NextAuthOptions = {
  providers: [
    GithubProvider({
      clientId: process.env.GITHUB_ID || "",
      clientSecret: process.env.GITHUB_SECRET || "",
    }),
  ],
  session: { strategy: "jwt" },
  callbacks: {
    session: async ({ session, token }) => {
      if (session.user && token.sub) {
        session.user.id = token.sub;
      }
      return session;
    },
  },
  secret: process.env.NEXTAUTH_SECRET,
}
