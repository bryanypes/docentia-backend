import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { buscarActaPorCodigo, descargarPdfActa, listarRegistroActas } from '../services/actasService.js'
import BuscarActa from './BuscarActa.jsx'

vi.mock('../services/actasService.js')
const sesion = vi.hoisted(() => ({ rol: 'ADMINISTRADOR' }))
vi.mock('../../auth/sesionContext.js', () => ({ useSesion: () => ({ usuario: { rol: sesion.rol } }) }))
vi.mock('../../../shared/components/layout/AppLayout/periodoContext.js', () => ({
  usePeriodoActivo: () => ({ periodo: { nombre: '2026-2' }, cargando: false }),
}))

const ACTA = {
  idActa: 7,
  codigo: 'DOC-1A2B3C4D5E6F',
  nombreActa: 'Acta de compromiso docente',
  estado: 'FIRMADA',
  periodo: '2026-2',
  nombreDocente: 'Nora Munoz',
  correoDocente: 'nora@uniautonoma.edu.co',
  programa: 'Software',
  escuela: 'Escuela de Ingeniería',
  fechaFirma: '2026-11-19T20:12:00',
  firmas: [{ nombre: 'Marta Ruiz', cargo: 'Coordinación de la Escuela de Ingeniería', correo: 'm@u.co', fecha: '2026-11-19T20:12:00' }],
  hashContenido: 'abc',
}
const RETRO = { ...ACTA, idActa: 8, codigo: 'DOC-AAAAAAAAAAAA', nombreActa: 'Acta de retroalimentación docente', nombreDocente: 'Luis Paz' }
const REEMPLAZADA = { ...ACTA, idActa: 6, codigo: 'DOC-000000000000', estado: 'ANULADA', nombreDocente: 'Pedro Paz' }

function abrir() {
  render(
    <MemoryRouter initialEntries={['/buscar-acta']}>
      <BuscarActa />
    </MemoryRouter>,
  )
}

describe('BuscarActa', () => {
  beforeAll(() => {
    // jsdom no implementa <dialog>.
    HTMLDialogElement.prototype.showModal ??= function () {
      this.open = true
    }
    HTMLDialogElement.prototype.close ??= function () {
      this.open = false
    }
  })

  beforeEach(() => {
    vi.clearAllMocks()
    sesion.rol = 'ADMINISTRADOR'
    listarRegistroActas.mockResolvedValue([ACTA, RETRO, REEMPLAZADA])
  })

  it('lista solo las vigentes y filtra por nombre y por tipo', async () => {
    abrir()
    const tabla = await screen.findByRole('table')
    expect(within(tabla).getByText('Nora Munoz')).toBeInTheDocument()
    expect(within(tabla).getByText('Luis Paz')).toBeInTheDocument()
    expect(within(tabla).queryByText('Pedro Paz')).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: /De retroalimentación/ }))
    expect(within(tabla).queryByText('Nora Munoz')).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /Todas/ }))

    await userEvent.type(screen.getByLabelText('Buscar acta'), 'nora')
    expect(within(tabla).queryByText('Luis Paz')).not.toBeInTheDocument()
    expect(within(tabla).getByText('Nora Munoz')).toBeInTheDocument()
  })

  it('con el código completo encuentra también un acta reemplazada', async () => {
    abrir()
    await screen.findByRole('table')
    await userEvent.type(screen.getByLabelText('Buscar acta'), 'doc-000000000000')
    expect(screen.getByText('Pedro Paz')).toBeInTheDocument()
    expect(screen.getByText('Reemplazada')).toBeInTheDocument()
  })

  it('la vicerrectoría abre el detalle con quién firmó y descarga el PDF', async () => {
    sesion.rol = 'VICERRECTOR'
    buscarActaPorCodigo.mockResolvedValue(ACTA)
    descargarPdfActa.mockResolvedValue(new Blob(['%PDF']))
    URL.createObjectURL = vi.fn(() => 'blob:x')
    URL.revokeObjectURL = vi.fn()
    abrir()
    await userEvent.click(await screen.findByRole('button', { name: 'Ver el acta de Nora Munoz' }))
    expect(await screen.findByText('Marta Ruiz')).toBeInTheDocument()
    expect(screen.getByText('Acta vigente')).toBeInTheDocument()
    expect(buscarActaPorCodigo).toHaveBeenCalledWith('DOC-1A2B3C4D5E6F')

    await userEvent.click(screen.getByRole('button', { name: 'Descargar PDF' }))
    expect(descargarPdfActa).toHaveBeenCalledWith(7)
  })

  it('la administración ve el registro y quién firmó, pero no descarga el PDF (lleva la nota)', async () => {
    sesion.rol = 'ADMINISTRADOR'
    buscarActaPorCodigo.mockResolvedValue(ACTA)
    abrir()
    await userEvent.click(await screen.findByRole('button', { name: 'Ver el acta de Nora Munoz' }))
    expect(await screen.findByText('Marta Ruiz')).toBeInTheDocument()

    expect(screen.queryByRole('button', { name: 'Descargar PDF' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Descargar el acta de/ })).not.toBeInTheDocument()
    expect(screen.getByText(/el PDF lleva la nota y no se descarga desde aquí/)).toBeInTheDocument()
    expect(descargarPdfActa).not.toHaveBeenCalled()
  })

  it('avisa cuando nada coincide', async () => {
    abrir()
    await screen.findByRole('table')
    await userEvent.type(screen.getByLabelText('Buscar acta'), 'zzz')
    expect(screen.getByText('Ningún acta coincide con «zzz»')).toBeInTheDocument()
  })
})
