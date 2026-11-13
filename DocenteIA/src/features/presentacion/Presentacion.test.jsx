import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import Presentacion from './Presentacion.jsx'

function renderPresentacion() {
  return render(
    <MemoryRouter>
      <Presentacion />
    </MemoryRouter>,
  )
}

describe('Presentacion', () => {
  it('explica qué es, cómo funciona y lleva al inicio de sesión', () => {
    renderPresentacion()
    expect(screen.getByRole('heading', { level: 1, name: 'Tu voz mejora la enseñanza' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Cómo funciona' })).toBeInTheDocument()
    const enlaces = screen.getAllByRole('link', { name: /Iniciar sesión/ })
    expect(enlaces.length).toBeGreaterThan(0)
    for (const enlace of enlaces) expect(enlace).toHaveAttribute('href', '/login')
    expect(screen.getByRole('img', { name: 'Universidad Autónoma del Cauca' })).toHaveAttribute(
      'src',
      '/brand/logo-uniautonoma.png',
    )
  })

  it('invita a ver los tutoriales', () => {
    renderPresentacion()
    expect(screen.getByRole('heading', { name: '¿No sabes cómo usar Evaluación Docente?' })).toBeInTheDocument()
    for (const enlace of screen.getAllByRole('link', { name: /Ver tutoriales|Tutoriales/ })) {
      expect(enlace).toHaveAttribute('href', '/tutoriales')
    }
  })

  it('pone el anonimato de los comentarios en primer plano', () => {
    renderPresentacion()
    expect(screen.getByRole('heading', { name: 'Tus comentarios siempre son anónimos' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Nadie sabe quién escribió' })).toBeInTheDocument()
  })

  it('el buzón de ayuda lleva a NOVA según el perfil, en otra pestaña', () => {
    renderPresentacion()
    expect(screen.getByRole('heading', { name: '¿Tienes dudas, encontraste un error o necesitas ayuda?' })).toBeInTheDocument()
    const estudiantes = screen.getByRole('link', { name: /^Estudiantes/ })
    expect(estudiantes).toHaveAttribute('href', expect.stringContaining('nova.uniautonoma.edu.co/login?redirect=%2Festudiante'))
    expect(estudiantes).toHaveAttribute('target', '_blank')
    expect(screen.getByRole('link', { name: /Radicar una PQRS/ })).toHaveAttribute('href', 'https://nova.uniautonoma.edu.co/pqrs')
    expect(screen.getByRole('link', { name: /Directorio de contactos/ })).toHaveAttribute(
      'href',
      'https://nova.uniautonoma.edu.co/contactos',
    )
  })
})
