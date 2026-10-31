export const ROLES = {
  VICERRECTOR: 'VICERRECTOR',
  COORDINADOR: 'COORDINADOR',
  DOCENTE: 'DOCENTE',
}

export const ETIQUETA_ROL = {
  VICERRECTOR: 'Vicerrectoría',
  COORDINADOR: 'Coordinación',
  DOCENTE: 'Docente',
}

export const ETIQUETA_FUENTE = {
  ESTUDIANTES: 'Estudiantes',
  COORDINACION: 'Coordinación',
  VICERRECTORIA: 'Vicerrectoría',
}

export const ORIGEN_FUENTE = {
  ESTUDIANTES: 'Sincronizado del sistema de encuestas',
  COORDINACION: 'Registrada por coordinación',
  VICERRECTORIA: 'Registrada por vicerrectoría',
}

/** Fuente con la que califica cada rol. El docente no califica. */
export const FUENTE_DEL_ROL = {
  COORDINADOR: 'COORDINACION',
  VICERRECTOR: 'VICERRECTORIA',
}

/** Los cinco aspectos que se califican, en el orden y con los nombres del backend. */
export const ASPECTOS = [
  {
    campo: 'dominioDisciplinar',
    nombre: 'Dominio disciplinar',
    guia: 'Dominio temático y actualización en su área de conocimiento.',
  },
  {
    campo: 'planeacionAcademica',
    nombre: 'Planeación académica',
    guia: 'Syllabus actualizado, objetivos claros y secuencia coherente.',
  },
  {
    campo: 'metodologiaEnsenanza',
    nombre: 'Metodología de enseñanza',
    guia: 'Estrategias didácticas, recursos y uso del tiempo de clase.',
  },
  {
    campo: 'evaluacionAprendizaje',
    nombre: 'Evaluación del aprendizaje',
    guia: 'Instrumentos pertinentes y retroalimentación oportuna.',
  },
  {
    campo: 'compromisoInstitucional',
    nombre: 'Compromiso institucional',
    guia: 'Cumplimiento de horarios, reuniones y reportes.',
  },
]

/** Las cuatro etapas del periodo (TipoEtapa), en el orden en que llegan de la API. */
export const ETAPAS = {
  RECOLECCION_CALIFICACIONES: 'RECOLECCION_CALIFICACIONES',
  REVISION_HALLAZGOS: 'REVISION_HALLAZGOS',
  AGENDA_CITAS: 'AGENDA_CITAS',
  FIRMA_ACTAS: 'FIRMA_ACTAS',
}

/** Campos que reconoce la carga del CSV de estudiantes (CampoImportacion del backend). */
export const CAMPOS_IMPORTACION = [
  { campo: 'CORREO_DOCENTE', nombre: 'Correo del docente', obligatorio: true },
  { campo: 'DOMINIO_DISCIPLINAR', nombre: 'Dominio disciplinar', obligatorio: true },
  { campo: 'PLANEACION_ACADEMICA', nombre: 'Planeación académica', obligatorio: true },
  { campo: 'METODOLOGIA_ENSENANZA', nombre: 'Metodología de enseñanza', obligatorio: true },
  { campo: 'EVALUACION_APRENDIZAJE', nombre: 'Evaluación del aprendizaje', obligatorio: true },
  { campo: 'COMPROMISO_INSTITUCIONAL', nombre: 'Compromiso institucional', obligatorio: true },
  { campo: 'COMENTARIO', nombre: 'Comentario', obligatorio: false },
]

export const ETIQUETA_ORIGEN_MAPEO = {
  MANUAL: 'Mapeo manual',
  PLANTILLA: 'Plantilla',
  AUTOMATICO: 'Reconocido por nombre de columna',
}

/** Niveles de desempeño del maquetado. El color siempre va acompañado de la etiqueta. */
export const NIVELES = [
  { etiqueta: 'Excelente', desde: 4.5, clase: 'excelente' },
  { etiqueta: 'Bueno', desde: 4.0, clase: 'bueno' },
  { etiqueta: 'Aceptable', desde: 3.5, clase: 'aceptable' },
  { etiqueta: 'Bajo', desde: 0, clase: 'bajo' },
]
