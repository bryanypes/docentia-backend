const assert = require('node:assert/strict')
const { after, before, test } = require('node:test')
const { chromium } = require('playwright')

const BASE = process.env.BASE_URL ?? 'http://localhost:5173'
const CLAVE = 'Docentia2026*'
const COORDINACION = 'coordinacion.ingenieria'
const DOCENTE = 'nora.munoz'

let navegador

/** Hoy en Colombia, como lo calcula el backend (America/Bogota). */
function hoy() {
  return new Date().toLocaleDateString('en-CA', { timeZone: 'America/Bogota' })
}

function sumarDias(fecha, dias) {
  const d = new Date(`${fecha}T12:00:00Z`)
  d.setUTCDate(d.getUTCDate() + dias)
  return d.toISOString().slice(0, 10)
}

async function entrar(usuario) {
  const contexto = await navegador.newContext({ acceptDownloads: true })
  const pagina = await contexto.newPage()
  pagina.setDefaultTimeout(20000)
  await pagina.goto(`${BASE}/login`)
  await pagina.fill('input[type=email]', `${usuario}@uniautonoma.edu.co`)
  await pagina.fill('input[type=password]', CLAVE)
  await pagina.click('button[type=submit]')
  await pagina.waitForURL((url) => !url.pathname.includes('login'))
  return pagina
}

