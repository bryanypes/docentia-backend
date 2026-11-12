import { BadgeCheck, BookOpen, Building2, CalendarDays, IdCard, KeyRound, Mail, RefreshCw, ShieldCheck, UserRound } from 'lucide-react'
import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import robotSaludo from '../../../assets/ilustraciones/robot-saludo.svg'
import { Encabezado } from '../../../shared/components/layout/Encabezado/Encabezado.jsx'
import { useAvisos } from '../../../shared/components/ui/Avisos/avisosContext.js'
import { Boton } from '../../../shared/components/ui/Boton/Boton.jsx'
import { Cargando, EstadoVista } from '../../../shared/components/ui/EstadoVista/EstadoVista.jsx'
import { Modal } from '../../../shared/components/ui/Modal/Modal.jsx'
import { ETIQUETA_ROL } from '../../../shared/constants/dominio.js'
import { useFetch } from '../../../shared/hooks/useFetch.js'
import { formatearFecha } from '../../../shared/utils/formatters.js'
import { FormularioContrasena } from '../../auth/components/FormularioContrasena.jsx'
import { useCambioContrasena } from '../../auth/components/useCambioContrasena.js'
import { useSesion } from '../../auth/sesionContext.js'
import { obtenerPerfil } from '../../auth/services/authService.js'
import './MiPerfil.css'

const iniciales = (nombre) =>
  nombre
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0])
    .join('')
    .toUpperCase()

/** Perfil propio de cualquier rol: sus datos, de solo lectura, y la seguridad de la cuenta. */
export default function MiPerfil() {
  const { usuario } = useSesion()
  const ubicacion = useLocation()
  const navegar = useNavigate()
  const perfil = useFetch(obtenerPerfil)
  // Desde /cambiar-contrasena (o un enlace) llega con el modal abierto.
  const [cambiando, setCambiando] = useState(Boolean(ubicacion.state?.cambiarContrasena))

  function cerrarModal() {
    setCambiando(false)
    if (ubicacion.state?.cambiarContrasena) navegar('.', { replace: true, state: null })
  }

  const datos = perfil.datos
  const primerNombre = usuario.nombreCompleto.split(' ')[0]

  return (
    <>
      <Encabezado miga="Cuenta · Mi perfil" titulo="Mi perfil" subtitulo="Tus datos en DOCENTIA y la seguridad de tu cuenta." />
      <div className="pagina perfil">
        <section className="perfil__presentacion" aria-labelledby="perfil-nombre">
          <img className="perfil__robot" src={robotSaludo} alt="" width="120" height="118" />
          <div className="perfil__saludo">
            <span className="perfil__burbuja">¡Hola, {primerNombre}! Esta es tu información en la plataforma.</span>
            <div className="perfil__identidad">
              <span className="perfil__avatar" aria-hidden="true">
                {iniciales(usuario.nombreCompleto)}
              </span>
              <div>
                <h2 id="perfil-nombre" className="perfil__nombre">
                  {usuario.nombreCompleto}
                </h2>
                <span className="perfil__rol">
                  <BadgeCheck size={15} aria-hidden="true" />
                  {ETIQUETA_ROL[usuario.rol]}
                </span>
              </div>
            </div>
          </div>
        </section>

        <div className="perfil__rejilla">
          <section className="panel perfil__datos" aria-labelledby="perfil-datos">
            <h2 id="perfil-datos" className="perfil__titulo">
              <UserRound size={18} aria-hidden="true" />
              Datos de la cuenta
            </h2>
            {perfil.cargando && <Cargando texto="Cargando tu perfil" bloques={2} />}
            {perfil.error && (
              <EstadoVista tipo="error" titulo="No fue posible cargar tu perfil" mensaje={perfil.error.message}>
                <Boton icono={RefreshCw} onClick={perfil.recargar}>
                  Intentar de nuevo
                </Boton>
              </EstadoVista>
            )}
            {datos && (
              <dl className="perfil__lista">
                <Dato icono={Mail} rotulo="Correo institucional" valor={datos.correo} />
                <Dato icono={IdCard} rotulo="Código" valor={datos.codigo} />
                <Dato
                  icono={Building2}
                  rotulo="Alcance"
                  valor={usuario.rol === 'VICERRECTOR' ? 'Toda la universidad' : datos.nombrePrograma ? null : datos.nombreFacultad ? 'Los programas a tu cargo' : null}
                />
                <Dato icono={BookOpen} rotulo="Programa" valor={datos.nombrePrograma} />
                <Dato icono={CalendarDays} rotulo="En la plataforma desde" valor={formatearFecha(datos.fechaCreacion)} />
              </dl>
            )}
            <p className="perfil__nota">Si algún dato no es correcto, pídele a la vicerrectoría que lo actualice.</p>
          </section>

          <section className="panel perfil__seguridad" aria-labelledby="perfil-seguridad">
            <h2 id="perfil-seguridad" className="perfil__titulo">
              <ShieldCheck size={18} aria-hidden="true" />
              Seguridad
            </h2>
            <div className="perfil__opcion">
              <span className="perfil__opcion-icono" aria-hidden="true">
                <KeyRound size={20} />
              </span>
              <div>
                <strong>Contraseña</strong>
                <span>Cámbiala cuando quieras; necesitarás la actual.</span>
              </div>
              <Boton variante="contorno" icono={KeyRound} onClick={() => setCambiando(true)}>
                Cambiar contraseña
              </Boton>
            </div>
          </section>
        </div>
      </div>

      {cambiando && <ModalContrasena onCerrar={cerrarModal} />}
    </>
  )
}

function Dato({ icono: Icono, rotulo, valor }) {
  if (!valor) return null
  return (
    <div className="perfil__dato">
      <Icono size={17} aria-hidden="true" />
      <dt>{rotulo}</dt>
      <dd>{valor}</dd>
    </div>
  )
}

/** Cambio de contraseña sobre Mi perfil, con DocenteIA reaccionando a lo que se escribe. */
function ModalContrasena({ onCerrar }) {
  const { reemplazarSesion } = useSesion()
  const { notificar } = useAvisos()
  const cambio = useCambioContrasena((sesion) => {
    reemplazarSesion(sesion)
    notificar({ tipo: 'exito', titulo: 'Contraseña actualizada', mensaje: 'La usarás desde tu próximo ingreso.' })
    onCerrar()
  })

  return (
    <Modal titulo="Cambiar contraseña" descripcion="Escribe tu contraseña actual y la nueva." icono={KeyRound} ancho="pequeno" onCerrar={onCerrar}>
      <div className="perfil__modal">
        <div className="perfil__modal-robot">
          {/* key: al cambiar de ilustración se hace un fundido corto */}
          <img key={cambio.avatar} src={cambio.avatar} alt="" width="96" height="94" />
          <span aria-live="polite">
            {cambio.enviando
              ? 'Guardando tu contraseña…'
              : cambio.listaErrores.length || cambio.errorGeneral
                ? 'Revisa lo que está marcado.'
                : cambio.listo
                  ? '¡Todo listo para guardar!'
                  : 'Te acompaño mientras la cambias.'}
          </span>
        </div>
        <FormularioContrasena
          cambio={cambio}
          acciones={
            <Boton variante="secundario" bloque onClick={onCerrar} disabled={cambio.enviando}>
              Cancelar
            </Boton>
          }
        />
      </div>
    </Modal>
  )
}
