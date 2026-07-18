import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function HomePage() {
  const { user } = useAuth()

  return (
    <div className="home-page">
      <section>
        <h2>Hola, {user?.username} 👋</h2>
        <p className="muted">
          Organiza tu práctica en rutinas con bloques (calentamiento, técnica,
          repertorio…) y crea tus propias tablaturas, acordes y arpegios.
        </p>
      </section>

      <section className="card-grid">
        <div className="card">
          <h3>Mis rutinas</h3>
          <p className="muted">
            Rutinas con bloques de práctica: duración, BPM objetivo, un recurso
            de tu librería e indicaciones. Practícalas con temporizador y
            metrónomo, bloque a bloque.
          </p>
          <Link className="button primary" to="/routines">▶ Mis rutinas</Link>
        </div>

        <div className="card">
          <h3>Editor de tablaturas</h3>
          <p className="muted">
            Secciones de tablatura, acordes y arpegios, con BPM por sección,
            reproducción con metrónomo, importación JSON y exportación a texto,
            imagen o PDF.
          </p>
          <Link className="button primary" to="/tabs/new">✚ Nueva tablatura</Link>
        </div>

        <div className="card">
          <h3>Librería</h3>
          <p className="muted">
            Tus tablaturas, acordes y fragmentos reutilizables, organizados por
            categorías, y colecciones exportables a PDF.
          </p>
          <Link className="button" to="/library">Abrir librería</Link>
        </div>
      </section>
    </div>
  )
}
