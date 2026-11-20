import { useFetch } from '../../../shared/hooks/useFetch.js'
import { obtenerConfiguracionEvaluacion } from '../services/configuracionService.js'

/** Mismo valor por defecto que el servidor (docentia.revision.umbral-promedio). */
export const UMBRAL_REVISION_PREDETERMINADO = 4

/** Nota final por debajo de la cual el docente queda «Para revisar», como la configuró el servidor. */
export function useUmbralRevision() {
  const { datos } = useFetch(obtenerConfiguracionEvaluacion)
  const umbral = Number(datos?.umbralRevision)
  return Number.isFinite(umbral) && umbral > 0 ? umbral : UMBRAL_REVISION_PREDETERMINADO
}

/** «4,0»: el umbral como se lee en pantalla. */
export function textoUmbral(umbral) {
  return Number(umbral).toFixed(1).replace('.', ',')
}

/** Solo un promedio completo se marca: uno parcial todavía puede cambiar. */
export function paraRevisar(promedio, umbral) {
  return promedio?.estado === 'COMPLETO' && promedio.valor != null && Number(promedio.valor) < umbral
}
