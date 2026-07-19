import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import type { Lang } from './types'
import { STORAGE_KEY, getStoredLang, translate, translateList } from './translate'

// ==========================================================================
// Contexto de idioma. El idioma se guarda a nivel de dispositivo en
// localStorage ('muskop.lang'), no en la sesión, para que funcione también en
// la pantalla de entrada (antes de abrir ninguna sesión). Por defecto: inglés.
// La lógica de traducción vive en ./translate (reutilizable fuera de React).
// ==========================================================================

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
  const [lang, setLangState] = useState<Lang>(getStoredLang)

  useEffect(() => {
    if (typeof document !== 'undefined') document.documentElement.lang = lang
  }, [lang])

  const setLang = useCallback((next: Lang) => {
    localStorage.setItem(STORAGE_KEY, next)
    setLangState(next)
  }, [])

  const value = useMemo<I18nContextValue>(
    () => ({
      lang,
      setLang,
      t: (key, vars) => translate(key, vars, lang),
      tList: (key) => translateList(key, lang),
    }),
    [lang, setLang],
  )

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>
}

export function useI18n(): I18nContextValue {
  const ctx = useContext(I18nContext)
  if (!ctx) throw new Error('useI18n debe usarse dentro de <I18nProvider>')
  return ctx
}
