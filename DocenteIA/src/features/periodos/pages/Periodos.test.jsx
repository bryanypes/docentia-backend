import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import { ApiError } from '../../../services/api.js'
import { activarPeriodo, listarPeriodos } from '../services/periodosService.js'
import Periodos, { FormularioPeriodo } from './Periodos.jsx'

vi.mock('../services/periodosService.js')

beforeEach(() => vi.clearAllMocks())

function llenarDatos() {
  fireEvent.change(screen.getByLabelText('Nombre'), { target: { value: '2027-1' } })
  fireEvent.change(screen.getByLabelText('Inicio'), { target: { value: '2027-02-01' } })
  fireEvent.change(screen.getByLabelText('Fin'), { target: { value: '2027-06-05' } })
}

describe('FormularioPeriodo', () => {
  it('envía solo las etapas con fechas y con el tipo que acepta el backend', async () => {
    const onGuardar = vi.fn().mockResolvedValue()
    render(<FormularioPeriodo onGuardar={onGuardar} />)
    llenarDatos()
    fireEvent.change(screen.getByLabelText('Apertura de Firma de actas'), { target: { value: '2027-05-21' } })
    fireEvent.change(screen.getByLabelText('Cierre de Firma de actas'), { target: { value: '2027-06-01' } })

    await userEvent.click(screen.getByRole('button', { name: 'Crear periodo' }))

    expect(onGuardar).toHaveBeenCalledWith({
      nombre: '2027-1',
      fechaInicio: '2027-02-01',
      fechaFin: '2027-06-05',
      etapas: [{ tipoEtapa: 'FIRMA_ACTAS', fechaApertura: '2027-05-21', fechaCierre: '2027-06-01' }],
    })
  })

  it('no envía una etapa a la que le falta el cierre', async () => {
    const onGuardar = vi.fn()
    render(<FormularioPeriodo onGuardar={onGuardar} />)
    llenarDatos()
    fireEvent.change(screen.getByLabelText('Apertura de Agenda de citas'), { target: { value: '2027-05-01' } })

    await userEvent.click(screen.getByRole('button', { name: 'Crear periodo' }))

    expect(onGuardar).not.toHaveBeenCalled()
    expect(screen.getByRole('alert')).toHaveTextContent('Agenda de citas: necesita fecha de apertura y de cierre')
  })

  it('muestra el error del backend', async () => {
    const onGuardar = vi.fn().mockRejectedValue(new ApiError(409, 'Ya existe un periodo académico con ese nombre'))
    render(<FormularioPeriodo onGuardar={onGuardar} />)
    llenarDatos()

    await userEvent.click(screen.getByRole('button', { name: 'Crear periodo' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Ya existe un periodo académico con ese nombre')
  })
})

describe('FormularioPeriodo · nombre calculado', () => {
  it('calcula AAAA-1 o AAAA-2 desde la fecha de inicio y no deja editarlo sin ser administración', async () => {
    const onGuardar = vi.fn().mockResolvedValue()
    render(<FormularioPeriodo onGuardar={onGuardar} />)
    const nombre = screen.getByLabelText('Nombre')
    fireEvent.change(screen.getByLabelText('Inicio'), { target: { value: '2027-08-02' } })
    fireEvent.change(screen.getByLabelText('Fin'), { target: { value: '2027-12-05' } })
    expect(nombre).toHaveValue('2027-2')
    expect(nombre).toHaveAttribute('readonly')
    fireEvent.change(screen.getByLabelText('Inicio'), { target: { value: '2027-03-01' } })
    expect(nombre).toHaveValue('2027-1')

    await userEvent.click(screen.getByRole('button', { name: 'Crear periodo' }))
    expect(onGuardar).toHaveBeenCalledWith(expect.objectContaining({ nombre: '2027-1' }))
  })

  it('administración ajusta el nombre, pero solo con formato AAAA-1 o AAAA-2', async () => {
    const onGuardar = vi.fn().mockResolvedValue()
    render(<FormularioPeriodo onGuardar={onGuardar} puedeAjustarNombre />)
    fireEvent.change(screen.getByLabelText('Inicio'), { target: { value: '2027-02-01' } })
    fireEvent.change(screen.getByLabelText('Fin'), { target: { value: '2027-06-05' } })
    fireEvent.change(screen.getByLabelText('Nombre'), { target: { value: '2027 - primero' } })

    await userEvent.click(screen.getByRole('button', { name: 'Crear periodo' }))
    expect(onGuardar).not.toHaveBeenCalled()
    expect(screen.getByRole('alert')).toHaveTextContent('formato AAAA-1 o AAAA-2')

    fireEvent.change(screen.getByLabelText('Nombre'), { target: { value: '2026-2' } })
    await userEvent.click(screen.getByRole('button', { name: 'Crear periodo' }))
    expect(onGuardar).toHaveBeenCalledWith(expect.objectContaining({ nombre: '2026-2' }))
  })
})

describe('Periodos · activación', () => {
  const PERIODOS = [
    { idPeriodo: 1, nombre: '2026-2', fechaInicio: '2026-07-01', fechaFin: '2026-12-31', esActivo: true, etapas: [] },
    { idPeriodo: 2, nombre: '2027-1', fechaInicio: '2027-02-01', fechaFin: '2027-06-05', esActivo: false, etapas: [] },
  ]

  async function pedirActivar() {
    listarPeriodos.mockResolvedValue(PERIODOS)
    renderizarConSesion(<Periodos />, { rol: 'VICERRECTOR' })
    const tarjeta = (await screen.findByRole('heading', { name: '2027-1' })).closest('section')
    await userEvent.click(within(tarjeta).getByRole('button', { name: 'Activar' }))
    await userEvent.click(
      within(await screen.findByRole('dialog', { name: '¿Activar 2027-1?' })).getByRole('button', { name: 'Activar' }),
    )
  }

  it('si el periodo no tiene matrículas pide una segunda confirmación antes de activarlo', async () => {
    activarPeriodo
      .mockRejectedValueOnce(new ApiError(409, 'El periodo 2027-1 no tiene matrículas: confirma para activarlo igual'))
      .mockResolvedValueOnce({})

    await pedirActivar()
    const aviso = await screen.findByRole('dialog', { name: '2027-1 no tiene matrículas' })
    expect(aviso).toHaveTextContent('no tiene matrículas')
    await userEvent.click(within(aviso).getByRole('button', { name: 'Activar de todos modos' }))

    expect(activarPeriodo).toHaveBeenNthCalledWith(1, 2)
    expect(activarPeriodo).toHaveBeenNthCalledWith(2, 2, true)
  })

  it('si no se confirma por segunda vez, el periodo no se activa ni se muestra error', async () => {
    activarPeriodo.mockRejectedValueOnce(new ApiError(409, 'El periodo 2027-1 no tiene matrículas'))

    await pedirActivar()
    const aviso = await screen.findByRole('dialog', { name: '2027-1 no tiene matrículas' })
    await userEvent.click(within(aviso).getByRole('button', { name: 'Cancelar' }))

    expect(activarPeriodo).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })
})
