import { Component } from 'react'

/** Último recurso: si una pantalla falla al dibujarse, la app no queda en blanco. */
export class LimiteError extends Component {
  state = { fallo: false }

  static getDerivedStateFromError() {
    return { fallo: true }
  }

  render() {
    if (!this.state.fallo) return this.props.children
    return (
      <main className="pagina" role="alert">
        <h1>Algo salió mal</h1>
        <p>La pantalla no se pudo mostrar. Recarga la página para continuar.</p>
        <button type="button" className="boton boton--primario" onClick={() => window.location.reload()}>
          Recargar
        </button>
      </main>
    )
  }
}
