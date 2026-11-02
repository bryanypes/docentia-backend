import { describe, expect, it } from 'vitest'
import { validarUsuario } from './validators.js'

const base = {
  nombreCompleto: 'Karen Mancilla',
  correo: 'karen@uniautonoma.edu.co',
  contrasena: 'Docentia2026*',
  rol: '',
  idFacultad: '',
  idPrograma: '',
}

describe('validarUsuario', () => {
  it('exige programa cuando el rol es estudiante', () => {
    expect(validarUsuario({ ...base, rol: 'ESTUDIANTE' }, { contrasenaObligatoria: true }).idPrograma).toBe(
      'Elige el programa (carrera) del estudiante',
    )
    expect(
      validarUsuario({ ...base, rol: 'ESTUDIANTE', idPrograma: '3' }, { contrasenaObligatoria: true }),
    ).toEqual({})
  })
})
