import { describe, expect, it } from 'vitest'
import { ETAPAS_PERIODO, validarFechasPeriodo } from './etapas.js'

const PERIODO = { fechaInicio: '2026-08-03', fechaFin: '2026-12-05' }

describe('etapas del periodo', () => {
  it('usa los cuatro tipos que acepta el backend, en su orden', () => {
    expect(ETAPAS_PERIODO.map(({ etapa }) => etapa)).toEqual([
      'RECOLECCION_CALIFICACIONES',
      'REVISION_HALLAZGOS',
      'AGENDA_CITAS',
      'FIRMA_ACTAS',
    ])
  })

  it('acepta una etapa en los bordes del periodo', () => {
    const etapas = [{ tipoEtapa: 'FIRMA_ACTAS', fechaApertura: '2026-08-03', fechaCierre: '2026-12-05' }]
    expect(validarFechasPeriodo({ ...PERIODO, etapas })).toEqual([])
  })

  it('explica cada problema por etapa', () => {
    const etapas = [
      { tipoEtapa: 'REVISION_HALLAZGOS', fechaApertura: '2026-11-10', fechaCierre: '2026-11-01' },
      { tipoEtapa: 'FIRMA_ACTAS', fechaApertura: '2026-12-01', fechaCierre: '2026-12-20' },
      { tipoEtapa: 'AGENDA_CITAS', fechaApertura: '2026-11-01', fechaCierre: '' },
    ]
    expect(validarFechasPeriodo({ ...PERIODO, etapas })).toEqual([
      'Revisión de hallazgos: el cierre no puede ser antes de la apertura.',
      'Firma de actas: debe quedar dentro de las fechas del periodo.',
      'Agenda de citas: necesita fecha de apertura y de cierre.',
    ])
  })

  it('rechaza un periodo que termina antes de empezar', () => {
    expect(validarFechasPeriodo({ fechaInicio: '2026-08-03', fechaFin: '2026-08-01', etapas: [] })).toEqual([
      'La fecha de fin del periodo no puede ser anterior a la de inicio.',
    ])
  })
})
