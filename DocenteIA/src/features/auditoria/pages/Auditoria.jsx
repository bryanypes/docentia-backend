import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { RegistroAuditoria } from '../components/RegistroAuditoria.jsx'

/** Página propia de auditoría. En el menú ya no aparece: se consulta en Usuarios → Actividad. */
export default function Auditoria() {
  return (
    <>
      <Encabezado miga="Vicerrectoría · Auditoría" titulo="Auditoría" subtitulo="Quién hizo qué y cuándo. Solo lectura." />
      <div className="pagina">
        <RegistroAuditoria />
      </div>
    </>
  )
}
