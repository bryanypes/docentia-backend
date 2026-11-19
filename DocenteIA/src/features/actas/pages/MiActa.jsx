import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { ROLES } from '../../../shared/constants/dominio.js'
import { useSesion } from '../../auth/sesionContext.js'
import { PanelActa } from '../components/PanelActa.jsx'

export default function MiActa() {
  const { usuario } = useSesion()
  return (
    <>
      <Encabezado
        miga={usuario.rol === ROLES.COORDINADOR ? 'Coordinación · Mi acta como docente' : 'Docente · Mi acta'}
        titulo="Mi acta"
        subtitulo="Se habilita en tu reunión de retroalimentación y la firmas ahí mismo. Firmar es irreversible y genera el PDF con su código de verificación."
      />
      <div className="pagina">
        <PanelActa />
      </div>
    </>
  )
}
