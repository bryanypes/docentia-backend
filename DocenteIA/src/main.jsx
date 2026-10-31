import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { AppRouter } from './app/AppRouter.jsx'
import { LimiteError } from './shared/components/ui/LimiteError/LimiteError.jsx'
import './styles/variables.css'
import './styles/globals.css'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <LimiteError>
      <AppRouter />
    </LimiteError>
  </StrictMode>,
)
