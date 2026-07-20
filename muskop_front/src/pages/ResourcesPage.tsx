import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import * as api from '../api/client'
import { useAuth } from '../auth/AuthContext'
import ResourceViewDialog from '../components/ResourceViewDialog'
import { useI18n } from '../i18n/I18nContext'
import { saveText } from '../native/share'
import type { ResourceSummary } from '../types/tab'
import { exportResourceBundle, parseResourceBundle } from '../utils/resourceIO'

const TYPE_ORDER = ['TAB', 'CHORD', 'SNIPPET', 'THEORY', 'MEDIA', 'COLLECTION']

function slug(text: string): string {
  return (
    text
      .toLowerCase()
      .normalize('NFD')
      .replace(/[̀-ͯ]/g, '')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/(^-|-$)/g, '') || 'resource'
  )
}

/** Editores disponibles por tipo de recurso (los demás no se pueden editar). */
const EDIT_ROUTE: Record<string, (id: string) => string> = {
  TAB: (id) => `/tabs/${id}`,
  THEORY: (id) => `/theory/${id}`,
}

/**
 * Página de gestión de recursos (Settings → Recursos): vista central de todos
 * los recursos con acciones de ver, editar, exportar, eliminar (individual) e
 * importar/exportar en bloque.
 */
export default function ResourcesPage() {
  const { user } = useAuth()
  const { t } = useI18n()
  const navigate = useNavigate()
  const fileRef = useRef<HTMLInputElement>(null)
  const [items, setItems] = useState<ResourceSummary[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [viewTarget, setViewTarget] = useState<ResourceSummary | null>(null)

  const reload = useCallback(() => {
    if (!user) return
    api
      .listResources(user.id)
      .then(setItems)
      .catch((err) => setError(err instanceof Error ? err.message : t('library.errorLoad')))
  }, [user, t])

  useEffect(reload, [reload])

  const sorted = useMemo(
    () =>
      [...(items ?? [])].sort(
        (a, b) =>
          TYPE_ORDER.indexOf(a.type.toUpperCase()) - TYPE_ORDER.indexOf(b.type.toUpperCase()) ||
          a.title.localeCompare(b.title),
      ),
    [items],
  )

  const run = (action: () => Promise<void>) => async () => {
    try {
      setError(null)
      await action()
    } catch (err) {
      setError(err instanceof Error ? err.message : t('resources.exportError'))
    }
  }

  const exportOne = (item: ResourceSummary) =>
    run(async () => {
      const detail = await api.getResource(item.id)
      await saveText(`${slug(item.title)}.muskopres.json`, exportResourceBundle([detail]))
    })

  const exportAll = run(async () => {
    if (!items || items.length === 0) return
    const details = await Promise.all(items.map((i) => api.getResource(i.id)))
    await saveText('resources.muskopres.json', exportResourceBundle(details))
  })

  const remove = (item: ResourceSummary) =>
    run(async () => {
      if (!window.confirm(t('library.deleteConfirm', { name: item.title }))) return
      await api.deleteResource(item.id)
      reload()
    })

  const importFile = async (file: File) => {
    try {
      setError(null)
      setNotice(null)
      const requests = parseResourceBundle(await file.text())
      for (const req of requests) {
        if (user) await api.createResource(user.id, req)
      }
      setNotice(t('resources.imported', { n: requests.length }))
      reload()
    } catch (err) {
      setError(err instanceof Error ? err.message : t('resources.importError'))
    }
  }

  return (
    <div className="resources-page">
      <div className="page-header">
        <h2>{t('resources.title')}</h2>
        <div className="header-actions">
          <input
            ref={fileRef}
            type="file"
            accept="application/json,.json"
            hidden
            onChange={(e) => {
              const f = e.target.files?.[0]
              if (f) void importFile(f)
              e.target.value = ''
            }}
          />
          <button type="button" onClick={() => fileRef.current?.click()}>
            {t('resources.importAll')}
          </button>
          <button type="button" onClick={exportAll} disabled={!items || items.length === 0}>
            {t('resources.exportAll')}
          </button>
          <button type="button" onClick={() => navigate('/settings')}>{t('resources.back')}</button>
        </div>
      </div>

      <p className="muted">{t('resources.intro')}</p>
      {error && <p className="error">{error}</p>}
      {notice && <p className="success">{notice}</p>}

      {items === null && !error && <p className="muted">{t('common.loading')}</p>}
      {items !== null && items.length === 0 && <p className="muted">{t('resources.empty')}</p>}

      {items !== null && items.length > 0 && (
        <>
          <p className="muted">{t('resources.count', { n: items.length })}</p>
          <ul className="library-list">
            {sorted.map((item) => {
              const type = item.type.toUpperCase()
              const editRoute = EDIT_ROUTE[type]
              return (
                <li key={item.id} className="library-row">
                  <span className={`badge badge-${type.toLowerCase()}`}>
                    {t(`library.types.${type}`)}
                  </span>
                  <span className="library-item-title">{item.title}</span>
                  {item.category && <span className="chip">{item.category}</span>}
                  <span className="row-actions">
                    {type !== 'COLLECTION' && (
                      <button type="button" onClick={() => setViewTarget(item)}>
                        👁 {t('common.view')}
                      </button>
                    )}
                    {editRoute && (
                      <button type="button" onClick={() => navigate(editRoute(item.id))}>
                        {t('common.edit')}
                      </button>
                    )}
                    <button type="button" onClick={exportOne(item)}>
                      {t('resources.exportOne')}
                    </button>
                    <button type="button" onClick={remove(item)}>{t('common.delete')}</button>
                  </span>
                </li>
              )
            })}
          </ul>
        </>
      )}

      {viewTarget && (
        <ResourceViewDialog
          id={viewTarget.id}
          title={viewTarget.title}
          onClose={() => setViewTarget(null)}
        />
      )}
    </div>
  )
}
