import { useEffect, useState } from 'react'
import * as api from '../api/client'
import { useI18n } from '../i18n/I18nContext'
import type { ResourceDetail } from '../types/tab'
import ResourceView from './ResourceView'

/**
 * Diálogo modal de solo lectura: carga un recurso por id y lo muestra con
 * `ResourceView`, sin entrar al editor. Usado desde la Librería (botón «Ver»).
 */
export default function ResourceViewDialog({
  id,
  title,
  onClose,
}: {
  id: string
  title: string
  onClose: () => void
}) {
  const { t } = useI18n()
  const [detail, setDetail] = useState<ResourceDetail | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    api
      .getResource(id)
      .then((d) => active && setDetail(d))
      .catch((err) => active && setError(err instanceof Error ? err.message : t('library.errorLoad')))
    return () => {
      active = false
    }
  }, [id, t])

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal modal-view" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>{title}</h3>
          <button type="button" onClick={onClose}>✕</button>
        </div>
        {error && <p className="error">{error}</p>}
        {!detail && !error && <p className="muted">{t('common.loading')}</p>}
        {detail && (
          <div className="resource-view-dialog-body">
            <ResourceView detail={detail} />
          </div>
        )}
      </div>
    </div>
  )
}
