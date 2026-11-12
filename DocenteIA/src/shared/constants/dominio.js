export const ROLES = {
  VICERRECTOR: 'VICERRECTOR',
  COORDINADOR: 'COORDINADOR',
  DOCENTE: 'DOCENTE',
  ESTUDIANTE: 'ESTUDIANTE',
  ADMINISTRADOR: 'ADMINISTRADOR',
  VISUALIZADOR: 'VISUALIZADOR',
}

export const ETIQUETA_ROL = {
  VICERRECTOR: 'Vicerrectoría',
  COORDINADOR: 'Coordinación',
  DOCENTE: 'Docente',
  ESTUDIANTE: 'Estudiante',
  ADMINISTRADOR: 'Administración',
  VISUALIZADOR: 'Visualización',
}

export const ETIQUETA_FUENTE = {
  ESTUDIANTES: 'Estudiantes',
  COORDINACION: 'Coordinación',
  AUTOEVALUACION: 'Autoevaluación',
}

export const ORIGEN_FUENTE = {
  ESTUDIANTES: 'Formulario de los estudiantes',
  COORDINACION: 'Registrada por coordinación',
  AUTOEVALUACION: 'Registrada por el docente',
}

/** Fuente que registra cada rol en la evaluación de otros. La vicerrectoría no califica (Res. 074). */
export const FUENTE_DEL_ROL = {
  COORDINADOR: 'COORDINACION',
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

export const ETIQUETA_ORIGEN_MAPEO = {
  MANUAL: 'Mapeo manual',
  PLANTILLA: 'Plantilla',
  AUTOMATICO: 'Reconocido por nombre de columna',
}

/** Escala de la Resolución 074. Quien clasifica es el backend; esto solo pinta y arma la leyenda. */
export const NIVELES = [
  { codigo: 'MUY_ALTO', etiqueta: 'Muy alto', desde: 4.6, clase: 'muy-alto' },
  { codigo: 'ALTO', etiqueta: 'Alto', desde: 3.8, clase: 'alto' },
  { codigo: 'ACEPTABLE', etiqueta: 'Aceptable', desde: 3.0, clase: 'aceptable' },
  { codigo: 'BAJO', etiqueta: 'Bajo', desde: 2.2, clase: 'bajo' },
  { codigo: 'MUY_BAJO', etiqueta: 'Muy bajo', desde: 0, clase: 'muy-bajo' },
]
