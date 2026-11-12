import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../../pruebas/renderizarConSesion.jsx'
import { api, ApiError } from '../../../services/api.js'
import { RegistroAuditoria as Auditoria } from '../components/RegistroAuditoria.jsx'

vi.spyOn(api, 'get')

beforeEach(() => vi.clearAllMocks())

describe('Auditoria', () => {
  it('lista y filtra por acción', async () => {
    api.get.mockResolvedValue({
      contenido: [{ id: 1, correo: 'a@b.co', accion: 'FIRMA_ACTA', entidad: 'ACTA', entidadId: 3, fecha: '2026-10-31T10:00:00', ip: '1.1.1.1', resultado: 'EXITO' }],
      totalElementos: 1,
      totalPaginas: 1,
    })
    renderizarConSesion(<Auditoria />, { rol: 'VICERRECTOR' })
    expect(await screen.findByText('a@b.co')).toBeInTheDocument()
    await userEvent.selectOptions(screen.getByLabelText('Acción'), 'LOGIN')
    expect(api.get).toHaveBeenLastCalledWith(expect.stringContaining('accion=LOGIN'))
  })

  it('muestra el error', async () => {
    api.get.mockRejectedValue(new ApiError(500, 'Falló'))
    renderizarConSesion(<Auditoria />, { rol: 'VICERRECTOR' })
    expect(await screen.findByText('No fue posible cargar la auditoría')).toBeInTheDocument()
  })
})
