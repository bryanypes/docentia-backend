import './PantallaAcceso.css'

/**
 * Pantalla única de acceso (login y cambio de contraseña): fondo claro de la paleta y una sola
 * tarjeta con el avatar de IA, el nombre de la plataforma y el formulario.
 *
 * @param avatar ilustración del robot; cambia según el estado del formulario
 * @param estadoAvatar texto breve para lectores de pantalla cuando el avatar comunica algo
 */
export function PantallaAcceso({ avatar, estadoAvatar, children }) {
  return (
    <div className="acceso">
      <main className="acceso__tarjeta">
        <div className="acceso__avatar">
          {/* key: al cambiar de ilustración se hace un fundido corto */}
          <img key={avatar} src={avatar} alt="" />
        </div>
        {estadoAvatar && (
          <span className="solo-lectores" role="status">
            {estadoAvatar}
          </span>
        )}

        <header className="acceso__marca">
          <p className="acceso__nombre">DOCENTIA</p>
          <img
            className="acceso__logo"
            src={`${import.meta.env.BASE_URL}brand/logo-uniautonoma.png`}
            alt="Universidad Autónoma del Cauca"
          />
          <p className="acceso__institucion">Evaluación docente institucional</p>
        </header>

        {children}
      </main>
    </div>
  )
}
