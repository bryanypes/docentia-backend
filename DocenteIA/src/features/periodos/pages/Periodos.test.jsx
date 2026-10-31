import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../../services/api.js'
import { FormularioPeriodo } from './Periodos.jsx'

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
