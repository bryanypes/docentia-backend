import { describe, expect, it } from 'vitest'
import { ROLES } from '../shared/constants/dominio.js'
import { panelInicioDelRol, rutaInicioDelRol } from './rutasPorRol.js'

describe('inicio por rol', () => {
  it('todos los roles entran por /inicio', () => {
    for (const rol of Object.values(ROLES)) expect(rutaInicioDelRol(rol)).toBe('/inicio')
  })

  it('cada rol ve su propio panel en /inicio', () => {
    expect(panelInicioDelRol(ROLES.ESTUDIANTE)).toBe('EVALUACION_PROFESORES')
    expect(panelInicioDelRol(ROLES.DOCENTE)).toBe('MI_EVALUACION')
    expect(panelInicioDelRol(ROLES.COORDINADOR)).toBe('GESTION')
    expect(panelInicioDelRol(ROLES.VICERRECTOR)).toBe('GESTION')
  })
})
