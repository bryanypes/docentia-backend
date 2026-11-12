import { describe, expect, it } from 'vitest'
import { ROLES } from '../shared/constants/dominio.js'
import { panelInicioDelRol, rutaInicioDelRol } from './rutasPorRol.js'

describe('inicio por rol', () => {
  it('los roles con panel entran por /inicio; administración y visualización a su pantalla', () => {
    for (const rol of [ROLES.ESTUDIANTE, ROLES.DOCENTE, ROLES.COORDINADOR, ROLES.VICERRECTOR]) {
      expect(rutaInicioDelRol(rol)).toBe('/inicio')
    }
    expect(rutaInicioDelRol(ROLES.ADMINISTRADOR)).toBe('/usuarios')
    expect(rutaInicioDelRol(ROLES.VISUALIZADOR)).toBe('/panel-programas')
    expect(panelInicioDelRol(ROLES.ADMINISTRADOR)).toBe('REDIRIGIR')
  })

  it('cada rol ve su propio panel en /inicio', () => {
    expect(panelInicioDelRol(ROLES.ESTUDIANTE)).toBe('EVALUACION_PROFESORES')
    expect(panelInicioDelRol(ROLES.DOCENTE)).toBe('MI_EVALUACION')
    expect(panelInicioDelRol(ROLES.COORDINADOR)).toBe('GESTION')
    expect(panelInicioDelRol(ROLES.VICERRECTOR)).toBe('GESTION')
  })
})
