import { describe, expect, it } from 'vitest'
import { ROLES } from '../shared/constants/dominio.js'
import { rutaInicioDelRol } from './rutasPorRol.js'

describe('rutaInicioDelRol', () => {
  it('lleva al estudiante a evaluar profesores y no a la gestión', () => {
    expect(rutaInicioDelRol(ROLES.ESTUDIANTE)).toBe('/evaluacion-profesores')
    expect(rutaInicioDelRol(ROLES.DOCENTE)).toBe('/mi-evaluacion')
    expect(rutaInicioDelRol(ROLES.COORDINADOR)).toBe('/inicio')
    expect(rutaInicioDelRol(ROLES.VICERRECTOR)).toBe('/inicio')
  })
})
