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

  it('suma el embudo, las facultades y los temas de comentarios cuando llegan', () => {
    const hojas = hojasDelReporte({
      filas: [FILA],
      r: calcular([FILA]),
      periodo: '2026-2',
      nombreGrupo: 'programa',
      embudo: [{ etiqueta: 'Evaluación completa', cantidad: 4 }, { etiqueta: 'Acta firmada', cantidad: 1 }],
      facultades: [{ nombre: 'Ingeniería', docentes: 4, completos: 4, promedio: 4.1, bajoUmbral: 1, reuniones: 2, actasFirmadas: 1 }],
      comentarios: { docentesConAnalisis: 3, nComentarios: 30, temas: [{ nombre: 'Claridad', criterio: 'C2', polaridad: 'POSITIVO', frecuencia: 10, porcentaje: 33.3, docentes: 3 }] },
    })
    expect(hojas.map((h) => h.nombre)).toEqual(expect.arrayContaining(['Embudo', 'Por facultad', 'Comentarios']))
    const temas = hojas.find((h) => h.nombre === 'Comentarios')
    expect(temas.filas[0]).toEqual(['Claridad', 'C2', 'Fortaleza', 10, 0.333, 3])
  })
})
