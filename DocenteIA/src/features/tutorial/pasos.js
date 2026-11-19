import { ROLES } from '../../shared/constants/dominio.js'

/**
 * Recorrido guiado del primer inicio de sesión, por rol. Cada paso lleva a una página real y resalta
 * su enlace en el menú. El texto le habla al usuario en segunda persona.
 */
export const PASOS_POR_ROL = {
  [ROLES.COORDINADOR]: [
    {
      ruta: '/inicio',
      titulo: 'Tu página de inicio',
      texto:
        'Aquí ves de un vistazo cómo va el periodo en tu facultad: el avance de las evaluaciones, lo que está pendiente y las alertas.',
    },
    {
      ruta: '/analisis',
      titulo: 'Análisis de comentarios con IA',
      texto:
        'Aquí pides que la inteligencia artificial analice los comentarios de los estudiantes de cada docente (con al menos 5 comentarios). Con base en ese análisis puedes dar tu calificación: tu coevaluación a los docentes. Si llegan comentarios nuevos, te avisamos para que actualices el análisis.',
    },
    {
      ruta: '/coevaluaciones',
      titulo: 'Coevaluación de tus docentes',
      texto:
        'Aquí respondes la coevaluación de cada docente de tus programas. Se guarda como borrador y, cuando la envías, entra a su promedio.',
    },
    {
      ruta: '/docentes',
      titulo: 'Docentes y lo que dicen sus estudiantes',
      texto:
        'En el expediente de cada docente ves la nota de sus estudiantes, su autoevaluación, tu coevaluación, el proceso de la cita y el acta.',
    },
    {
      ruta: '/estadisticas',
      titulo: 'Estadísticas de tu facultad',
      texto:
        'Desempeño por programa, avance del proceso y, al final, lo que dicen los comentarios: sentimiento, criterios y temas más mencionados.',
    },
    {
      ruta: '/agenda',
      titulo: 'Agenda de citas',
      texto:
        'Abres horarios para las reuniones de retroalimentación, ves quién reservó y registras la asistencia el día de la reunión.',
    },
    {
      ruta: '/mi-evaluacion',
      titulo: 'Tu evaluación como docente',
      texto:
        'Tú también eres docente evaluado: aquí ves tu promedio, tu autoevaluación, tu cita con la vicerrectoría y tu acta.',
    },
  ],
  [ROLES.VICERRECTOR]: [
    {
      ruta: '/inicio',
      titulo: 'Tu página de inicio',
      texto: 'El estado de toda la universidad en el periodo: avance, alertas y lo que necesita tu atención.',
    },
    {
      ruta: '/estadisticas',
      titulo: 'Estadísticas institucionales',
      texto:
        'Desempeño por facultad y programa, avance del proceso y el resumen de lo que dicen los comentarios analizados con IA.',
    },
    {
      ruta: '/docentes',
      titulo: 'Docentes y coordinadores',
      texto: 'El expediente de cada docente. Los coordinadores también aparecen aquí: tú eres quien los evalúa.',
    },
    {
      ruta: '/analisis',
      titulo: 'Análisis de comentarios con IA',
      texto: 'Consulta el análisis con inteligencia artificial de los comentarios de los estudiantes de cada docente.',
    },
    {
      ruta: '/coevaluaciones',
      titulo: 'Coevaluación de coordinadores',
      texto:
        'Los coordinadores también son docentes: aquí les das tu coevaluación, que ocupa el lugar de la coordinación en su promedio.',
    },
    {
      ruta: '/agenda',
      titulo: 'Agenda con coordinadores',
      texto: 'Abres horarios que solo ven los coordinadores para su reunión de retroalimentación y firmas su acta.',
    },
    {
      ruta: '/periodos',
      titulo: 'Periodos y fechas',
      texto: 'Las etapas del periodo: recolección, revisión de hallazgos, agenda de citas y firma de actas.',
    },
  ],
  [ROLES.DOCENTE]: [
    {
      ruta: '/inicio',
      titulo: 'Tu evaluación',
      texto:
        'Aquí ves cómo va tu evaluación del periodo. Tu promedio aparece cuando están las tres notas: estudiantes, coordinación y autoevaluación.',
    },
    {
      ruta: '/mi-autoevaluacion',
      titulo: 'Tu autoevaluación',
      texto: 'Respondes el instrumento por criterios. Puedes guardar el borrador y enviarlo cuando estés listo.',
    },
    {
      ruta: '/mi-cita',
      titulo: 'Tu cita de retroalimentación',
      texto: 'Eliges un horario con la coordinación para la reunión donde conversan tu resultado.',
    },
    {
      ruta: '/mi-acta',
      titulo: 'Tu acta',
      texto: 'En la reunión firmas el acta con tus compromisos. Queda en PDF con un código de verificación.',
    },
  ],
  [ROLES.ESTUDIANTE]: [
    {
      ruta: '/inicio',
      titulo: 'Tus evaluaciones',
      texto:
        'Aquí aparecen los profesores de tus materias. Evalúa a cada uno; tus respuestas son anónimas y nadie sabe quién escribió cada comentario.',
    },
    {
      ruta: '/perfil',
      titulo: 'Tu perfil',
      texto: 'Tus datos y la seguridad de tu cuenta. Desde aquí también puedes ver este tutorial otra vez.',
    },
  ],
  [ROLES.ADMINISTRADOR]: [
    {
      ruta: '/usuarios',
      titulo: 'Usuarios y roles',
      texto: 'Creas y editas usuarios, y ves dónde está cada persona: por facultad, por rol o por programa.',
    },
    { ruta: '/programas', titulo: 'Programas', texto: 'El catálogo de facultades y programas académicos.' },
    {
      ruta: '/ponderaciones',
      titulo: 'Ponderaciones',
      texto: 'Cuánto pesa cada evaluador en el promedio: estudiantes, coordinación y autoevaluación.',
    },
    { ruta: '/periodos', titulo: 'Periodos', texto: 'Las fechas de cada etapa del proceso de evaluación.' },
    {
      ruta: '/solicitudes-correccion',
      titulo: 'Solicitudes de corrección',
      texto:
        'Cuando alguien necesita corregir una evaluación ya enviada y su superior lo aprueba, te llega aquí y por correo. Tú reabres el formulario para que la corrija.',
    },
  ],
  [ROLES.VISUALIZADOR]: [
    {
      ruta: '/panel-programas',
      titulo: 'Panel por programa',
      texto: 'Los resultados agregados de cada programa, sin datos de docentes individuales.',
    },
  ],
}

/** Segundos que dura cada paso cuando avanza solo, como un video. */
export const SEGUNDOS_POR_PASO = 12
