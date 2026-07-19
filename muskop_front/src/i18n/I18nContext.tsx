import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { DEFAULT_LANG, type Dict, type Lang } from './types'
import { en } from './en'
import { es } from './es'

// ==========================================================================
// Contexto de idioma. El idioma se guarda a nivel de dispositivo en
// localStorage ('muskop.lang'), no en la sesión, para que funcione también en
// la pantalla de entrada (antes de abrir ninguna sesión). Por defecto: inglés.
// ==========================================================================

const STORAGE_KEY = 'muskop.lang'
const DICTS: Record<Lang, Dict> = { en, es }

function readStored(): Lang {
  const v = typeof localStorage !== 'undefined' ? localStorage.getItem(STORAGE_KEY) : null
  return v === 'es' || v === 'en' ? v : DEFAULT_LANG
}

/** Recorre el diccionario por una clave con puntos ("library.title"). */
function lookup(dict: Dict, key: string): string | string[] | undefined {
  let node: string | string[] | Dict | undefined = dict
  for (const part of key.split('.')) {
    if (node === undefined || typeof node === 'string' || Array.isArray(node)) return undefined
    node = node[part]
  }
  return typeof node === 'string' || Array.isArray(node) ? node : undefined
}

/** Sustituye {vars} en un texto por sus valores. */
function interpolate(text: string, vars?: Record<string, string | number>): string {
  if (!vars) return text
  return text.replace(/\{(\w+)\}/g, (_, name) =>
    name in vars ? String(vars[name]) : `{${name}}`,
  )
}

export type TranslateFn = (key: string, vars?: Record<string, string | number>) => string
export type TranslateListFn = (key: string) => string[]

interface I18nContextValue {
  lang: Lang
  setLang: (lang: Lang) => void
  t: TranslateFn
  /** Para claves que son listas (p. ej. recomendaciones del catálogo). */
  tList: TranslateListFn
}

const I18nContext = createContext<I18nContextValue | null>(null)

export function I18nProvider({ children }: { children: ReactNode }) {
  const [lang, setLangState] = useState<Lang>(readStored)

  useEffect(() => {
    if (typeof document !== 'undefined') document.documentElement.lang = lang
  }, [lang])

  const setLang = useCallback((next: Lang) => {
    localStorage.setItem(STORAGE_KEY, next)
    setLangState(next)
  }, [])

  const value = useMemo<I18nContextValue>(() => {
    const dict = DICTS[lang]
    const fallback = DICTS[DEFAULT_LANG]
    const t: TranslateFn = (key, vars) => {
      const found = lookup(dict, key) ?? lookup(fallback, key)
      if (typeof found === 'string') return interpolate(found, vars)
      return key // clave no encontrada: se muestra la clave (señal de desarrollo)
    }
    const tList: TranslateListFn = (key) => {
      const found = lookup(dict, key) ?? lookup(fallback, key)
      return Array.isArray(found) ? found : []
    }
    return { lang, setLang, t, tList }
  }, [lang, setLang])

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}

export function useI18n(): I18nContextValue {
  const ctx = useContext(I18nContext)
  if (!ctx) throw new Error('useI18n debe usarse dentro de <I18nProvider>')
  return ctx
}
