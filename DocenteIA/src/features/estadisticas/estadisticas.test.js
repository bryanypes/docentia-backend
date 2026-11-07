import { describe, expect, it } from 'vitest'
import { calcular, filtrar, unirDatos } from './estadisticas.js'

const docente = (id, programa, idFacultad = 1) => ({
  idDocente: id,
  nombreCompleto: `Docente ${id}`,
  correo: `d${id}@u.co`,
  nombrePrograma: programa,
  idFacultad,
  nombreFacultad: `Facultad ${idFacultad}`,
})
const completo = (valor) => ({ estado: 'COMPLETO', valor, evaluadoresPendientes: [] })

const SEGUIMIENTO = [
  { docente: docente(1, 'Software'), promedio: completo(4.7) },
  { docente: docente(2, 'Software'), promedio: completo(2.9) },
  { docente: docente(3, 'Electrónica', 2), promedio: completo(3.9) },
  { docente: docente(4, 'Electrónica', 2), promedio: { estado: 'PARCIAL', valor: null, evaluadoresPendientes: ['ESTUDIANTES'] } },
]
const PROCESOS = [
  { docente: { idDocente: 1 }, estado: 'FINALIZADO', coberturaEstudiantes: { respondidas: 8, esperadas: 10 }, acta: { tipo: 'RETROALIMENTACION', estado: 'FIRMADA' }, cita: { activa: true, asistencia: 'ASISTIO' } },
  { docente: { idDocente: 2 }, estado: 'CITA_AGENDADA', coberturaEstudiantes: { respondidas: 2, esperadas: 10 }, acta: null, cita: { activa: true, confirmacion: null } },
  { docente: { idDocente: 3 }, estado: 'EVALUACION_COMPLETA', coberturaEstudiantes: { respondidas: 0, esperadas: 0 }, acta: null, cita: null },
  { docente: { idDocente: 4 }, estado: 'EVALUACIONES_EN_CURSO', coberturaEstudiantes: { respondidas: 0, esperadas: 5 }, acta: null, cita: null },
]

describe('estadísticas', () => {
  const filas = unirDatos(SEGUIMIENTO, PROCESOS)

  it('calcula avance, promedio, niveles, cobertura y proceso del alcance', () => {
    const r = calcular(filas, { umbral: 3.8 })
    expect(r.total).toBe(4)
    expect(r.completos).toBe(3)
    expect(r.avance).toBe(75)
    expect(r.promedioGeneral).toBe(3.83)
    expect(r.mediana).toBe(3.9)
    expect(r.bajoUmbral).toBe(1)
    expect(r.cobertura).toEqual({ respondidas: 10, esperadas: 25, porcentaje: 40 })
    expect(r.porNivel.find((n) => n.codigo === 'MUY_ALTO').cantidad).toBe(1)
    expect(r.porNivel.find((n) => n.codigo === 'BAJO').cantidad).toBe(1)
    expect(r.porEstado.find((e) => e.estado === 'FINALIZADO').cantidad).toBe(1)
    expect(r.pendientesPorEvaluador.find((p) => p.evaluador === 'ESTUDIANTES').cantidad).toBe(1)
    expect(r.actas).toMatchObject({ firmadas: 1, retroalimentacion: 1, sinActa: 2 })
    expect(r.citas).toMatchObject({ agendadas: 2, asistieron: 1 })
    expect(r.histograma.reduce((a, b) => a + b.cantidad, 0)).toBe(3)
  })

  it('agrupa por programa con promedio, avance y cobertura', () => {
    const { porGrupo } = calcular(filas)
    const software = porGrupo.find((g) => g.nombre === 'Software')
    expect(software).toMatchObject({ docentes: 2, promedio: 3.8, avance: 100, cobertura: 50 })
    expect(porGrupo.find((g) => g.nombre === 'Electrónica')).toMatchObject({ avance: 50, promedio: 3.9 })
  })

  it('cada filtro reduce las mismas filas para todas las gráficas', () => {
    expect(filtrar(filas, { programa: 'Software' })).toHaveLength(2)
    expect(filtrar(filas, { facultad: '2' })).toHaveLength(2)
    expect(filtrar(filas, { nivel: 'BAJO' }).map((f) => f.docente.idDocente)).toEqual([2])
    expect(filtrar(filas, { estado: 'FINALIZADO' }).map((f) => f.docente.idDocente)).toEqual([1])
    expect(filtrar(filas, { busqueda: 'docente 3' })).toHaveLength(1)
    expect(calcular(filtrar(filas, { programa: 'Software' })).promedioGeneral).toBe(3.8)
  })
})
