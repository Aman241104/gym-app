'use client';

import React, { createContext, useContext, useEffect, useState } from 'react';
import { useSession } from 'next-auth/react';

interface SettingsContextType {
  unit: 'kg' | 'lbs';
  setUnit: (unit: 'kg' | 'lbs') => void;
  loading: boolean;
}

const SettingsContext = createContext<SettingsContextType | undefined>(undefined);

export function SettingsProvider({ children }: { children: React.ReactNode }) {
  const { data: session, status } = useSession();
  const [unit, setUnitState] = useState<'kg' | 'lbs'>('kg');
  const [settingsFetched, setSettingsFetched] = useState(false);

  useEffect(() => {
    if (status === 'authenticated' && !settingsFetched) {
      fetch('/api/settings')
        .then(res => res.json())
        .then(json => {
          if (json.success && json.data) {
            setUnitState(json.data.unit);
          }
        })
        .catch(err => console.error('Error fetching settings:', err))
        .finally(() => setSettingsFetched(true));
    }
  }, [status, settingsFetched]);

  const setUnit = async (newUnit: 'kg' | 'lbs') => {
    setUnitState(newUnit);
    if (session) {
      try {
        await fetch('/api/settings', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ unit: newUnit }),
        });
      } catch (error) {
        console.error('Failed to save unit preference:', error);
      }
    }
  };

  const loading = status === 'loading' || (status === 'authenticated' && !settingsFetched);

  return (
    <SettingsContext.Provider value={{ unit, setUnit, loading }}>
      {children}
    </SettingsContext.Provider>
  );
}

export function useSettings() {
  const context = useContext(SettingsContext);
  if (context === undefined) {
    throw new Error('useSettings must be used within a SettingsProvider');
  }
  return context;
}
