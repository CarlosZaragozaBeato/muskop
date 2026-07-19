import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import * as sessions from '../storage/sessionManager'
import type { MuskopUser } from '../types/tab'

// ==========================================================================
// Contexto de sesión (local-first): el "login" crea o abre una sesión local;
// no hay ningún backend detrás. Mantiene el nombre useAuth/user para el
// resto de la app.
// ==========================================================================

interface AuthContextValue {
  user: MuskopUser | null
  /** false mientras se intenta reabrir la última sesión del dispositivo */
  ready: boolean
  createSession: (username: string, label?: string) => Promise<void>
  openSession: (deviceId: string) => Promise<void>
  importSession: (file: File) => Promise<void>
  downloadSession: () => void
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

function toUser(session: { user: { username: string } } | null): MuskopUser | null {
  return session ? { id: 1, username: session.user.username } : null
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<MuskopUser | null>(null)
  const [ready, setReady] = useState(false)

  useEffect(() => {
    let cancelled = false
    sessions
      .resumeLastSession()
      .then((session) => {
        if (!cancelled) setUser(toUser(session))
      })
      .finally(() => {
        if (!cancelled) setReady(true)
      })
    return () => {
      cancelled = true
    }
  }, [])

  const createSession = useCallback(async (username: string, label?: string) => {
    setUser(toUser(await sessions.startNewSession(username, label)))
  }, [])

  const openSession = useCallback(async (deviceId: string) => {
    setUser(toUser(await sessions.openStoredSession(deviceId)))
  }, [])

  const importSession = useCallback(async (file: File) => {
    setUser(toUser(await sessions.importSessionFile(file)))
  }, [])

  const downloadSession = useCallback(() => {
    sessions.downloadActiveSession()
  }, [])

  const logout = useCallback(() => {
    sessions.closeSession()
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider
      value={{ user, ready, createSession, openSession, importSession, downloadSession, logout }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>')
  return ctx
}
