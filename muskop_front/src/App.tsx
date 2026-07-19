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
import TabEditorPage from './pages/TabEditorPage'
import { NavLink } from 'react-router-dom'
import { useI18n } from './i18n/I18nContext'
import LanguageSwitcher from './i18n/LanguageSwitcher'
import { useTheme } from './theme/ThemeContext'
import ThemeToggle from './theme/ThemeToggle'
import { useBackButton } from './native/useBackButton'
import logoLight from './assets/brand/logotipo-light.svg'
import logoDark from './assets/brand/logotipo-dark.svg'

function ProtectedLayout() {
  const { user, ready, logout, downloadSession } = useAuth()
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
          <nav>
            <NavLink to="/">{t('nav.home')}</NavLink>
            <NavLink to="/routines">{t('nav.routines')}</NavLink>
            <NavLink to="/progress">{t('nav.progress')}</NavLink>
            <NavLink to="/explore">{t('nav.explore')}</NavLink>
            <NavLink to="/library">{t('nav.library')}</NavLink>
            <NavLink to="/tabs/new">{t('nav.newTab')}</NavLink>
          </nav>
        </div>
        <div className="header-right">
          <ThemeToggle />
          <LanguageSwitcher />
          <span className="muted">{user.username}</span>
          <button type="button" title={t('header.sessionTitle')} onClick={downloadSession}>
            ⬇ {t('header.session')}
          </button>
          <button type="button" onClick={logout}>{t('header.logout')}</button>
        </div>
      </header>
      <main>
        <Outlet />
      </main>
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
        <Route path="/tabs/new" element={<TabEditorPage />} />
        <Route path="/tabs/:id" element={<TabEditorPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
