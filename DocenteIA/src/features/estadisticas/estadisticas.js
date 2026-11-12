import { NIVELES } from '../../shared/constants/dominio.js'
import { nivelDeNota } from '../../shared/utils/formatters.js'
import { ORDEN_ESTADOS } from '../proceso/estados.js'

export const FUENTES = ['ESTUDIANTES', 'COORDINACION', 'AUTOEVALUACION']

/** Intervalos del histograma de promedios, de medio punto. */
export const INTERVALOS = [1, 1.5, 2, 2.5, 3, 3.5, 4, 4.5].map((desde) => ({
  desde,
  hasta: desde + 0.5,
  etiqueta: `${String(desde).replace('.', ',')}–${String(desde + 0.5).replace('.', ',')}`,
}))

const redondear = (valor, decimales = 2) =>
  Math.round(valor * 10 ** decimales + 1e-9) / 10 ** decimales

/**
 * Une el seguimiento (promedio de cada docente) con el estado de su proceso.
 * @returns [{ docente, promedio, proceso, nivel }]
 */
export function unirDatos(docentes = [], procesos = []) {
  const porDocente = new Map(procesos.map((p) => [p.docente.idDocente, p]))
  return docentes.map(({ docente, promedio }) => {
    const completo = promedio?.estado === 'COMPLETO'
    return {
      docente,
      promedio,
      proceso: porDocente.get(docente.idDocente) ?? null,
      nivel: completo ? (nivelDeNota(promedio.valor)?.codigo ?? null) : null,
    }
  })
}

/**
 * Filtros que comparte toda la vista: cada gráfica se recalcula con las mismas filas.
 * Un filtro vacío ('') no filtra.
 */
export function filtrar(filas, { facultad = '', programa = '', nivel = '', estado = '', busqueda = '' } = {}) {
  const termino = busqueda.trim().toLowerCase()
  return filas.filter(
    ({ docente, nivel: nivelFila, proceso }) =>
      (!facultad || String(docente.idFacultad) === String(facultad)) &&
      (!programa || docente.nombrePrograma === programa) &&
      (!nivel || nivelFila === nivel) &&
      (!estado || proceso?.estado === estado) &&
      (!termino || `${docente.nombreCompleto} ${docente.correo}`.toLowerCase().includes(termino)),
  )
}

