import { screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import { ApiError } from '../../../services/api.js'
import { obtenerHeteroevaluacion } from '../services/formulariosService.js'
import HeteroevaluacionClase from './HeteroevaluacionClase.jsx'

const params = vi.hoisted(() => ({ idClase: '11', idDocente: '4' }))

vi.mock('../services/formulariosService.js')
vi.mock('react-router-dom', () => ({
  Link: ({ to, children }) => <a href={to}>{children}</a>,
  MemoryRouter: ({ children }) => children,
  useParams: () => ({ idClase: params.idClase, idDocente: params.idDocente }),
}))

const FORMULARIO = {
  periodo: { nombre: '2026-2' },
  tipo: 'HETERO',
  idDocente: 4,
  nombreDocente: 'Zulema León',
  estado: null,
  enviadaEn: null,
  ventanaAbierta: true,
  respondidos: 0,
  total: 11,
  idClase: 11,
  nombreCurso: 'DESARROLLO DE APLICACIONES MÓVILES',
  grupo: 'A',
  jornada: 'S001',
  nombrePrograma: 'ADMINISTRACION DE EMPRESAS PROFESIONAL',
  escala: [1, 2, 3, 4, 5].map((valor) => ({ valor, etiqueta: `Nivel ${valor}` })),
  criterios: [
    {
      codigo: 'C2',
      nombre: 'Capacidad para establecer un clima propicio',
      comentario: null,
      descriptores: [{ codigo: 'D02', texto: 'Genera un ambiente de respeto', valor: null }],
    },
  ],
}

beforeEach(() => {
  vi.clearAllMocks()
  params.idClase = '11'
  params.idDocente = '4'
})

describe('HeteroevaluacionClase', () => {
  it('carga el instrumento HETERO de la clase', async () => {
    obtenerHeteroevaluacion.mockResolvedValue(FORMULARIO)
    renderizarConSesion(<HeteroevaluacionClase />, { rol: 'ESTUDIANTE' })
    expect(await screen.findByRole('heading', { name: 'Evaluación de Zulema León' })).toBeInTheDocument()
    expect(screen.getByText(/DESARROLLO DE APLICACIONES MÓVILES/)).toBeInTheDocument()
    expect(screen.getByText(/Grupo A/)).toBeInTheDocument()
    expect(screen.getByText('0 de 11 ítems respondidos')).toBeInTheDocument()
    expect(screen.getByRole('group', { name: 'Genera un ambiente de respeto' })).toBeInTheDocument()
    expect(obtenerHeteroevaluacion).toHaveBeenCalledWith('11', '4')
  })

  it('avisa si la recolección está cerrada y no ofrece enviar', async () => {
    obtenerHeteroevaluacion.mockResolvedValue({ ...FORMULARIO, ventanaAbierta: false })
    renderizarConSesion(<HeteroevaluacionClase />, { rol: 'ESTUDIANTE' })
    expect(await screen.findByText('La recolección está cerrada')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Enviar formulario' })).not.toBeInTheDocument()
  })

  it('muestra 404 si la clase no es del estudiante', async () => {
    params.idClase = '99'
    params.idDocente = '1'
    obtenerHeteroevaluacion.mockRejectedValue(new ApiError(404, 'No encontrado'))
    renderizarConSesion(<HeteroevaluacionClase />, { rol: 'ESTUDIANTE' })
    expect(await screen.findByText('Formulario no encontrado')).toBeInTheDocument()
    expect(screen.getByText('Esa clase no está entre tus matrículas de este periodo.')).toBeInTheDocument()
  })

  it('muestra el 409 de etapa o periodo cerrado', async () => {
    obtenerHeteroevaluacion.mockRejectedValue(
      new ApiError(409, 'Los formularios solo se responden en el periodo activo'),
    )
    renderizarConSesion(<HeteroevaluacionClase />, { rol: 'ESTUDIANTE' })
    expect(await screen.findByText('No se puede responder ahora')).toBeInTheDocument()
    expect(screen.getByText('Los formularios solo se responden en el periodo activo')).toBeInTheDocument()
  })
})
