import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { PanelActa } from '../components/PanelActa.jsx'

export default function MiActa() {
  return (
    <>
      <Encabezado
        miga="Docente · Mi acta"
        titulo="Mi acta"
        subtitulo="Se habilita en tu reunión de retroalimentación y la firmas ahí mismo. Firmar es irreversible y genera el PDF con su código de verificación."
      />
      <div className="pagina">
        <PanelActa />
      </div>
    </>
  )
}
