import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import { ApiError } from '../../../services/api.js'
import * as docentesService from '../../docentes/services/docentesService.js'
import * as periodosService from '../../periodos/services/periodosService.js'
import * as analisisService from '../services/analisisService.js'
import Analisis from './Analisis.jsx'

vi.mock('../services/analisisService.js')
vi.mock('../../docentes/services/docentesService.js')
vi.mock('../../periodos/services/periodosService.js')

const COMPLETADO = {
  idSolicitud: 1,
  estado: 'COMPLETADO',
  mensaje: null,
  encuestasCerradas: 15,
  comentariosEnviados: 13,
  comentariosFiltrados: 1,
  fechaSolicitud: '2026-10-27T09:00:01.2',
  borrador: {
    modelo: 'simulado-determinista',
    versionInstruccion: 'analisis-v1',
    fechaGeneracion: '2026-10-27T09:00:04.1',
    hallazgos: [
      {
        idHallazgo: 31,
        tipo: 'FORTALEZA',
        texto: 'Explica con ejemplos reales.',
        estado: 'PENDIENTE_VALIDACION',
        citas: [
          {
            idCita: 40,
            fragmento: 'explica con ejemplos reales',
            verificada: true,
            comentario: 'Explica con ejemplos reales del sector.',
          },
        ],
      },
      {
        idHallazgo: 32,
        tipo: 'OPORTUNIDAD_DE_MEJORA',
        texto: 'Mejorar el trato.',
        estado: 'SIN_CITA_VERIFICADA',
        citas: [{ idCita: 41, fragmento: 'se sienten ignorados', verificada: false, comentario: null }],
      },
    ],
  },
}

function renderizar(rol = 'COORDINADOR') {
  renderizarConSesion(<Analisis />, { rol, ruta: '/analisis?docente=4&periodo=2' })
}

describe('Analisis', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    docentesService.listarDocentes.mockResolvedValue([
      { idDocente: 4, nombreCompleto: 'Julián Camilo Hoyos', nombrePrograma: 'Software' },
    ])
    periodosService.listarPeriodos.mockResolvedValue([{ idPeriodo: 2, nombre: '2026-2', esActivo: true }])
    analisisService.listarComentariosFiltrados.mockResolvedValue([
      { idComentario: 112, texto: 'Se viste mal.', categoria: 'APARIENCIA' },
    ])
  })

  it('muestra el borrador con citas, comentario de origen y filtrados', async () => {
    analisisService.listarSolicitudes.mockResolvedValue([COMPLETADO])
    analisisService.obtenerSolicitud.mockResolvedValue(COMPLETADO)
    renderizar()

    expect(await screen.findByText('Explica con ejemplos reales.')).toBeInTheDocument()
    expect(screen.getByText('«explica con ejemplos reales»')).toBeInTheDocument()
    expect(screen.getByText('Explica con ejemplos reales del sector.')).toBeInTheDocument()
    expect(screen.getByText(/No aparece tal cual/)).toBeInTheDocument()
    expect(await screen.findByText('Apariencia física')).toBeInTheDocument()
    expect(screen.getByText('0 de 2 hallazgos validados.')).toBeInTheDocument()
  })

  it('validar un hallazgo sin cita verificada muestra el 422 del backend', async () => {
    analisisService.listarSolicitudes.mockResolvedValue([COMPLETADO])
    analisisService.obtenerSolicitud.mockResolvedValue(COMPLETADO)
    analisisService.validarHallazgo.mockRejectedValue(new ApiError(422, 'El hallazgo no tiene ninguna cita'))
    renderizar()

    const hallazgo = (await screen.findByText('Mejorar el trato.')).closest('article')
    await userEvent.click(within(hallazgo).getByRole('button', { name: 'Marcar como válido' }))

    expect(await within(hallazgo).findByRole('alert')).toHaveTextContent('El hallazgo no tiene ninguna cita')
    expect(analisisService.validarHallazgo).toHaveBeenCalledWith(32)
  })

  it('con poca evidencia dice cuántas encuestas hay de las 10', async () => {
    const insuficiente = {
      ...COMPLETADO,
      estado: 'INSUFICIENTE',
      encuestasCerradas: 6,
      mensaje: 'evidencia insuficiente (6 de 10 requeridas)',
      borrador: null,
    }
    analisisService.listarSolicitudes.mockResolvedValue([insuficiente])
    analisisService.obtenerSolicitud.mockResolvedValue(insuficiente)
    renderizar()

    expect(await screen.findByText(/6 de 10 encuestas con comentario/)).toBeInTheDocument()
  })

  it('solicitar un análisis lo pone en cola y lo sigue', async () => {
    const enCola = { ...COMPLETADO, estado: 'EN_COLA', borrador: null }
    analisisService.listarSolicitudes.mockResolvedValue([])
    analisisService.solicitarAnalisis.mockResolvedValue(enCola)
    analisisService.obtenerSolicitud.mockResolvedValue(enCola)
    renderizar()

    await userEvent.click(await screen.findByRole('button', { name: 'Solicitar análisis' }))

    expect(analisisService.solicitarAnalisis).toHaveBeenCalledWith('4', '2')
    expect(await screen.findByText('En cola…')).toBeInTheDocument()
  })

  it('la vicerrectoría consulta pero no solicita ni valida', async () => {
    analisisService.listarSolicitudes.mockResolvedValue([COMPLETADO])
    analisisService.obtenerSolicitud.mockResolvedValue(COMPLETADO)
    renderizar('VICERRECTOR')

    expect(await screen.findByText('Explica con ejemplos reales.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Solicitar/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Marcar como válido' })).not.toBeInTheDocument()
  })
})
