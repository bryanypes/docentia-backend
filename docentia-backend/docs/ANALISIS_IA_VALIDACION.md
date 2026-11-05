# Validación del modelo local

> **Datos sintéticos.** Los 90 comentarios de `src/test/resources/ia/validacion-comentarios.json` los
> escribió el equipo; ninguno es de un estudiante real. El léxico y el conjunto los escribió la misma
> persona, así que estas cifras **sobreestiman** lo que pasará con comentarios reales. Sirven como piso
> de regresión, no como medida de calidad. Antes de usar el análisis en una decisión, hay que repetir
> la medición con una muestra real etiquetada por coordinación.

## Cómo se mide

`ValidacionAnalisisTest` corre el mismo pipeline del flujo asíncrono sobre el conjunto etiquetado:

- **Por criterio**: precisión, exhaustividad y F1 de las etiquetas (criterio presente o no en cada
  comentario útil).
- **Polaridad**: exactitud del sentimiento del comentario.
- **Exclusiones**: comentarios que debían excluirse y se excluyeron por el motivo esperado.

Los umbrales de la prueba son las cifras de la primera corrida real, redondeadas hacia abajo: si una
regla nueva las baja, la prueba falla.

## Resultados (léxico `lexico-2026.11-v1`, reglas `reglas-1.0`)

| Criterio | Precisión | Exhaustividad | F1 | Soporte |
|----------|-----------|---------------|----|---------|
| C2 Ejecutar | 0,94 | 1,00 | 0,97 | 34 |
| C3 Evaluar | 0,85 | 1,00 | 0,92 | 11 |
| C4 Sensibilidad social | 1,00 | 1,00 | 1,00 | 10 |
| C5 Creatividad | 1,00 | 1,00 | 1,00 | 15 |

| Medida | Valor |
|--------|-------|
| Comentarios útiles evaluados | 73 |
| Exactitud de polaridad | 0,89 |
| Exclusiones correctas | 17 de 17 |
| 2.000 comentarios | menos de 1 s en un portátil (650 a 810 ms en tres corridas) |

## Con la muestra del modelo de lenguaje

`conLaMuestraDelModeloDeLenguajeLasMetricasNoBajanNiFaltanCriterios` mide los mismos comentarios
etiquetados, pero solo los que entrarían en la muestra. Con el tope real (150) entra todo el conjunto
(73 útiles) y las cifras son las de arriba; para que de verdad se muestree se usa un tope menor.

| Muestra | C2 | C3 | C4 | C5 | Polaridad |
|---------|----|----|----|----|-----------|
| Todos (73) | 0,97 | 0,92 | 1,00 | 1,00 | 0,89 |
| 60 (la prueba) | 0,98 | 0,89 | 1,00 | 1,00 | 0,87 |
| 40 | 0,97 | 0,86 | 1,00 | 1,00 | 0,83 |

Con 40, C3 y la polaridad quedan un punto por debajo del umbral de regresión: el muestreo
estratificado le da más peso a los casos raros, que son los difíciles. Todos los criterios presentes
en el total aparecen en la muestra.

## Otras propiedades probadas

| Propiedad | Prueba |
|-----------|--------|
| Toda cita aparece textual en un comentario analizado y las frecuencias cuadran | `todaCitaApareceTextualmenteYLasFrecuenciasCuadran` |
| Ningún correo, teléfono, documento, enlace o nombre llega a la salida | `ningunDatoPersonalLlegaALaSalida` |
| Un intento de inyección no cambia el resultado | `losIntentosDeInyeccionNoCambianElResultado` |
| Mismo resultado con la entrada en otro orden | `dosCorridasIgualesDanLoMismoAunqueCambieElOrdenDeEntrada` |
| Con pocos comentarios advierte y no inventa temas | `conPocosComentariosLoAdvierteSinInventarTemas` |
| Formulario: 409 con la recolección abierta | `AnalisisComentariosTest.elFormularioSeAnalizaSoloConLaRecoleccionCerrada` |
| Formulario: la copia no tiene fechas, ids de respuesta ni orden de llegada | `laCopiaAnalizadaNoTieneFechasNiIdsDeRespuestaNiOrdenDeLlegada` |
| Formulario: el orden de llegada no cambia el resultado | `elOrdenDeLlegadaNoCambiaElResultado` |
| El registro no lleva texto de comentarios | `elAnalisisDelFormularioCompletaConElModeloLocalYQuedaPendienteDeValidacion` |

## Proveedor real (opcional)

`ProveedorRealOpcionalTest` solo corre si el ambiente trae `DOCENTIA_IA_URL_BASE`,
`DOCENTIA_IA_MODELO` y `DOCENTIA_IA_CLAVE`. Envía 5 comentarios sintéticos, uno con una orden, y
comprueba que los hallazgos tengan citas verificables, que no se apoyen en el comentario con la orden
y que la oportunidad de mejora no desaparezca. No guarda ni imprime la respuesta.

Se corrió una vez en local con un proveedor compatible con chat/completions y pasó. Hizo falta
`DOCENTIA_IA_ENVIAR_TEMPERATURA=false` porque ese modelo rechaza el parámetro. En CI se omite.

## Cómo repetirlo

```bash
./mvnw test -Dtest=ValidacionAnalisisTest      # imprime las líneas METRICA
```

Para medir con datos reales: etiquetar una muestra en el mismo formato, reemplazar el archivo solo en
la copia local y correr la prueba. Ese archivo no se sube: los comentarios reales no se versionan.
