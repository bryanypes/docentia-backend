import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { render } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { SesionContext } from '../../auth/sesionContext.js'
import { ApiError } from '../../../services/api.js'
import { enviarEvaluacion, obtenerEvaluacion } from '../../estudiante/services/estudianteService.js'
import HeteroevaluacionClase from './HeteroevaluacionClase.jsx'

vi.mock('../../estudiante/services/estudianteService.js')

const FORMULARIO = {
  idAsignacion: 7,
  curso: 'Programación web',
  grupo: 'A',
  docente: 'Laura',
  estado: 'PENDIENTE',
  ventanaAbierta: true,
  total: 2,
  aviso: 'Tus respuestas son anónimas y se usan para la evaluación de la enseñanza.',
  escala: [1, 2, 3, 4, 5].map((valor) => ({ valor, etiqueta: `Nivel ${valor}` })),
  criterios: [
    {
      codigo: 'C2',
      nombre: 'Capacidad para ejecutar',
      descriptores: [
        { codigo: 'D02', texto: 'Socializa resultados' },
        { codigo: 'D05', texto: 'Expresa con claridad' },
      ],
    },
  ],
}

function abrir() {
  const usuario = { idUsuario: 1, nombreCompleto: 'Est', rol: 'ESTUDIANTE' }
  render(
    <MemoryRouter initialEntries={['/evaluacion-profesores/7']}>
      <SesionContext.Provider value={{ usuario, validando: false }}>
        <Routes>
          <Route path="/evaluacion-profesores/:idAsignacion" element={<HeteroevaluacionClase />} />
        </Routes>
      </SesionContext.Provider>
    </MemoryRouter>,
  )
}

beforeEach(() => vi.clearAllMocks())

describe('HeteroevaluacionClase', () => {
  it('avisa el anonimato, exige todo y la confirmación, y agradece', async () => {
    obtenerEvaluacion.mockResolvedValue(FORMULARIO)
    let terminar
    enviarEvaluacion.mockReturnValue(new Promise((resolver) => (terminar = resolver)))
    abrir()
    expect(await screen.findByText('Tus respuestas son anónimas')).toBeInTheDocument()
    // Por pasos: primero el criterio, luego la revisión con el comentario y el envío.
    expect(screen.getByRole('heading', { name: 'Capacidad para ejecutar' })).toBeInTheDocument()
    await userEvent.click(screen.getAllByRole('radio', { name: 'Nivel 4' })[0])
    expect(screen.getByText('1 de 2 ítems respondidos')).toBeInTheDocument()
    await userEvent.click(screen.getAllByRole('radio', { name: 'Nivel 5' })[1])
    await userEvent.click(screen.getByRole('button', { name: 'Revisar' }))
    expect(screen.getByText(/No incluyas datos personales/)).toBeInTheDocument()
    const boton = screen.getByRole('button', { name: 'Enviar evaluación' })
    expect(boton).toBeDisabled()
    await userEvent.click(screen.getByRole('checkbox'))
    await userEvent.dblClick(boton)
    expect(enviarEvaluacion).toHaveBeenCalledTimes(1)
    expect(enviarEvaluacion).toHaveBeenCalledWith('7', { respuestas: { D02: 4, D05: 5 }, comentario: null })
    terminar({ estado: 'ENVIADA' })
    expect(await screen.findByText('¡Gracias por tu evaluación!')).toBeInTheDocument()
  })

  it('muestra el 409 del doble envío', async () => {
    obtenerEvaluacion.mockResolvedValue({ ...FORMULARIO, total: 1, criterios: [{ ...FORMULARIO.criterios[0], descriptores: [FORMULARIO.criterios[0].descriptores[0]] }] })
    enviarEvaluacion.mockRejectedValue(new ApiError(409, 'Ya enviaste esta evaluación'))
    abrir()
    await userEvent.click((await screen.findAllByRole('radio', { name: 'Nivel 3' }))[0])
    await userEvent.click(screen.getByRole('button', { name: 'Revisar' }))
    await userEvent.click(screen.getByRole('checkbox'))
    await userEvent.click(screen.getByRole('button', { name: 'Enviar evaluación' }))
    expect(await screen.findByText('Ya enviaste esta evaluación')).toBeInTheDocument()
  })

  it('una evaluación enviada o cerrada no se puede responder', async () => {
    obtenerEvaluacion.mockResolvedValue({ ...FORMULARIO, estado: 'ENVIADA' })
    abrir()
    expect(await screen.findByText('Ya enviaste esta evaluación')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Enviar evaluación' })).not.toBeInTheDocument()
  })

  it('una asignación ajena responde 404', async () => {
    obtenerEvaluacion.mockRejectedValue(new ApiError(404, 'Evaluación no encontrada'))
    abrir()
    expect(await screen.findByText('Evaluación no encontrada')).toBeInTheDocument()
  })
})
