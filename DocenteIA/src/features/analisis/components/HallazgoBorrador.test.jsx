import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { HallazgoBorrador } from './HallazgoBorrador.jsx'

vi.mock('../services/analisisService.js', () => ({ listarVersionesHallazgo: vi.fn().mockResolvedValue([]) }))

const HALLAZGO = { idHallazgo: 1, texto: 'Explica bien', estado: 'PENDIENTE_VALIDACION', citas: [] }

describe('HallazgoBorrador', () => {
  it('descarta con motivo', async () => {
    const onDescartar = vi.fn().mockResolvedValue(true)
    render(<HallazgoBorrador hallazgo={HALLAZGO} puedeValidar onDescartar={onDescartar} />)
    await userEvent.click(screen.getByRole('button', { name: 'Descartar' }))
    await userEvent.type(screen.getByLabelText('Motivo del descarte (opcional)'), 'Repetido')
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar descarte' }))
    expect(onDescartar).toHaveBeenCalledWith(HALLAZGO, 'Repetido')
  })

  it('edita el texto', async () => {
    const onEditar = vi.fn().mockResolvedValue(true)
    render(<HallazgoBorrador hallazgo={HALLAZGO} puedeValidar onEditar={onEditar} />)
    await userEvent.click(screen.getByRole('button', { name: 'Editar texto' }))
    const campo = screen.getByLabelText('Texto del hallazgo')
    await userEvent.clear(campo)
    await userEvent.type(campo, 'Mejor redacción')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar texto' }))
    expect(onEditar).toHaveBeenCalledWith(HALLAZGO, 'Mejor redacción')
  })

  it('sin permiso no muestra acciones', () => {
    render(<HallazgoBorrador hallazgo={HALLAZGO} puedeValidar={false} />)
    expect(screen.queryByRole('button', { name: 'Descartar' })).not.toBeInTheDocument()
  })
})
