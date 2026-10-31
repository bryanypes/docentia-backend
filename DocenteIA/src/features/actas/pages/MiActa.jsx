import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { PanelActa } from '../components/PanelActa.jsx'

export default function MiActa() {
  return (
    <>
      <Encabezado
        miga="Docente · Mi acta"
        titulo="Mi acta de retroalimentación"
        subtitulo="Revísala después de tu cita. Firmar es irreversible y genera el PDF con su código de verificación."
      />
      <div className="pagina">
        <PanelActa />
      </div>
    </>
  )
}
