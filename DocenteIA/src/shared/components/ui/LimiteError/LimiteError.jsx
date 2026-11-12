import { Component } from 'react'
import { PantallaError } from './PantallaError.jsx'

/**
 * Último recurso: si una pantalla falla al dibujarse, la app no queda en blanco. En la raíz recarga
 * la página; dentro del maquetado (`enLinea`) solo vuelve a dibujar la pantalla y el menú sigue.
 */
export class LimiteError extends Component {
  state = { fallo: false, intento: 0 }

  static getDerivedStateFromError() {
    return { fallo: true }
  }

  reintentar = () => {
    if (this.props.enLinea) {
      this.setState((previo) => ({ fallo: false, intento: previo.intento + 1 }))
    } else {
      window.location.reload()
    }
  }

  render() {
    if (!this.state.fallo) return this.props.children
    return <PantallaError enLinea={this.props.enLinea} onReintentar={this.reintentar} />
  }
}
