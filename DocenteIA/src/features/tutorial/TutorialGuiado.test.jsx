import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderizarConSesion } from '../../pruebas/renderizarConSesion.jsx'
import { TutorialGuiado } from './TutorialGuiado.jsx'
import * as tutorialService from './tutorialService.js'

vi.mock('./tutorialService.js', async (original) => ({
  ...(await original()),
  obtenerTutorial: vi.fn(),
  marcarTutorialVisto: vi.fn(),
}))

describe('TutorialGuiado', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    tutorialService.marcarTutorialVisto.mockResolvedValue({ visto: true })
  })

  it('en el primer inicio bloquea la pantalla y recorre los pasos del coordinador', async () => {
    tutorialService.obtenerTutorial.mockResolvedValue({ visto: false })
    renderizarConSesion(<TutorialGuiado />)

    const dialogo = await screen.findByRole('dialog', { name: 'Tu página de inicio' })
    expect(dialogo).toHaveAttribute('aria-modal', 'true')
    expect(screen.getByText('Paso 1 de 7')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Saltar este paso' }))
    expect(screen.getByRole('heading', { name: 'Análisis de comentarios con IA' })).toBeInTheDocument()
    expect(screen.getByText(/puedes dar tu calificación: tu coevaluación/)).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Anterior' }))
    expect(screen.getByText('Paso 1 de 7')).toBeInTheDocument()
  })

  it('saltar todo el tutorial lo cierra y lo marca como visto', async () => {
    tutorialService.obtenerTutorial.mockResolvedValue({ visto: false })
    renderizarConSesion(<TutorialGuiado />)

    await userEvent.click(await screen.findByRole('button', { name: /Saltar todo el tutorial/ }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(tutorialService.marcarTutorialVisto).toHaveBeenCalledTimes(1)
    expect(screen.getByRole('status')).toHaveTextContent('queda guardado aquí, en el signo de interrogación')

    await userEvent.click(screen.getByRole('button', { name: 'Ver el tutorial' }))
    expect(await screen.findByRole('dialog', { name: 'Tu página de inicio' })).toBeInTheDocument()
  })

  it('en el último paso termina y se puede volver a pedir desde el perfil', async () => {
    tutorialService.obtenerTutorial.mockResolvedValue({ visto: false })
    renderizarConSesion(<TutorialGuiado />, { rol: 'ESTUDIANTE' })

    await userEvent.click(await screen.findByRole('button', { name: 'Siguiente' }))
    expect(screen.getByText(/ver este tutorial otra vez desde Mi perfil/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Terminar' }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

    tutorialService.pedirTutorial()
    expect(await screen.findByRole('dialog', { name: 'Tus evaluaciones' })).toBeInTheDocument()
  })

  it('quien ya lo vio no lo vuelve a ver al entrar, pero tiene el signo de interrogación', async () => {
    tutorialService.obtenerTutorial.mockResolvedValue({ visto: true })
    renderizarConSesion(<TutorialGuiado />)

    await waitFor(() => expect(tutorialService.obtenerTutorial).toHaveBeenCalled())
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ver el tutorial' })).toBeInTheDocument()
  })
})