/** Llama a la API con el token de la pestaña, para lo que la pantalla no permite intentar. */
function llamar(pagina, metodo, ruta, cuerpo) {
  return pagina.evaluate(
    async ({ metodo, ruta, cuerpo }) => {
      const respuesta = await fetch(`/api${ruta}`, {
        method: metodo,
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${sessionStorage.getItem('docentia.token')}`,
        },
        body: cuerpo ? JSON.stringify(cuerpo) : undefined,
      })
      return { estado: respuesta.status, datos: await respuesta.json().catch(() => null) }
    },
    { metodo, ruta, cuerpo },
  )
}

async function abrirExpediente(pagina, nombre) {
  await pagina.click('a[href="/docentes"]')
  // El listado va paginado: se busca por nombre para no depender de la página en que quede.
  await pagina.getByPlaceholder('Buscar docente, correo o programa').fill(nombre)
  await pagina.getByText(nombre).first().click()
  await pagina.getByRole('heading', { name: 'Acta de la reunión' }).waitFor()
}

/** Firma en el modal: modo «Escribir», confirmación de lectura y envío. */
async function firmarEnModal(pagina, nombre) {
  await pagina.getByRole('button', { name: 'Firmar acta' }).first().click()
  const modal = pagina.getByRole('dialog')
  await modal.getByRole('button', { name: 'Escribir' }).click()
  await modal.getByLabel('Tu nombre como firma').fill(nombre)
  await modal.getByRole('checkbox').check()
  await modal.getByRole('button', { name: 'Firmar acta' }).click()
  await modal.waitFor({ state: 'detached' })
}

/**
 * Responde todos los pasos del cuestionario con el mismo valor y llega a la revisión. Cada paso
 * entra con una transición: se hace clic en la tarjeta de la opción (el radio está oculto) y se
 * espera a que el título del paso cambie antes de seguir.
 */
async function responderTodo(pagina, valor) {
  const respondidos = new Set()
  for (let paso = 0; paso < 20; paso += 1) {
    const titulo = await pagina.locator('[data-titulo-paso]').last().innerText()
    for (;;) {
      const opcion = pagina
        .locator(`.cuestionario__contenido:last-child label.pregunta__opcion--${valor}`)
        .filter({ has: pagina.locator('input:not(:checked)') })
        .first()
      if ((await opcion.count()) === 0) break
      const nombre = await opcion.locator('input').getAttribute('name')
      await opcion.click()
      await pagina.locator(`input[name="${nombre}"][value="${valor}"]:checked`).waitFor({ state: 'attached' })
      respondidos.add(nombre)
    }
    const siguiente = pagina.getByRole('button', { name: /^(Siguiente|Revisar)$/ })
    if ((await siguiente.count()) === 0) break
    await siguiente.click()
    await pagina.waitForFunction((anterior) => {
      const titulos = [...document.querySelectorAll('[data-titulo-paso]')]
      return titulos.length === 1 && titulos[0].innerText !== anterior
    }, titulo)
  }
  return respondidos.size
}

before(async () => {
  navegador = await chromium.launch()
  // La semilla trae fechas fijas; se ajustan las cuatro etapas para que estén abiertas hoy.
  const pagina = await entrar('vicerrectoria')
  const periodo = (await llamar(pagina, 'GET', '/periodos-academicos/activo')).datos
  const apertura = sumarDias(hoy(), -7) < periodo.fechaInicio ? periodo.fechaInicio : sumarDias(hoy(), -7)
  const etapas = ['RECOLECCION_CALIFICACIONES', 'REVISION_HALLAZGOS', 'AGENDA_CITAS', 'FIRMA_ACTAS'].map(
    (tipoEtapa) => ({ tipoEtapa, fechaApertura: apertura, fechaCierre: periodo.fechaFin }),
  )
  const cambio = await llamar(pagina, 'PUT', `/periodos-academicos/${periodo.idPeriodo}`, {
    nombre: periodo.nombre,
    fechaInicio: periodo.fechaInicio,
    fechaFin: periodo.fechaFin,
    etapas,
  })
  assert.equal(cambio.estado, 200, JSON.stringify(cambio.datos))
  await pagina.context().close()
})

after(async () => {
  await navegador.close()
})

let idActa

test('la coordinación ve el expediente con categoría, genera el acta y abre un horario', async () => {
  const pagina = await entrar(COORDINACION)
  await abrirExpediente(pagina, 'Nora')
  await pagina.getByText('Bajo desempeño').first().waitFor()
  await pagina.getByText(/50\s%\sestudiantes/).waitFor()

  await pagina.getByRole('button', { name: 'Generar acta' }).click()
  await pagina.getByText('Nota menor a 3,80').waitFor()
  await pagina.getByRole('button', { name: 'Agregar compromiso' }).click()
  const modal = pagina.getByRole('dialog')
  await modal.getByLabel('Compromiso').fill('Actualizar el syllabus')
  await modal.getByLabel('Plazo', { exact: true }).fill('2027-03-30')
  await modal.getByRole('button', { name: 'Guardar compromiso' }).click()
  await pagina.getByText(/Actualizar el syllabus/).first().waitFor()

  // Franja de hoy: la asistencia solo se registra el día de la cita.
  const franja = await llamar(pagina, 'POST', '/franjas', { fecha: hoy(), horaInicio: '23:00', horaFin: '23:30' })
  assert.equal(franja.estado, 201, JSON.stringify(franja.datos))
  await pagina.context().close()
})

test('el docente reserva la cita y no ve el acta antes de la reunión', async () => {
  const pagina = await entrar(DOCENTE)
  await pagina.click('a[href="/mi-cita"]')
  await pagina.getByRole('button', { name: /^Reservar / }).first().click()
  await pagina.getByRole('dialog').getByRole('button', { name: 'Reservar', exact: true }).click()
  await pagina.getByText(/Cita reservada/).waitFor()

  const firma = await llamar(pagina, 'GET', '/actas')
  const borrador = firma.datos.find((a) => a.estado !== 'ANULADA')
  if (borrador) {
    const intento = await llamar(pagina, 'POST', `/actas/${borrador.idActa}/firmas`, { confirmaLectura: true })
    assert.notEqual(intento.estado, 200)
  }
  await pagina.context().close()
})

test('la coordinación registra la asistencia y firma el acta', async () => {
  const pagina = await entrar(COORDINACION)
  await abrirExpediente(pagina, 'Nora')
  await pagina.getByRole('button', { name: 'Asistió · habilitar resultado' }).click()
  await pagina.getByText('Habilitado', { exact: true }).waitFor()
  await firmarEnModal(pagina, 'Coordinación Ingeniería')
  await pagina.getByText(/Coordinación: /).first().waitFor()
  await pagina.context().close()
})

test('el docente firma su acta en la reunión', async () => {
  const pagina = await entrar(DOCENTE)
  await pagina.click('a[href="/mi-acta"]')
  await firmarEnModal(pagina, 'Nora Patricia Muñoz')
  await pagina.getByText('Firmada', { exact: true }).first().waitFor()
  const actas = await llamar(pagina, 'GET', '/actas')
  idActa = actas.datos[0].idActa
  await pagina.context().close()
})

test('la coordinación descarga el PDF y no puede editar el acta firmada', async () => {
  const pagina = await entrar(COORDINACION)
  await abrirExpediente(pagina, 'Nora')
  const [descarga] = await Promise.all([
    pagina.waitForEvent('download'),
    pagina.getByRole('button', { name: 'Descargar PDF' }).click(),
  ])
  assert.match(descarga.suggestedFilename(), /\.pdf$/)

  assert.equal(await pagina.getByRole('button', { name: 'Agregar compromiso' }).count(), 0)
  const edicion = await llamar(pagina, 'POST', `/actas/${idActa}/compromisos`, {
    descripcion: 'Intento',
    fechaLimite: '2027-03-30',
  })
  assert.equal(edicion.estado, 409)
  await pagina.context().close()
})

test('la vicerrectoría corrige con una versión nueva y queda en la auditoría', async () => {
  const pagina = await entrar('vicerrectoria')
  await abrirExpediente(pagina, 'Nora')
  await pagina.getByLabel('Motivo de la corrección').fill('Error de digitación')
  await pagina.getByRole('button', { name: 'Corregir con una versión nueva' }).click()
  // La corrección es irreversible: la pantalla pide confirmarla en un diálogo.
  await pagina.getByRole('dialog').getByRole('button', { name: 'Corregir acta' }).click()
  await pagina.getByText(/versión 2/).first().waitFor()
  await pagina.getByText('Historial de versiones (1)').waitFor()

  // La auditoría ya no tiene pantalla, pero el registro se conserva.
  const auditoria = await llamar(pagina, 'GET', '/auditoria?accion=CORRECCION_ACTA')
  assert.equal(auditoria.estado, 200)
  assert.ok(auditoria.datos.contenido.length > 0)
  await pagina.context().close()
})

test('la coordinación revisa y publica los hallazgos, ocultos al docente hasta su reunión', async () => {
  const pagina = await entrar(COORDINACION)
  await pagina.click('a[href="/analisis"]')
  const valor = await pagina
    .locator('#docenteAnalisis option', { hasText: 'Julián' })
    .getAttribute('value')
  await pagina.selectOption('#docenteAnalisis', valor)
  await pagina.getByRole('button', { name: 'Solicitar análisis' }).click()
  await pagina.getByText('Borrador del análisis').waitFor({ timeout: 45000 })

  const hallazgos = pagina.locator('.analisis__hallazgo')
  const cantidad = await hallazgos.count()
  await hallazgos.first().getByRole('button', { name: 'Editar texto' }).click()
  await pagina.getByLabel('Texto del hallazgo').fill('Redacción revisada por coordinación')
  await pagina.getByRole('button', { name: 'Guardar texto' }).click()
  await pagina.getByText('Redacción revisada por coordinación').first().waitFor()
  await hallazgos.first().getByRole('button', { name: 'Marcar como válido' }).click()
  await hallazgos.first().getByText('Validado').waitFor()
  for (let i = 1; i < cantidad; i += 1) {
    await hallazgos.nth(i).getByRole('button', { name: 'Descartar' }).click()
    await hallazgos.nth(i).getByRole('button', { name: 'Confirmar descarte' }).click()
    await hallazgos.nth(i).getByText('Descartado').waitFor()
  }
  await pagina.getByRole('button', { name: 'Publicar revisión' }).click()
  await pagina.getByText('Revisión publicada').waitFor()
  await pagina.context().close()

  // Julián no ha tenido su reunión: lo publicado no le llega todavía.
  const docente = await entrar('julian.hoyos')
  const propio = await llamar(docente, 'GET', '/analisis-ia/resultados/propio')
  assert.ok(!JSON.stringify(propio.datos ?? '').includes('Redacción revisada por coordinación'))
  await docente.context().close()
})

test('el panel por programa no identifica docentes', async () => {
  const pagina = await entrar(COORDINACION)
  await pagina.click('a[href="/panel-programas"]')
  await pagina.getByText(/Cobertura/).first().waitFor()
  const texto = await pagina.locator('.pagina').innerText()
  assert.ok(!texto.includes('Nora'))
  await pagina.context().close()
})

test('las notificaciones llegan a la campana y se leen', async () => {
  const pagina = await entrar(DOCENTE)
  const campana = pagina.getByRole('button', { name: /Notificaciones: \d+ sin leer/ })
  await campana.waitFor()
  await campana.click()
  await pagina.getByRole('link', { name: 'Ver todas' }).click()
  await pagina.getByRole('heading', { name: 'Notificaciones', level: 1 }).waitFor()
  await pagina.getByRole('button', { name: 'Marcar todas como leídas' }).click()
  await pagina.getByRole('button', { name: 'Notificaciones', exact: true }).waitFor()
  await pagina.context().close()
})

async function enviarFormulario(pagina) {
  await pagina.getByRole('checkbox').check()
  await pagina.getByRole('button', { name: 'Enviar formulario' }).click()
  await pagina.getByText('Formulario enviado').waitFor()
}

test('los formularios cambian la nota y el docente no ve la coevaluación', async () => {
  const docente = await entrar('ramiro.paz')
  await docente.click('a[href="/mi-autoevaluacion"]')
  await docente.getByText('0 de 14 ítems respondidos').waitFor()
  assert.equal(await responderTodo(docente, 5), 14)
  await docente.getByText('14 de 14 ítems respondidos').waitFor()
  await enviarFormulario(docente)
  await docente.context().close()

  const coordinacion = await entrar(COORDINACION)
  await abrirExpediente(coordinacion, 'Ramiro')
  // 1,90 en estudiantes y coevaluación con 5,00 en autoevaluación: 2,52.
  await coordinacion.getByText('2,52').first().waitFor()
  await coordinacion.getByText('Bajo desempeño', { exact: true }).first().waitFor()
  await coordinacion.click('a[href="/coevaluaciones"]')
  // La lista va paginada: se busca al docente por nombre.
  await coordinacion.getByPlaceholder('Buscar docente o programa').fill('Ramiro')
  await coordinacion
    .locator('tr', { hasText: 'Ramiro Antonio Paz' })
    .locator('a[href^="/coevaluaciones/"]')
    .click()
  await coordinacion.getByText('Coevaluación de Ramiro Antonio Paz').first().waitFor()
  assert.equal(await responderTodo(coordinacion, 5), 6)
  await enviarFormulario(coordinacion)
  await abrirExpediente(coordinacion, 'Ramiro')
  // Con la coevaluación en 5,00 sube a 3,45.
  await coordinacion.getByText('3,45').first().waitFor()
  await coordinacion.getByText('Desempeño aceptable').first().waitFor()
  await coordinacion.context().close()

  const otraVez = await entrar('ramiro.paz')
  const propio = await llamar(otraVez, 'GET', '/expedientes/propio')
  assert.equal(propio.datos.notasPorFuente, undefined)
  const sesion = await llamar(otraVez, 'GET', '/sesiones/actual')
  const ajeno = await llamar(otraVez, 'GET', `/formularios/coevaluacion/${sesion.datos.idUsuario}`)
  assert.equal(ajeno.estado, 403)
  await otraVez.context().close()
})

test('el estudiante evalúa de forma anónima y la nota cambia al llegar al mínimo', async () => {
  // Laura tiene 6 estudiantes matriculados y 4 respuestas: con la quinta se alcanza el mínimo.
  const coordinacion = await entrar(COORDINACION)
  await abrirExpediente(coordinacion, 'Laura Demo')
  await coordinacion.getByText('3,50').first().waitFor()
  await coordinacion.context().close()

  const estudiante = await entrar('estudiante.demo6')
  await estudiante.getByRole('heading', { name: 'Mis evaluaciones' }).first().waitFor()
  await estudiante.getByRole('link', { name: 'Evaluar a Laura Demo Estudiantes' }).click()
  await estudiante.getByText('0 de 11 ítems respondidos').waitFor()
  await estudiante.getByText('Tus respuestas son anónimas', { exact: true }).first().waitFor()
  assert.equal(await responderTodo(estudiante, 5), 11)
  await estudiante.getByText('11 de 11 ítems respondidos').waitFor()
  await estudiante.locator('.instrumento__comentario textarea').fill('Explica con ejemplos claros y responde las dudas con paciencia.')
  await estudiante.getByRole('checkbox').check()
  await estudiante.getByRole('button', { name: 'Enviar evaluación' }).click()
  await estudiante.getByText('¡Gracias por tu evaluación!').waitFor()
  const lista = await llamar(estudiante, 'GET', '/estudiante/evaluaciones')
  const asignacion = lista.datos.evaluaciones[0].idAsignacion
  const respuestas = Object.fromEntries(
    ['D02', 'D03', 'D04', 'D05', 'D06', 'D07', 'D08', 'D09', 'D10', 'D11', 'D12'].map((c) => [c, 5]),
  )
  const doble = await llamar(estudiante, 'POST', `/estudiante/evaluaciones/${asignacion}/enviar`, {
    respuestas,
    comentario: 'Segundo envío del mismo estudiante, que debe rechazarse.',
  })
  assert.equal(doble.estado, 409)
  assert.equal(lista.datos.evaluaciones[0].estado, 'ENVIADA')
  await estudiante.context().close()

  const otraVez = await entrar(COORDINACION)
  await abrirExpediente(otraVez, 'Laura Demo')
  // Con el quinto envío los estudiantes pasan de 3,00 (nota inicial de la semilla) a 5,00 por formulario: 4,50.
  await otraVez.getByText('4,50').first().waitFor()
  await otraVez.getByText('Alto desempeño').first().waitFor()
  await otraVez.context().close()

  const docente = await entrar('laura.demo')
  assert.equal((await llamar(docente, 'GET', '/cobertura-heteroevaluacion')).estado, 403)
  assert.equal((await llamar(docente, 'GET', '/estudiante/evaluaciones')).estado, 403)
  const propio = await llamar(docente, 'GET', '/expedientes/propio')
  assert.equal(propio.datos.notasPorFuente, undefined)
  await docente.context().close()
})

test('el análisis lee los comentarios del formulario cuando cierra la recolección y se conserva', async () => {
  const coordinacion = await entrar(COORDINACION)
  const docentes = await llamar(coordinacion, 'GET', '/docentes?size=500')
  const mariana = (docentes.datos.content ?? docentes.datos).find((d) => d.nombreCompleto === 'Mariana Ruiz Ortega')
  const activo = await llamar(coordinacion, 'GET', '/periodos-academicos/activo')
  const idPeriodo = activo.datos.idPeriodo
  const pedido = { docenteId: mariana.idDocente, periodoId: idPeriodo, fuente: 'FORMULARIO' }

  // Con la recolección abierta no se analiza, y el mensaje lo dice.
  const antes = await llamar(coordinacion, 'POST', '/analisis-ia/solicitudes', pedido)
  assert.equal(antes.estado, 409)
  assert.match(antes.datos.mensaje, /La recolección aún no termina/)

  // La vicerrectoría cierra la recolección ayer.
  const vicerrectoria = await entrar('vicerrectoria')
  const periodo = await llamar(vicerrectoria, 'GET', `/periodos-academicos/${idPeriodo}`)
  const etapas = periodo.datos.etapas.map(({ tipoEtapa, fechaApertura, fechaCierre }) => ({
    tipoEtapa,
    fechaApertura,
    fechaCierre: tipoEtapa === 'RECOLECCION_CALIFICACIONES' ? sumarDias(hoy(), -1) : fechaCierre,
  }))
  const cierre = await llamar(vicerrectoria, 'PUT', `/periodos-academicos/${idPeriodo}`, {
    fechaInicio: periodo.datos.fechaInicio,
    fechaFin: periodo.datos.fechaFin,
    etapas,
  })
  assert.equal(cierre.estado, 200, JSON.stringify(cierre.datos))
  await vicerrectoria.context().close()

  // Desde la pantalla, con el modelo local por defecto.
  await coordinacion.goto(`${BASE}/analisis?docente=${mariana.idDocente}&periodo=${idPeriodo}`)
  await coordinacion.getByText('Se analizan los comentarios del formulario anónimo').waitFor()
  await coordinacion.getByRole('button', { name: 'Solicitar análisis' }).click()
  await coordinacion.getByText(/Fortalezas/).first().waitFor({ timeout: 60000 })

  // Al salir y volver el resultado sigue ahí; pedir otro ya es regenerar.
  await coordinacion.goto(`${BASE}/inicio`)
  await coordinacion.goto(`${BASE}/analisis?docente=${mariana.idDocente}&periodo=${idPeriodo}`)
  await coordinacion.getByText(/Fortalezas/).first().waitFor()
  await coordinacion.getByRole('button', { name: 'Regenerar análisis' }).waitFor()
  await coordinacion.context().close()
})
