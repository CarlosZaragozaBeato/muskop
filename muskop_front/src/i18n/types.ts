// ==========================================================================
// Internacionalización (solución propia, sin dependencias). Dos idiomas:
// inglés (por defecto) y español. Ver src/i18n/I18nContext.tsx.
// ==========================================================================

export type Lang = 'en' | 'es'

export const LANGS: { id: Lang; label: string; flag: string }[] = [
  { id: 'en', label: 'English', flag: '🇬🇧' },
  { id: 'es', label: 'Español', flag: '🇪🇸' },
]

export const DEFAULT_LANG: Lang = 'en'

/** Diccionario anidado: hojas string o array de string (listas). */
export interface Dict {
  [key: string]: string | string[] | Dict
}
