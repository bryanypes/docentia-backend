const assert = require('node:assert/strict')
const { after, before, test } = require('node:test')
const { chromium } = require('playwright')

const BASE = process.env.BASE_URL ?? 'http://localhost:5173'
const CLAVE = 'Docentia2026*'
const COORDINACION = 'coordinacion.ingenieria'
const DOCENTE = 'nora.munoz'

let navegador

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
  await pagina.getByText(nombre).first().click()
  await pagina.getByRole('heading', { name: 'Acta de retroalimentación' }).waitFor()
}

before(async () => {
  navegador = await chromium.launch()
})

after(async () => {
  await navegador.close()
})

let idActa

test('la coordinación ve el expediente con categoría y genera el acta', async () => {
  const pagina = await entrar(COORDINACION)
  await abrirExpediente(pagina, 'Nora')
  await pagina.getByText('Bajo desempeño').first().waitFor()
  await pagina.getByText(/50\s%\sestudiantes/).waitFor()

  await pagina.getByRole('button', { name: 'Generar acta' }).click()
  await pagina.getByText('Nota menor a 3,80').waitFor()
  await pagina.getByLabel('Compromiso').fill('Actualizar el syllabus')
  await pagina.getByLabel('Plazo').fill('2027-03-30')
  await pagina.getByRole('button', { name: 'Agregar compromiso' }).click()
  await pagina.getByText(/Actualizar el syllabus/).first().waitFor()

  const manana = new Date(Date.now() + 2 * 864e5).toISOString().slice(0, 10)
  const franja = await llamar(pagina, 'POST', '/franjas', { fecha: manana, horaInicio: '10:00', horaFin: '10:30' })
  assert.equal(franja.estado, 201)
  await pagina.context().close()
})

test('el docente reserva la cita y firma', async () => {
  const pagina = await entrar(DOCENTE)
  await pagina.click('a[href="/mi-cita"]')
  await pagina.getByRole('button', { name: /Reservar/ }).first().click()
  await pagina.getByText(/Cita reservada/).waitFor()

  await pagina.click('a[href="/mi-acta"]')
  await pagina.getByRole('checkbox').check()
  await pagina.getByRole('button', { name: 'Firmar acta' }).click()
  await pagina.getByText(/Docente: Nora/).waitFor()
  const actas = await llamar(pagina, 'GET', '/actas')
  idActa = actas.datos[0].idActa
  await pagina.context().close()
})

test('la coordinación firma, descarga el PDF y no puede editar el acta firmada', async () => {
  const pagina = await entrar(COORDINACION)
  await abrirExpediente(pagina, 'Nora')
  await pagina.getByRole('checkbox').check()
  await pagina.getByRole('button', { name: 'Firmar acta' }).click()
  const [descarga] = await Promise.all([
    pagina.waitForEvent('download'),
    pagina.getByRole('button', { name: 'Descargar PDF' }).click(),
  ])
  assert.match(descarga.suggestedFilename(), /^acta-DOC-.*\.pdf$/)

  assert.equal(await pagina.getByRole('button', { name: 'Agregar compromiso' }).count(), 0)
  const edicion = await llamar(pagina, 'POST', `/actas/${idActa}/compromisos`, {
    descripcion: 'Intento',
    fechaLimite: '2027-03-30',
  })
  assert.equal(edicion.estado, 409)
  await pagina.context().close()
})

test('la vicerrectoría corrige con una versión nueva y ve la auditoría', async () => {
  const pagina = await entrar('vicerrectoria')
  await abrirExpediente(pagina, 'Nora')
  await pagina.getByLabel('Motivo de la corrección').fill('Error de digitación')
  await pagina.getByRole('button', { name: /Corregir/ }).click()
  await pagina.getByText(/versión 2/).waitFor()
  await pagina.getByText('Historial de versiones (1)').waitFor()

  await pagina.click('a[href="/auditoria"]')
  await pagina.getByRole('cell', { name: 'Corrección de acta' }).first().waitFor()
  await pagina.context().close()
})

test('la coordinación revisa y publica los hallazgos y el docente los ve', async () => {
  const pagina = await entrar(COORDINACION)
  await pagina.click('a[href="/analisis"]')
  const valor = await pagina
    .locator('#docenteAnalisis option', { hasText: 'Julián' })
    .getAttribute('value')
  await pagina.selectOption('#docenteAnalisis', valor)
  await pagina.getByRole('button', { name: /Solicitar/ }).click()
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

  const docente = await entrar('julian.hoyos')
  await docente.getByText('Redacción revisada por coordinación').waitFor()
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
  await pagina.getByRole('button', { name: /Acta/ }).first().click()
  await pagina.getByRole('heading', { name: 'Mi acta de retroalimentación' }).waitFor()

  await pagina.getByRole('button', { name: /Notificaciones/ }).click()
  await pagina.getByRole('link', { name: 'Ver todas' }).click()
  await pagina.getByRole('heading', { name: 'Notificaciones', level: 1 }).waitFor()
  await pagina.getByRole('button', { name: 'Marcar todas como leídas' }).click()
  await pagina.getByRole('button', { name: 'Notificaciones', exact: true }).waitFor()
  await pagina.context().close()
})

async function responderTodo(pagina, valor) {
  const opciones = pagina.locator(`input[type=radio][value="${valor}"]`)
  const cantidad = await opciones.count()
  for (let i = 0; i < cantidad; i += 1) await opciones.nth(i).check()
  return cantidad
}

async function enviarFormulario(pagina) {
  await pagina.getByRole('checkbox').check()
  await pagina.getByRole('button', { name: 'Enviar formulario' }).click()
  await pagina.getByText('Formulario enviado').waitFor()
}

test('los formularios cambian la nota y el docente no ve la coevaluación', async () => {
  const docente = await entrar('ramiro.paz')
  await docente.click('a[href="/mi-autoevaluacion"]')
  await docente.getByRole('group', { name: 'Sensibilidad social' }).waitFor()
  assert.equal(await responderTodo(docente, 5), 14)
  await docente.getByText('14 de 14 ítems respondidos').waitFor()
  await enviarFormulario(docente)
  await docente.context().close()

  const coordinacion = await entrar(COORDINACION)
  await abrirExpediente(coordinacion, 'Ramiro')
  await coordinacion.getByText('Muy bajo desempeño').first().waitFor()
  await coordinacion.click('a[href="/coevaluaciones"]')
  await coordinacion.getByRole('link', { name: /Ramiro/ }).click()
  await coordinacion.getByRole('group', { name: /Sensibilidad/ }).waitFor({ state: 'detached', timeout: 2000 }).catch(() => {})
  assert.equal(await responderTodo(coordinacion, 5), 6)
  await enviarFormulario(coordinacion)
  await abrirExpediente(coordinacion, 'Ramiro')
  await coordinacion.getByText('Muy bajo desempeño').first().waitFor({ state: 'detached' })
  await coordinacion.context().close()

  const otraVez = await entrar('ramiro.paz')
  const propio = await llamar(otraVez, 'GET', '/expedientes/propio')
  assert.equal(propio.datos.notasPorFuente, undefined)
  const sesion = await llamar(otraVez, 'GET', '/sesiones/actual')
  const ajeno = await llamar(otraVez, 'GET', `/formularios/coevaluacion/${sesion.datos.idUsuario}`)
  assert.equal(ajeno.estado, 403)
  await otraVez.getByRole('heading', { name: 'Mi autoevaluación por criterio' }).waitFor()
  await otraVez.context().close()
})
