import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { FormularioInstrumento, INTERVALO_AUTOGUARDADO_MS } from './FormularioInstrumento.jsx'

const FORMULARIO = {
  estado: null,
  ventanaAbierta: true,
  total: 2,
  enviadaEn: null,
  escala: [1, 2, 3, 4, 5].map((valor) => ({ valor, etiqueta: `Nivel ${valor}` })),
  criterios: [
    {
      codigo: 'C1',
      nombre: 'Capacidad para planear',
      comentario: null,
      descriptores: [
        { codigo: 'D01', texto: 'Prepara el curso', valor: null },
        { codigo: 'D02', texto: 'Socializa resultados', valor: null },
      ],
    },
  ],
}

function responderTodo() {
  return Promise.all([]).then(async () => {
    await userEvent.click(screen.getAllByRole('radio', { name: 'Nivel 4' })[0])
    await userEvent.click(screen.getAllByRole('radio', { name: 'Nivel 5' })[1])
  })
}

afterEach(() => vi.useRealTimers())

describe('FormularioInstrumento', () => {
  it('muestra un criterio por paso con su avance', async () => {
    render(<FormularioInstrumento formulario={FORMULARIO} onGuardar={vi.fn()} onEnviar={vi.fn()} />)
    expect(screen.getByRole('group', { name: 'Capacidad para planear' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Capacidad para planear/ })).toHaveAttribute('aria-current', 'step')
    expect(screen.getByRole('group', { name: 'Prepara el curso' })).toBeInTheDocument()
    await userEvent.click(screen.getAllByRole('radio', { name: 'Nivel 4' })[0])
    expect(screen.getByText('1 de 2 ítems respondidos')).toBeInTheDocument()
  })

  it('enviar exige todos los ítems y la confirmación', async () => {
    const onGuardar = vi.fn().mockResolvedValue({})
    const onEnviar = vi.fn().mockResolvedValue({})
    render(<FormularioInstrumento formulario={FORMULARIO} onGuardar={onGuardar} onEnviar={onEnviar} />)
    await responderTodo()
    await userEvent.click(screen.getByRole('button', { name: 'Revisar' }))
    expect(screen.getByText('Completo')).toBeInTheDocument()
    const enviar = screen.getByRole('button', { name: 'Enviar formulario' })
    expect(enviar).toBeDisabled()
    await userEvent.click(screen.getByRole('checkbox'))
    await userEvent.dblClick(enviar)
    expect(onGuardar).toHaveBeenCalledWith({ respuestas: { D01: 4, D02: 5 }, comentarios: { C1: '' } })
    expect(onEnviar).toHaveBeenCalledTimes(1)
  })

  it('guarda cada 30 segundos solo si hubo cambios', async () => {
    vi.useFakeTimers()
    const onGuardar = vi.fn().mockResolvedValue({})
    render(<FormularioInstrumento formulario={FORMULARIO} onGuardar={onGuardar} onEnviar={vi.fn()} />)
    await act(async () => vi.advanceTimersByTime(INTERVALO_AUTOGUARDADO_MS))
    expect(onGuardar).not.toHaveBeenCalled()
    await act(async () => screen.getAllByRole('radio', { name: 'Nivel 3' })[0].click())
    await act(async () => vi.advanceTimersByTime(INTERVALO_AUTOGUARDADO_MS))
    expect(onGuardar).toHaveBeenCalledTimes(1)
    await act(async () => vi.advanceTimersByTime(INTERVALO_AUTOGUARDADO_MS))
    expect(onGuardar).toHaveBeenCalledTimes(1)
  })

  it('enviada queda de solo lectura con su fecha', async () => {
    render(
      <FormularioInstrumento
        formulario={{ ...FORMULARIO, estado: 'ENVIADA', enviadaEn: '2026-10-31T10:00:00' }}
        onGuardar={vi.fn()}
        onEnviar={vi.fn()}
      />,
    )
    expect(screen.getByText(/Se envió el/)).toBeInTheDocument()
    expect(screen.getAllByRole('radio')[0]).toBeDisabled()
    await userEvent.click(screen.getByRole('button', { name: 'Revisar' }))
    expect(screen.queryByRole('button', { name: 'Enviar formulario' })).not.toBeInTheDocument()
  })

  it('avisa si la ventana está cerrada', () => {
    render(
      <FormularioInstrumento formulario={{ ...FORMULARIO, ventanaAbierta: false }} onGuardar={vi.fn()} onEnviar={vi.fn()} />,
    )
    expect(screen.getByText('La recolección está cerrada')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Guardar borrador' })).not.toBeInTheDocument()
  })
})
