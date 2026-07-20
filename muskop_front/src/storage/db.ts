import type { MuskopSession } from './session'

// ==========================================================================
// Persistencia local del dispositivo: IndexedDB con un único store de
// sesiones. Cada registro es una sesión completa más metadatos de listado.
// ==========================================================================

const DB_NAME = 'muskop'
const DB_VERSION = 1
const STORE = 'sessions'

export interface StoredSession {
  /** Id local del dispositivo (no viaja en el archivo exportado) */
  id: string
  username: string
  /** Etiqueta opcional para distinguir varias sesiones del mismo usuario */
  label?: string | null
  updatedAt: string
  data: MuskopSession
}

function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION)
    req.onupgradeneeded = () => {
      if (!req.result.objectStoreNames.contains(STORE)) {
        req.result.createObjectStore(STORE, { keyPath: 'id' })
      }
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => reject(req.error ?? new Error('No se pudo abrir IndexedDB'))
  })
}

function tx<T>(mode: IDBTransactionMode, run: (store: IDBObjectStore) => IDBRequest<T>): Promise<T> {
  return openDb().then(
    (db) =>
      new Promise<T>((resolve, reject) => {
        const transaction = db.transaction(STORE, mode)
        const req = run(transaction.objectStore(STORE))
        req.onsuccess = () => resolve(req.result)
        req.onerror = () => reject(req.error ?? new Error('Error de IndexedDB'))
        transaction.oncomplete = () => db.close()
      }),
  )
}

export function listStoredSessions(): Promise<StoredSession[]> {
  return tx('readonly', (store) => store.getAll() as IDBRequest<StoredSession[]>).then(
    (sessions) =>
      sessions.sort((a, b) => b.updatedAt.localeCompare(a.updatedAt)),
  )
}

export function getStoredSession(id: string): Promise<StoredSession | undefined> {
  return tx('readonly', (store) => store.get(id) as IDBRequest<StoredSession | undefined>)
}

export function putStoredSession(stored: StoredSession): Promise<unknown> {
  return tx('readwrite', (store) => store.put(stored))
}

export function deleteStoredSession(id: string): Promise<unknown> {
  return tx('readwrite', (store) => store.delete(id))
}

export function newDeviceId(): string {
  return `s-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`
}
