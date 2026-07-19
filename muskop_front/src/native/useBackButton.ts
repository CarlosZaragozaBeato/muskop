import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { App } from '@capacitor/app'
import { isNativePlatform } from './share'

/**
 * En Android, el botón físico "atrás" navega hacia atrás en la app; si ya
 * estamos en una pantalla raíz (inicio o login), sale de la aplicación.
 * En web no hace nada (el navegador gestiona su propio botón atrás).
 */
export function useBackButton() {
  const navigate = useNavigate()

  useEffect(() => {
    if (!isNativePlatform()) return
    let remove: (() => void) | undefined
    App.addListener('backButton', () => {
      const atRoot = window.location.pathname === '/' || window.location.pathname === '/login'
      if (atRoot) {
        App.exitApp()
      } else {
        navigate(-1)
      }
    }).then((handle) => {
      remove = () => handle.remove()
    })
    return () => remove?.()
  }, [navigate])
}
