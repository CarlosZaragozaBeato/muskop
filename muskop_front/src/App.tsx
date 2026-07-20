import { Navigate, Outlet, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import HomePage from './pages/HomePage'
import LibraryPage from './pages/LibraryPage'
import LoginPage from './pages/LoginPage'
import PracticePage from './pages/PracticePage'
import ProgressPage from './pages/ProgressPage'
import ExplorePage from './pages/ExplorePage'
import TheoryPage from './pages/TheoryPage'
import RoutineEditorPage from './pages/RoutineEditorPage'
import RoutinesPage from './pages/RoutinesPage'
import SettingsPage from './pages/SettingsPage'
import TabEditorPage from './pages/TabEditorPage'
import { NavLink } from 'react-router-dom'
import { useI18n } from './i18n/I18nContext'
import { useTheme } from './theme/ThemeContext'
import { useBackButton } from './native/useBackButton'
import logoLight from './assets/brand/logotipo-light.svg'
import logoDark from './assets/brand/logotipo-dark.svg'

/** Secciones principales de navegación (barra superior y barra inferior). */
const NAV_ITEMS = [
  { to: '/', key: 'nav.home', icon: '🏠', end: true },
  { to: '/routines', key: 'nav.routines', icon: '📋', end: false },
  { to: '/progress', key: 'nav.progress', icon: '📈', end: false },
  { to: '/explore', key: 'nav.explore', icon: '🧭', end: false },
  { to: '/library', key: 'nav.library', icon: '📚', end: false },
] as const

function ProtectedLayout() {
  const { user, ready } = useAuth()
  const { t } = useI18n()
  const { theme } = useTheme()

  if (!ready) {
    return null
  }
  if (!user) {
    return <Navigate to="/login" replace />
  }

  return (
    <div className="app-layout">
      <header className="app-header">
        <div className="header-left">
          <img
            className="brand-logo"
            src={theme === 'dark' ? logoDark : logoLight}
            alt="Muskop"
          />
          <nav className="top-nav">
            {NAV_ITEMS.map((item) => (
              <NavLink key={item.to} to={item.to} end={item.end}>
                <span className="nav-icon" aria-hidden="true">{item.icon}</span>
                <span className="nav-label">{t(item.key)}</span>
              </NavLink>
            ))}
          </nav>
        </div>
        <div className="header-right">
          <span className="muted header-user">{user.username}</span>
          <NavLink
            className="header-settings"
            to="/settings"
            title={t('nav.settings')}
            aria-label={t('nav.settings')}
          >
            ⚙️
          </NavLink>
        </div>
      </header>
      <main>
        <Outlet />
      </main>
      <nav className="bottom-nav">
        {NAV_ITEMS.map((item) => (
          <NavLink key={item.to} to={item.to} end={item.end} className="bottom-nav-item">
            <span className="nav-icon" aria-hidden="true">{item.icon}</span>
            <span className="nav-label">{t(item.key)}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  )
}

export default function App() {
  useBackButton()
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/routines" element={<RoutinesPage />} />
        <Route path="/routines/new" element={<RoutineEditorPage />} />
        <Route path="/routines/:id" element={<RoutineEditorPage />} />
        <Route path="/routines/:id/practice" element={<PracticePage />} />
        <Route path="/progress" element={<ProgressPage />} />
        <Route path="/explore" element={<ExplorePage />} />
        <Route path="/theory/new" element={<TheoryPage />} />
        <Route path="/theory/:id" element={<TheoryPage />} />
        <Route path="/library" element={<LibraryPage />} />
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="/tabs/new" element={<TabEditorPage />} />
        <Route path="/tabs/:id" element={<TabEditorPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
