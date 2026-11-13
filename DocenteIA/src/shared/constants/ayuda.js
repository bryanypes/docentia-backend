/** Dominio de los correos de la Corporación: solo ellos pueden ingresar a DOCENTIA. */
export const DOMINIO_INSTITUCIONAL = 'uniautonoma.edu.co'

/** NOVA, el portal institucional de trámites y soporte. Se puede cambiar por despliegue con VITE_URL_NOVA. */
export const URL_NOVA = (import.meta.env.VITE_URL_NOVA?.trim() || 'https://nova.uniautonoma.edu.co').replace(/\/$/, '')

const ticketsDe = (ruta) => `${URL_NOVA}/login?redirect=${encodeURIComponent(ruta)}&accion=tickets`

/** Tickets de soporte en NOVA, según el perfil de quien pide ayuda. */
export const TICKETS_NOVA = {
  GENERAL: ticketsDe('/dashboard?tab=tickets'),
  ESTUDIANTE: ticketsDe('/estudiante?s=tickets'),
  DOCENTE: ticketsDe('/docente?s=tickets'),
  ADMINISTRATIVO: ticketsDe('/area?tab=tickets'),
}

/** Otros servicios públicos de NOVA. */
export const SERVICIOS_NOVA = {
  PQRS: `${URL_NOVA}/pqrs`,
  CONTACTOS: `${URL_NOVA}/contactos`,
  MANUAL: `${URL_NOVA}/manual`,
  POLITICA_DATOS: `${URL_NOVA}/politica-datos`,
}

/** Mesa de ayuda por defecto (tickets de coordinación y demás perfiles). */
export const URL_MESA_AYUDA = TICKETS_NOVA.GENERAL

/** true si el correo tiene forma válida pero no es de la Corporación. */
export function esCorreoExterno(correo) {
  const dominio = correo.trim().toLowerCase().split('@')[1]
  return Boolean(dominio) && dominio !== DOMINIO_INSTITUCIONAL
}
