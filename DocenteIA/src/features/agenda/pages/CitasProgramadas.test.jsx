import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import { listarPeriodos } from '../../periodos/services/periodosService.js'
import { listarCitasProgramadas } from '../services/agendaService.js'
import CitasProgramadas from './CitasProgramadas.jsx'

vi.mock('../services/agendaService.js')
vi.mock('../../periodos/services/periodosService.js')

const CITAS = [
  {
    idCita: 1,
    fecha: '2026-11-21',
    horaInicio: '09:00:00',
    horaFin: '09:30:00',
    nombreDocente: 'Nora Ficción',
    rolDocente: 'DOCENTE',
    unidad: 'Software',
    nombreAnfitrion: 'Marta Ruiz',
    rolAnfitrion: 'COORDINADOR',
    confirmacion: 'CONFIRMADA',
    asistencia: null,
  },
  {
    idCita: 2,
    fecha: '2026-11-22',
    horaInicio: '10:00:00',
    horaFin: '10:30:00',
    nombreDocente: 'Andrés Plantilla',
    rolDocente: 'COORDINADOR',
    unidad: 'Ingeniería',
    nombreAnfitrion: 'Elena Ficción',
    rolAnfitrion: 'VICERRECTOR',
    confirmacion: null,
    asistencia: null,
  },
]

beforeEach(() => {
  vi.clearAllMocks()
  listarPeriodos.mockResolvedValue([])
  listarCitasProgramadas.mockResolvedValue(CITAS)
})

describe('CitasProgramadas', () => {
  it('muestra las citas de coordinación y de vicerrectoría, por día', async () => {
    renderizarConSesion(<CitasProgramadas />, { rol: 'ADMINISTRADOR' })
    expect(await screen.findByText('Nora Ficción')).toBeInTheDocument()
    expect(screen.getByText('Andrés Plantilla')).toBeInTheDocument()
    expect(screen.getByText('con Marta Ruiz (coordinación)')).toBeInTheDocument()
    expect(screen.getByText('con Elena Ficción (vicerrectoría)')).toBeInTheDocument()
    expect(screen.getByText('Confirmada', { selector: '.pildora' })).toBeInTheDocument()
    expect(screen.getByText('Por confirmar', { selector: '.pildora' })).toBeInTheDocument()
  })

  it('filtra por tipo de reunión y por búsqueda', async () => {
    renderizarConSesion(<CitasProgramadas />, { rol: 'ADMINISTRADOR' })
    await userEvent.click(await screen.findByRole('button', { name: /Vicerrectoría ↔ coordinadores/ }))
    expect(screen.queryByText('Nora Ficción')).not.toBeInTheDocument()
    expect(screen.getByText('Andrés Plantilla')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Quitar filtros' }))
    await userEvent.type(screen.getByRole('searchbox'), 'marta')
    expect(screen.getByText('Nora Ficción')).toBeInTheDocument()
    expect(screen.queryByText('Andrés Plantilla')).not.toBeInTheDocument()
  })
})
