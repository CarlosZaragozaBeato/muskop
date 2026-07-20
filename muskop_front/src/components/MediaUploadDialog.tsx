import { useRef, useState } from 'react'
import { useI18n } from '../i18n/I18nContext'
import type { MediaContent } from '../types/tab'
import {
  MAX_MEDIA_BYTES,
  MEDIA_ACCEPT,
  formatBytes,
  mediaKindFromMime,
  readAsDataUrl,
} from '../utils/media'

/**
 * Diálogo para subir un recurso multimedia (imagen, audio o vídeo) a la
 * librería. Valida el tipo y el tamaño (`MAX_MEDIA_BYTES`) y guarda el binario
 * como data URL base64 dentro del recurso.
 */
export default function MediaUploadDialog({
  onClose,
  onCreate,
}: {
  onClose: () => void
  onCreate: (title: string, category: string | null, content: MediaContent) => Promise<void>
}) {
  const { t } = useI18n()
  const inputRef = useRef<HTMLInputElement>(null)
  const [file, setFile] = useState<File | null>(null)
  const [title, setTitle] = useState('')
  const [category, setCategory] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const pick = (f: File | null) => {
    setError(null)
    if (!f) return
    if (!mediaKindFromMime(f.type)) {
      setFile(null)
      setError(t('media.unsupported'))
      return
    }
    if (f.size > MAX_MEDIA_BYTES) {
      setFile(null)
      setError(t('media.tooLarge', { size: formatBytes(f.size), max: formatBytes(MAX_MEDIA_BYTES) }))
      return
    }
    setFile(f)
    if (!title.trim()) setTitle(f.name.replace(/\.[^.]+$/, ''))
  }

  const submit = async () => {
    if (!file) return
    const mediaType = mediaKindFromMime(file.type)
    if (!mediaType) return
    setSaving(true)
    setError(null)
    try {
      const data = await readAsDataUrl(file)
      await onCreate(title.trim() || file.name, category.trim() || null, {
        kind: 'media',
        mediaType,
        mime: file.type,
        name: file.name,
        data,
        size: file.size,
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
      setSaving(false)
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>{t('media.title')}</h3>
          <button type="button" onClick={onClose}>✕</button>
        </div>

        <p className="muted">{t('media.hint', { max: formatBytes(MAX_MEDIA_BYTES) })}</p>

        <input
          ref={inputRef}
          type="file"
          accept={MEDIA_ACCEPT}
          hidden
          onChange={(e) => pick(e.target.files?.[0] ?? null)}
        />
        <div className="settings-row">
          <button type="button" onClick={() => inputRef.current?.click()}>
            {file ? t('media.change') : t('media.pickFile')}
          </button>
          {file && (
            <span className="muted">
              {t('media.selected', { name: file.name, size: formatBytes(file.size) })}
            </span>
          )}
        </div>

        <label>
          {t('media.titleLabel')}
          <input
            type="text"
            placeholder={t('media.titlePlaceholder')}
            value={title}
            onChange={(e) => setTitle(e.target.value)}
          />
        </label>
        <label>
          {t('media.categoryLabel')}
          <input
            type="text"
            placeholder={t('media.categoryPlaceholder')}
            value={category}
            onChange={(e) => setCategory(e.target.value)}
          />
        </label>

        {error && <p className="error">{error}</p>}

        <div className="modal-footer">
          <button
            type="button"
            className="primary"
            disabled={!file || saving}
            onClick={submit}
          >
            {saving ? t('common.saving') : t('media.add')}
          </button>
        </div>
      </div>
    </div>
  )
}
