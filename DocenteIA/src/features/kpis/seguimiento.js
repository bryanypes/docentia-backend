import { NIVELES } from '../../shared/constants/dominio.js'
import { nivelDeNota } from '../../shared/utils/formatters.js'

export const FUENTES = ['ESTUDIANTES', 'COORDINACION', 'VICERRECTORIA']
// Por debajo de "Bueno" un docente merece una conversación de seguimiento.
export const UMBRAL_ATENCION = 4.0

/**
 * Resume el seguimiento del periodo activo (GET /docentes/seguimiento) en lo que el tablero muestra.
 * @param agruparPor 'nombreFacultad' para la vicerrectoría, 'nombrePrograma' para la coordinación
 */
export function analizarSeguimiento(docentes, miFuente, agruparPor) {
  const total = docentes.length
  const completos = docentes.filter((d) => d.promedio?.estado === 'COMPLETO')
  const faltanPorFuente = Object.fromEntries(
    FUENTES.map((f) => [f, docentes.filter((d) => d.promedio?.fuentesPendientes?.includes(f)).length]),
  )

  // Rango legible de cada nivel, para la leyenda: "desde 4,5", "4 a 4,5", "menos de 3,5".
  const coma = (n) => String(n).replace('.', ',')
  const porNivel = NIVELES.map((nivel, i) => ({
    ...nivel,
    rango:
      i === 0
        ? `desde ${coma(nivel.desde)}`
        : nivel.desde === 0
          ? `menos de ${coma(NIVELES[i - 1].desde)}`
          : `${coma(nivel.desde)} a ${coma(NIVELES[i - 1].desde)}`,
    cantidad: completos.filter((d) => nivelDeNota(d.promedio.valor).clase === nivel.clase).length,
  }))

  const atencion = completos
    .filter((d) => Number(d.promedio.valor) < UMBRAL_ATENCION)
    .sort((a, b) => Number(a.promedio.valor) - Number(b.promedio.valor))

  const grupos = new Map()
  for (const d of completos) {
    const clave = d.docente[agruparPor]
    const grupo = grupos.get(clave) ?? { nombre: clave, suma: 0, cantidad: 0 }
    grupo.suma += Number(d.promedio.valor)
    grupo.cantidad += 1
    grupos.set(clave, grupo)
  }
  const porGrupo = [...grupos.values()]
    // Redondeo a dos decimales "hacia arriba en el medio", igual que el backend (4,325 → 4,33).
    .map((g) => ({ ...g, promedio: Math.round((g.suma / g.cantidad) * 100 + 1e-9) / 100 }))
    .sort((a, b) => b.promedio - a.promedio)

  return { total, completos: completos.length, faltanPorFuente, faltaMia: faltanPorFuente[miFuente] ?? 0, porNivel, atencion, porGrupo }
}
