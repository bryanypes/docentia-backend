import { describe, expect, it } from 'vitest'
import { calcular } from './estadisticas.js'
import { hojasDelReporte } from './reporteExcel.js'

const FILA = {
  docente: { idDocente: 1, nombreCompleto: 'Ana Ruiz', correo: 'a@u.co', nombreFacultad: 'Ingeniería', nombrePrograma: 'Software' },
  promedio: { estado: 'COMPLETO', valor: 4.2, fuentesPendientes: [] },
  nivel: 'ALTO',
  proceso: { estado: 'FINALIZADO', coberturaEstudiantes: { respondidas: 8, esperadas: 10 } },
}

const titulos = (hoja) => hoja.columnas.map((c) => c.titulo)

describe('hojasDelReporte', () => {
  it('con la cobertura oculta no lleva columnas ni cifra de respuesta de estudiantes', () => {
    const hojas = hojasDelReporte({ filas: [FILA], r: calcular([FILA]), periodo: '2026-2', nombreGrupo: 'programa', conCobertura: false })
    const [resumen, docentes, , , grupo] = hojas
    expect(titulos(docentes)).not.toContain('Matrículas')
    expect(titulos(docentes)).not.toContain('Respuesta de estudiantes')
    expect(docentes.filas[0]).toHaveLength(docentes.columnas.length)
    expect(titulos(grupo)).not.toContain('Respondieron')
    expect(resumen.filas.map((f) => f[0])).not.toContain('Respuesta de estudiantes')
  })

  it('con la cobertura visible conserva sus columnas', () => {
    const [, docentes] = hojasDelReporte({ filas: [FILA], r: calcular([FILA]), periodo: '2026-2', nombreGrupo: 'programa' })
    expect(titulos(docentes)).toContain('Matrículas')
  })
})
