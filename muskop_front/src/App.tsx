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

function ProtectedLayout() {
  const { user, ready, logout, downloadSession } = useAuth()

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
          <span className="brand">🎸 Muskop</span>
          <nav>
            <NavLink to="/">Inicio</NavLink>
            <NavLink to="/routines">Rutinas</NavLink>
            <NavLink to="/progress">Progreso</NavLink>
            <NavLink to="/explore">Explorar</NavLink>
            <NavLink to="/library">Librería</NavLink>
            <NavLink to="/tabs/new">Nueva tablatura</NavLink>
          </nav>
        </div>
        <div className="header-right">
          <span className="muted">{user.username}</span>
          <button
            type="button"
            title="Descarga tu sesión (.muskop.json): tu librería y rutinas en un archivo"
            onClick={downloadSession}
          >
            ⬇ Sesión
          </button>
          <button type="button" onClick={logout}>Salir</button>
        </div>
      </header>
      <main>
        <Outlet />
      </main>
    </div>
  )
}

export default function App() {
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
