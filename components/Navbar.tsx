'use client';

import Link from 'next/link';
import React from 'react';
import { useSession, signIn, signOut } from "next-auth/react"

const Navbar: React.FC = () => {
  const { data: session } = useSession();

  return (
    <nav className="bg-gray-900 border-b border-gray-800 text-white sticky top-0 z-50">
      <div className="container mx-auto px-4 py-3 flex justify-between items-center">
        <Link href="/" className="text-xl font-black text-blue-500 tracking-tighter hover:text-blue-400 transition-colors">
          GYM TRACKER
        </Link>
        <div className="flex items-center space-x-6 text-sm font-semibold">
          <div className="hidden sm:flex items-center space-x-6">
            <Link href="/" className="hover:text-blue-400 transition-colors">Dashboard</Link>
            <Link href="/routines" className="hover:text-blue-400 transition-colors">Routines</Link>
            <Link href="/workout" className="hover:text-blue-400 transition-colors">Log</Link>
            <Link href="/history" className="hover:text-blue-400 transition-colors">History</Link>
            <Link href="/settings" className="hover:text-blue-400 transition-colors">Settings</Link>
          </div>
          
          {session ? (
            <div className="flex items-center space-x-4 ml-4 pl-4 border-l border-gray-700">
              <span className="text-gray-400 hidden sm:inline">{session.user?.name}</span>
              <button 
                onClick={() => signOut()}
                className="bg-gray-800 hover:bg-gray-700 text-gray-300 px-3 py-1 rounded transition-colors"
              >
                Sign Out
              </button>
            </div>
          ) : (
            <button 
              onClick={() => signIn()}
              className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-1 rounded transition-colors"
            >
              Sign In
            </button>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
