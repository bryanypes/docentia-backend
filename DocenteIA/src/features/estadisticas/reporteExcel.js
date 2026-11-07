import { ETIQUETA_FUENTE, NIVELES } from '../../shared/constants/dominio.js'
import { formatearFecha } from '../../shared/utils/formatters.js'
import { formatearHora } from '../agenda/franjas.js'
import { ESTADOS_PROCESO, ETIQUETA_ASISTENCIA, ETIQUETA_CONFIRMACION, ETIQUETA_TIPO_ACTA } from '../proceso/estados.js'

// La hoja Docentes lleva título en la fila 1, encabezado en la 3 y datos desde la 4.
const PRIMERA_FILA = 4

const COLUMNAS_DOCENTES = [
  { titulo: 'Docente', ancho: 30 },
  { titulo: 'Correo', ancho: 30 },
  { titulo: 'Facultad', ancho: 28 },
  { titulo: 'Programa', ancho: 30 },
  { titulo: 'Promedio', ancho: 11, formato: 'nota' },
  { titulo: 'Desempeño', ancho: 16 },
  { titulo: 'Evaluación', ancho: 13 },
  { titulo: 'Calificaciones que faltan', ancho: 28 },
  { titulo: 'Proceso', ancho: 26 },
  { titulo: 'Estudiantes que respondieron', ancho: 14, formato: 'entero' },
  { titulo: 'Matrículas', ancho: 12, formato: 'entero' },
  { titulo: 'Respuesta de estudiantes', ancho: 14, formato: 'porcentaje' },
  { titulo: 'Cita', ancho: 24 },
  { titulo: 'Confirmación', ancho: 20 },
  { titulo: 'Asistencia', ancho: 13 },
  { titulo: 'Tipo de acta', ancho: 26 },
  { titulo: 'Estado del acta', ancho: 16 },
]

const etiquetaNivel = (codigo) => NIVELES.find((n) => n.codigo === codigo)?.etiqueta ?? ''

function filaDocente({ docente, promedio, proceso, nivel }, numero) {
  const completo = promedio?.estado === 'COMPLETO'
  const cobertura = proceso?.coberturaEstudiantes
  const cita = proceso?.cita
  const acta = proceso?.acta
  return [
    docente.nombreCompleto,
    docente.correo,
    docente.nombreFacultad,
    docente.nombrePrograma,
    completo ? Number(promedio.valor) : null,
    etiquetaNivel(nivel),
    completo ? 'Completa' : 'Incompleta',
    (promedio?.fuentesPendientes ?? []).map((f) => ETIQUETA_FUENTE[f] ?? f).join(', '),
    proceso ? (ESTADOS_PROCESO[proceso.estado]?.etiqueta ?? proceso.estado) : '',
    cobertura?.respondidas ?? null,
    cobertura?.esperadas ?? null,
    { formula: `IF(N(K${numero})>0,J${numero}/K${numero},"")` },
    cita ? `${formatearFecha(cita.fecha)}, ${formatearHora(cita.horaInicio)}` : '',
    cita?.confirmacion ? (ETIQUETA_CONFIRMACION[cita.confirmacion] ?? cita.confirmacion) : cita ? 'Sin responder' : '',
    cita?.asistencia ? (ETIQUETA_ASISTENCIA[cita.asistencia] ?? cita.asistencia) : '',
    acta ? (ETIQUETA_TIPO_ACTA[acta.tipo] ?? acta.tipo ?? '') : '',
    acta?.estado ? acta.estado.charAt(0) + acta.estado.slice(1).toLowerCase().replaceAll('_', ' ') : '',
  ]
}

/**
 * Hojas del reporte de estadísticas. El resumen usa SUBTOTAL sobre la hoja Docentes, así que al
 * filtrar esa hoja en Excel las cifras del resumen se recalculan solas.
 *
 * @param filas filas ya filtradas en la vista (unirDatos + filtrar)
 * @param r     resultado de calcular(filas)
 */
