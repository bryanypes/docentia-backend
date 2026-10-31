import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { SesionContext } from '../features/auth/sesionContext.js'

/** Renderiza una pantalla con sesión iniciada y router, como dentro de la aplicación. */
export function renderizarConSesion(elemento, { rol = 'COORDINADOR', ruta = '/' } = {}) {
  const usuario = { idUsuario: 2, nombreCompleto: 'Usuario de prueba', rol, idFacultad: 1 }
  return render(
    <MemoryRouter initialEntries={[ruta]}>
      <SesionContext.Provider value={{ usuario, validando: false }}>{elemento}</SesionContext.Provider>
    </MemoryRouter>,
  )
}
