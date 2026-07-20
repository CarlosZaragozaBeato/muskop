import { useI18n } from './I18nContext'
import { LANGS, type Lang } from './types'

/** Selector compacto de idioma (EN/ES). Se usa en la cabecera y en el login. */
export default function LanguageSwitcher() {
  const { lang, setLang, t } = useI18n()
  return (
    <select
      className="lang-switcher"
      value={lang}
      aria-label={t('lang.label')}
      title={t('lang.label')}
      onChange={(e) => setLang(e.target.value as Lang)}
    >
      {LANGS.map((l) => (
        <option key={l.id} value={l.id}>
          {l.flag} {l.label}
        </option>
      ))}
    </select>
  )
}
