import { NIVELES } from '../../shared/constants/dominio.js'
import { nivelDeNota } from '../../shared/utils/formatters.js'

export const FUENTES = ['ESTUDIANTES', 'COORDINACION', 'AUTOEVALUACION']

/**
 * Resume el seguimiento del periodo activo (GET /docentes/seguimiento) en lo que el tablero muestra.
 * @param agruparPor 'nombreFacultad' para la vicerrectoría, 'nombrePrograma' para la coordinación
 * @param umbral nota bajo la cual el acta exige compromisos (docentia.compromisos.umbral)
 * @param umbralDestacado nota desde la cual un docente es destacado (docentia.kpi.destacado-umbral);
 *     sin él, la mención de honor toma a los que están en el umbral de compromisos o por encima
 */
export function analizarSeguimiento(docentes, miFuente, agruparPor, umbral, umbralDestacado = null) {
  const total = docentes.length
  const completos = docentes.filter((d) => d.promedio?.estado === 'COMPLETO')
  const faltanPorFuente = Object.fromEntries(
    FUENTES.map((f) => [f, docentes.filter((d) => d.promedio?.fuentesPendientes?.includes(f)).length]),
  )

  // Rango legible de cada nivel, para la leyenda: "desde 4,6", "3,8 a 4,6", "menos de 2,2".
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
    .filter((d) => Number(d.promedio.valor) < umbral)
    .sort((a, b) => Number(a.promedio.valor) - Number(b.promedio.valor))

  // Mención de honor: las mejores notas, desde el umbral de destacados (o el de compromisos).
  const pisoHonor = umbralDestacado ?? umbral
  const honor = completos
    .filter((d) => Number(d.promedio.valor) >= pisoHonor)
    .sort((a, b) => Number(b.promedio.valor) - Number(a.promedio.valor))

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

  return {
    total,
    umbral,
    pisoHonor,
    completos: completos.length,
    faltanPorFuente,
    faltaMia: faltanPorFuente[miFuente] ?? 0,
    porNivel,
    atencion,
    honor,
    porGrupo,
  }
}