export function hojasDelReporte({ filas, r, periodo, descripcionFiltros, nombreGrupo }) {
  const ultima = Math.max(PRIMERA_FILA, PRIMERA_FILA + filas.length - 1)
  const rango = (columna) => `Docentes!${columna}${PRIMERA_FILA}:${columna}${ultima}`

  const resumen = {
    nombre: 'Resumen',
    titulo: `Estadísticas de la evaluación docente · ${periodo ?? 'periodo activo'}`,
    columnas: [
      { titulo: 'Indicador', ancho: 42 },
      { titulo: 'Valor', ancho: 14 },
      { titulo: 'Cómo se calcula', ancho: 70 },
    ],
    filas: [
      ['Filtros de la vista al descargar', descripcionFiltros || 'Ninguno', ''],
      ['Docentes visibles', { formula: `SUBTOTAL(103,${rango('A')})`, formato: 'entero' }, 'Cambia al filtrar la hoja Docentes'],
      ['Con evaluación completa', { formula: `SUBTOTAL(102,${rango('E')})`, formato: 'entero' }, 'Docentes con las tres fuentes'],
      [
        'Avance',
        { formula: `IFERROR(SUBTOTAL(102,${rango('E')})/SUBTOTAL(103,${rango('A')}),"")`, formato: 'porcentaje' },
        'Completos sobre visibles',
      ],
      ['Promedio general', { formula: `IFERROR(SUBTOTAL(101,${rango('E')}),"")`, formato: 'nota' }, 'Promedio ponderado 50/30/20 de los completos'],
      ['Promedio más alto', { formula: `IFERROR(SUBTOTAL(104,${rango('E')}),"")`, formato: 'nota' }, ''],
      ['Promedio más bajo', { formula: `IFERROR(SUBTOTAL(105,${rango('E')}),"")`, formato: 'nota' }, ''],
      ['Mediana (al descargar)', { valor: r.mediana, formato: 'nota' }, 'Valor fijo de la selección descargada'],
      [
        `Docentes bajo ${String(r.umbral).replace('.', ',')}`,
        { formula: `COUNTIF(${rango('E')},"<${r.umbral}")`, formato: 'entero' },
        'Su acta es de compromiso (todas las filas, sin importar el filtro)',
      ],
      [
        'Respuesta de estudiantes',
        { formula: `IFERROR(SUBTOTAL(109,${rango('J')})/SUBTOTAL(109,${rango('K')}),"")`, formato: 'porcentaje' },
        'Respondieron sobre matrículas',
      ],
      ['Reuniones realizadas', { valor: r.citas.asistieron, formato: 'entero' }, 'Citas con asistencia registrada'],
      ['Citas activas', { valor: r.citas.agendadas, formato: 'entero' }, `${r.citas.confirmadas} confirmadas`],
      ['Actas firmadas', { valor: r.actas.firmadas, formato: 'entero' }, `${r.actas.compromiso} de compromiso, ${r.actas.retroalimentacion} de retroalimentación`],
      ['Completos sin acta', { valor: r.actas.sinActa, formato: 'entero' }, ''],
    ],
  }

  const docentes = {
    nombre: 'Docentes',
    titulo: 'Docentes de la selección · usa los filtros del encabezado',
    columnas: COLUMNAS_DOCENTES,
    filas: filas.map((fila, i) => filaDocente(fila, PRIMERA_FILA + i)),
  }

  // Las cantidades por nivel y por proceso se cuentan con fórmulas sobre la hoja Docentes.
  const nivel = {
    nombre: 'Desempeño',
    columnas: [
      { titulo: 'Nivel', ancho: 18 },
      { titulo: 'Desde', ancho: 10, formato: 'nota' },
      { titulo: 'Docentes', ancho: 12, formato: 'entero' },
      { titulo: 'Porcentaje', ancho: 12, formato: 'porcentaje' },
    ],
    filas: NIVELES.map((n, i) => [
      n.etiqueta,
      n.desde,
      { formula: `COUNTIF(${rango('F')},A${i + 2})` },
      { formula: `IFERROR(C${i + 2}/SUM(C$2:C$${NIVELES.length + 1}),"")` },
    ]),
  }

  const proceso = {
    nombre: 'Proceso',
    columnas: [
      { titulo: 'Paso', ancho: 30 },
      { titulo: 'Docentes', ancho: 12, formato: 'entero' },
      { titulo: 'Qué significa', ancho: 70 },
    ],
    filas: r.porEstado.map((e, i) => [
      ESTADOS_PROCESO[e.estado].etiqueta,
      { formula: `COUNTIF(${rango('I')},A${i + 2})` },
      ESTADOS_PROCESO[e.estado].ayuda ?? '',
    ]),
  }

  const grupo = {
    nombre: nombreGrupo === 'facultad' ? 'Por facultad' : 'Por programa',
    columnas: [
      { titulo: nombreGrupo === 'facultad' ? 'Facultad' : 'Programa', ancho: 34 },
      { titulo: 'Docentes', ancho: 11, formato: 'entero' },
      { titulo: 'Completos', ancho: 11, formato: 'entero' },
      { titulo: 'Promedio', ancho: 11, formato: 'nota' },
      { titulo: 'Avance', ancho: 11, formato: 'porcentaje' },
      { titulo: 'Respondieron', ancho: 13, formato: 'entero' },
      { titulo: 'Matrículas', ancho: 12, formato: 'entero' },
      { titulo: 'Respuesta de estudiantes', ancho: 14, formato: 'porcentaje' },
    ],
    filas: r.porGrupo.map((g, i) => [
      g.nombre,
      g.docentes,
      g.completos,
      g.promedio,
      { formula: `IFERROR(C${i + 2}/B${i + 2},"")` },
      g.respondidas,
      g.esperadas,
      { formula: `IF(G${i + 2}>0,F${i + 2}/G${i + 2},"")` },
    ]),
  }

  const distribucion = {
    nombre: 'Distribución',
    columnas: [
      { titulo: 'Tramo de promedio', ancho: 18 },
      { titulo: 'Desde', ancho: 10, formato: 'nota' },
      { titulo: 'Hasta', ancho: 10, formato: 'nota' },
      { titulo: 'Docentes', ancho: 12, formato: 'entero' },
    ],
    filas: r.histograma.map((h) => [h.etiqueta, h.desde, h.hasta, h.cantidad]),
  }

  const fuentes = {
    nombre: 'Calificaciones que faltan',
    columnas: [
      { titulo: 'Fuente', ancho: 24 },
      { titulo: 'Docentes sin ella', ancho: 16, formato: 'entero' },
    ],
    filas: r.pendientesPorFuente.map((p) => [ETIQUETA_FUENTE[p.fuente] ?? p.fuente, p.cantidad]),
  }

  return [resumen, docentes, nivel, proceso, grupo, distribucion, fuentes]
}

/** Nombre del archivo: estadisticas-docentia-2026-2-2026-11-03.xlsx */
export function nombreDelReporte(periodo, hoy = new Date().toISOString().slice(0, 10)) {
  const limpio = String(periodo ?? 'periodo')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[^A-Za-z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
    .toLowerCase()
  return `estadisticas-docentia-${limpio}-${hoy}.xlsx`
}
