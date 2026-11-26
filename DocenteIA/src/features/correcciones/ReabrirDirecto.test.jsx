import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listarUsuarios } from '../usuarios/services/usuariosService.js'
import { listarFormulariosDocente, reabrirDirecto } from './correccionesService.js'
import { ReabrirDirecto } from './ReabrirDirecto.jsx'

vi.mock('../usuarios/services/usuariosService.js')
vi.mock('./correccionesService.js')

const USUARIOS = [
  { idUsuario: 4, nombreCompleto: 'Nora Ficción', correo: 'nora@u.co', rol: 'DOCENTE', esActivo: true },
  { idUsuario: 9, nombreCompleto: 'Luis Muestra', correo: 'luis@u.co', rol: 'ESTUDIANTE', esActivo: true },
]
const FORMULARIOS = [
  { idFormulario: 21, tipo: 'AUTO', nombreEvaluador: 'Nora Ficción', enviadaEn: '2026-11-19T10:00:00', reabrible: true },
  {
    idFormulario: 22,
    tipo: 'COEVAL',
    nombreEvaluador: 'Paola Patrón',
    enviadaEn: '2026-11-19T11:00:00',
    reabrible: false,
    motivoNoReabrible: 'El acta de este periodo ya está firmada',
  },
]

beforeEach(() => {
  vi.clearAllMocks()
  listarUsuarios.mockResolvedValue(USUARIOS)
  listarFormulariosDocente.mockResolvedValue(FORMULARIOS)
  reabrirDirecto.mockResolvedValue({ estado: 'ATENDIDA' })
})

describe('ReabrirDirecto', () => {
  it('solo ofrece docentes y coordinadores, y dice por qué un formulario no se reabre', async () => {
    render(<ReabrirDirecto />)
    const selector = await screen.findByLabelText('Docente')
    expect(screen.queryByRole('option', { name: /Luis Muestra/ })).not.toBeInTheDocument()
    await userEvent.selectOptions(selector, '4')

    expect(await screen.findByText('Autoevaluación')).toBeInTheDocument()
    expect(screen.getByText('El acta de este periodo ya está firmada')).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Reabrir' })).toHaveLength(1)
  })

  it('reabre con un motivo y avisa a quien la respondió', async () => {
    const onReabierto = vi.fn()
    render(<ReabrirDirecto onReabierto={onReabierto} />)
    await userEvent.selectOptions(await screen.findByLabelText('Docente'), '4')
    await userEvent.click(await screen.findByRole('button', { name: 'Reabrir' }))

    const confirmar = screen.getByRole('button', { name: 'Confirmar reapertura' })
    expect(confirmar).toBeDisabled()
    await userEvent.type(screen.getByLabelText('Motivo de la reapertura'), 'Respondió por error la escala')
    await userEvent.click(confirmar)

    expect(reabrirDirecto).toHaveBeenCalledWith(21, 'Respondió por error la escala')
    expect(await screen.findByText('Evaluación reabierta')).toBeInTheDocument()
    expect(screen.getByText(/Le avisamos a Nora Ficción/)).toBeInTheDocument()
    expect(onReabierto).toHaveBeenCalled()
  })
})
