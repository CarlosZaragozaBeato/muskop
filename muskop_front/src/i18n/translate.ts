import { DEFAULT_LANG, type Dict, type Lang } from './types'
import { en } from './en'
import { es } from './es'

// ==========================================================================
// Traducción autónoma (sin React). La usan el proveedor useI18n() y también
// la capa de datos (validaciones/errores) que corre fuera del árbol de React.
// El idioma se lee de localStorage ('muskop.lang'); por defecto, inglés.
// ==========================================================================

export const STORAGE_KEY = 'muskop.lang'
const DICTS: Record<Lang, Dict> = { en, es }

export function getStoredLang(): Lang {
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
  return text.replace(/\{(\w+)\}/g, (_, name) => (name in vars ? String(vars[name]) : `{${name}}`))
}

export function translate(
  key: string,
  vars?: Record<string, string | number>,
  lang: Lang = getStoredLang(),
): string {
  const found = lookup(DICTS[lang], key) ?? lookup(DICTS[DEFAULT_LANG], key)
  if (typeof found === 'string') return interpolate(found, vars)
  return key // clave no encontrada: se muestra la clave (señal de desarrollo)
}

export function translateList(key: string, lang: Lang = getStoredLang()): string[] {
  const found = lookup(DICTS[lang], key) ?? lookup(DICTS[DEFAULT_LANG], key)
  return Array.isArray(found) ? found : []
}
