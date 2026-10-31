import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { FormularioPonderacion } from './Ponderaciones.jsx'

const GUARDADA = { pesoEstudiantes: 50, pesoCoordinacion: 30, pesoVicerrectoria: 20 }

describe('FormularioPonderacion', () => {
  it('solo deja guardar si los pesos suman 100', async () => {
    const onGuardar = vi.fn().mockResolvedValue()
    render(<FormularioPonderacion guardada={GUARDADA} onGuardar={onGuardar} />)
    const estudiantes = screen.getByLabelText('Estudiantes (%)')

    await userEvent.clear(estudiantes)
    await userEvent.type(estudiantes, '40')
    expect(screen.getByRole('button', { name: 'Guardar ponderación' })).toBeDisabled()

    await userEvent.clear(screen.getByLabelText('Coordinación (%)'))
    await userEvent.type(screen.getByLabelText('Coordinación (%)'), '40')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar ponderación' }))

    expect(onGuardar).toHaveBeenCalledWith({ pesoEstudiantes: 40, pesoCoordinacion: 40, pesoVicerrectoria: 20 })
  })
})