/** Todas las cifras de la vista, a partir de las filas ya filtradas. */
export function calcular(filas, { umbral = 3.8, agruparPor = 'nombrePrograma' } = {}) {
  const total = filas.length
  const completos = filas.filter((f) => f.promedio?.estado === 'COMPLETO')
  const valores = completos.map((f) => Number(f.promedio.valor))
  const suma = valores.reduce((a, b) => a + b, 0)

  const cobertura = filas.reduce(
    (acc, f) => {
      const c = f.proceso?.coberturaEstudiantes
      if (c) {
        acc.respondidas += c.respondidas
        acc.esperadas += c.esperadas
      }
      return acc
    },
    { respondidas: 0, esperadas: 0 },
  )

  const porNivel = NIVELES.map((n) => {
    const cantidad = completos.filter((f) => f.nivel === n.codigo).length
    return { ...n, cantidad, porcentaje: completos.length ? Math.round((cantidad / completos.length) * 100) : 0 }
  })

  const porEstado = ORDEN_ESTADOS.map((clave) => ({
    estado: clave,
    cantidad: filas.filter((f) => f.proceso?.estado === clave).length,
  }))

  const pendientesPorFuente = FUENTES.map((fuente) => ({
    fuente,
    cantidad: filas.filter((f) => f.promedio?.fuentesPendientes?.includes(fuente)).length,
  }))

  const histograma = INTERVALOS.map((intervalo, i) => ({
    ...intervalo,
    cantidad: valores.filter((v) =>
      i === INTERVALOS.length - 1 ? v >= intervalo.desde && v <= 5 : v >= intervalo.desde && v < intervalo.hasta,
    ).length,
  }))
  // Las notas bajo 1 caen en el primer intervalo.
  histograma[0].cantidad += valores.filter((v) => v < 1).length

  const grupos = new Map()
  for (const f of filas) {
    const clave = f.docente[agruparPor]
    const g = grupos.get(clave) ?? { nombre: clave, docentes: 0, completos: 0, suma: 0, respondidas: 0, esperadas: 0 }
    g.docentes += 1
    if (f.promedio?.estado === 'COMPLETO') {
      g.completos += 1
      g.suma += Number(f.promedio.valor)
    }
    const c = f.proceso?.coberturaEstudiantes
    if (c) {
      g.respondidas += c.respondidas
      g.esperadas += c.esperadas
    }
    grupos.set(clave, g)
  }
  const porGrupo = [...grupos.values()]
    .map((g) => ({
      ...g,
      promedio: g.completos ? redondear(g.suma / g.completos) : null,
      cobertura: g.esperadas ? Math.round((g.respondidas / g.esperadas) * 100) : null,
      avance: Math.round((g.completos / g.docentes) * 100),
    }))
    .sort((a, b) => (b.promedio ?? -1) - (a.promedio ?? -1))

  const actas = filas.map((f) => f.proceso?.acta).filter(Boolean)
  const citas = filas.map((f) => f.proceso?.cita).filter(Boolean)

  return {
    total,
    completos: completos.length,
    avance: total ? Math.round((completos.length / total) * 100) : 0,
    promedioGeneral: valores.length ? redondear(suma / valores.length) : null,
    mediana: valores.length ? mediana(valores) : null,
    bajoUmbral: valores.filter((v) => v < umbral).length,
    umbral,
    cobertura: { ...cobertura, porcentaje: cobertura.esperadas ? Math.round((cobertura.respondidas / cobertura.esperadas) * 100) : null },
    porNivel,
    porEstado,
    pendientesPorFuente,
    histograma,
    porGrupo,
    actas: {
      total: actas.length,
      compromiso: actas.filter((a) => a.tipo === 'COMPROMISO').length,
      retroalimentacion: actas.filter((a) => a.tipo === 'RETROALIMENTACION').length,
      firmadas: actas.filter((a) => a.estado === 'FIRMADA').length,
      sinActa: filas.filter((f) => f.promedio?.estado === 'COMPLETO' && !f.proceso?.acta).length,
    },
    citas: {
      agendadas: citas.filter((c) => c.activa).length,
      confirmadas: citas.filter((c) => c.activa && c.confirmacion === 'CONFIRMADA').length,
      asistieron: citas.filter((c) => c.asistencia === 'ASISTIO').length,
      noAsistieron: filas.filter((f) => f.proceso?.estado === 'NO_ASISTIO').length,
    },
  }
}

function mediana(valores) {
  const orden = [...valores].sort((a, b) => a - b)
  const medio = Math.floor(orden.length / 2)
  return redondear(orden.length % 2 ? orden[medio] : (orden[medio - 1] + orden[medio]) / 2)
}

const formatoNota = (v) => Number(v).toFixed(2).replace('.', ',')

/**
 * Lectura en una frase de los datos visibles, para quien no interpreta gráficas. Solo describe lo
 * calculado; no agrega cálculos nuevos.
 */
export function lecturaEstadisticas(r) {
  if (!r || r.total === 0) return ''
  const docentes = r.total === 1 ? '1 docente' : `${r.total} docentes`
  const partes = [`De ${docentes}, ${r.completos} ${r.completos === 1 ? 'tiene' : 'tienen'} la evaluación completa (${r.avance} %).`]
  if (r.promedioGeneral !== null) {
    partes.push(`El promedio es ${formatoNota(r.promedioGeneral)} sobre 5.`)
    partes.push(
      r.bajoUmbral === 0
        ? `Nadie está bajo ${formatoNota(r.umbral)}.`
        : `${r.bajoUmbral} ${r.bajoUmbral === 1 ? 'queda' : 'quedan'} bajo ${formatoNota(r.umbral)} y ${r.bajoUmbral === 1 ? 'firmará' : 'firmarán'} acta de compromiso.`,
    )
  } else {
    partes.push('Aún no hay promedios: faltan calificaciones de alguna fuente.')
  }
  return partes.join(' ')
}
