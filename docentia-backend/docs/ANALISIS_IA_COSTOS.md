# Costos del análisis con el proveedor externo

Cuántos tokens gasta el análisis cuando se usa un modelo de lenguaje externo, y qué hace el sistema
para gastar poco. El modelo local por reglas, que es el de por defecto, no gasta tokens.

Se reportan tokens, no dinero. Si se configuran `docentia.ia.precio-millon-tokens-entrada` y
`-salida` (vacíos por defecto), el endpoint de uso calcula el costo; aquí no se inventan precios.

## Cómo se midió

`BancoCargaTest` genera 50, 200, 1.000, 2.000 y 5.000 comentarios **sintéticos** a partir del conjunto
de validación (con saludos, firmas y relleno para variar el largo; promedio cercano a 190 caracteres).

- **Antes**: la llamada como era hasta ahora: instrucción `analisis-v2`, todos los comentarios
  completos en `{"comentarios":[{"id","texto"}]}`, citas con texto y salida de hasta 4.096 tokens.
- **Después**: instrucción `analisis-v3`, muestra de hasta 150 comentarios recortados a 300
  caracteres en `{"t":[temas],"c":[[id,texto]]}`, citas por id y salida de hasta 4.000 tokens.
- **Real**: N = 50, 200 y 1.000 contra un proveedor compatible con chat/completions, con la clave del
  `.env` local, tope de 200.000 tokens y 10 llamadas. **Estimado**: N = 2.000 y 5.000, por caracteres
  (2,3 caracteres por token, calibrado con las llamadas reales).

## Antes y después por llamada

| N | Antes: entrada | Antes: salida | Después: comentarios enviados | Después: entrada | Después: salida | Ahorro de entrada |
|---|---|---|---|---|---|---|
| 50 | 4.662 real | 4.096 real, cortada en el tope | 50 | 4.180 real | 2.274 real, completa | 10 % |
| 200 | 16.939 real | 4.096 real, cortada en el tope | 150 | 10.615 real | ≈ 2.300 (\*) | 37 % |
| 1.000 | 81.840 real | 4.096 real, cortada en el tope | 150 | 10.307 real | ≈ 2.300 (\*) | 87 % |
| 2.000 | 165.967 estimado | ≥ 4.096 | 150 | 10.473 estimado | ≈ 2.300 | 94 % |
| 5.000 | 414.509 estimado (\*\*) | ≥ 4.096 | 150 | 10.257 estimado | ≈ 2.300 | 97 % |

(\*) En la corrida del banco con tope de salida de 1.500, las tres respuestas llegaron al tope y salieron
cortadas: el modelo probado gasta parte de la salida razonando antes de responder. Con 4.000 de tope
la respuesta de N = 50 llegó completa (2.274 tokens, 10 hallazgos, JSON válido). La salida no depende
de N, así que para 200 y 1.000 se toma ese valor.

(\*\*) La versión anterior ya cortaba en 2.000 comentarios por corrida; con ese corte, la entrada real
de 5.000 sería la de 2.000 y el ahorro, 94 %.

**Crecimiento.** Antes, la entrada crecía de forma lineal: unos 82 tokens por comentario. Después es
casi constante: por encima de 150 comentarios queda alrededor de 10.000 tokens, porque el modelo de
lenguaje ve solo la muestra y el modelo local cuenta todo. **Entrada y salida**: antes, con 1.000
comentarios, el 95 % era entrada; después, cerca del 80 % es entrada y el 20 % salida.

**Total por análisis con 1.000 comentarios**: antes, al menos 85.936 tokens con una respuesta cortada;
después, unos 12.600 con una respuesta completa. Ahorro de **85 %**.

## Caché

Antes de llamar se calcula la huella SHA-256 de lo que se enviaría: proveedor y modelo, versión de la
instrucción, topes, temas y comentarios (ya anonimizados y en orden de huella). Si existe una respuesta
con esa huella, se usa y no se llama. Ahorra el **100 %** de esa llamada.

- Pedir el mismo análisis otra vez, regenerarlo sin cambios o tener dos docentes con exactamente los
  mismos comentarios: cero tokens.
- Abrir pantallas, listar comentarios o ver el resumen: nunca llaman al proveedor; leen lo guardado.
- Un comentario nuevo, otro modelo, otra versión de la instrucción u otros topes: llamada nueva.

El endpoint `GET /analisis-comentarios/uso?periodo=` muestra el ahorro real: llamadas, aciertos de
caché, tokens y porcentajes de ahorro por caché y por topes.

## Topes y por qué esos valores

| Propiedad | Por defecto | Por qué |
|-----------|-------------|---------|
| `docentia.ia.max-comentarios-llm` | 150 | Con la muestra estratificada aparecen todos los criterios y polaridades del total en las pruebas, y la entrada queda en unos 10.000 tokens. Las métricas de validación con una muestra de 60 no bajan de los umbrales (con 40 sí bajan un poco; ver validación) |
| `docentia.ia.max-caracteres-comentario` | 300 | La mayoría de comentarios reales caben; los largos repiten ideas. El modelo local sigue leyendo hasta 1.000 |
| `docentia.ia.max-tokens-entrada` | 16.000 | Margen sobre los ~10.000 de 150 comentarios. Si no cabe, se achica la muestra, nunca la instrucción |
| `docentia.ia.max-tokens-salida` | 4.000 | Con 1.500 la respuesta del modelo probado salía cortada; con 4.000 llegó completa usando 2.274 |
| `docentia.ia.presupuesto-tokens-dia` | 500.000 | Unos 40 análisis nuevos de 1.000 comentarios por día. 0 lo desactiva |
| `docentia.ia.presupuesto-tokens-periodo` | 3.000.000 | Unos 240 análisis nuevos por periodo. 0 lo desactiva |
| `docentia.ia.enfriamiento-regeneracion-minutos` | 60 | Evita regenerar por impaciencia; la caché ya evita el gasto si nada cambió |
| `docentia.ia.periodos-referencia` | 1 (máximo 3) | Las tendencias usan solo agregados guardados; nunca se vuelven a leer ni a enviar comentarios viejos |

Los topes aplicados quedan en `resultado.limitesAplicados` para que la interfaz diga cuántos comentarios
vio el modelo de lenguaje.

## Caché de prompt del proveedor

La instrucción fija va primero y es idéntica en todas las llamadas, así que un proveedor con caché de
prompt puede reutilizarla. El sistema no depende de eso: en las llamadas medidas el proveedor informó
0 tokens en caché (la instrucción v3 es corta, unos 500 tokens). `uso_ia.tokens_cache` lo registra
cuando el proveedor lo informa.

## Tokens reales gastados en esta medición

| Corrida | Llamadas | Tokens |
|---------|----------|--------|
| Banco: antes y después para N = 50, 200 y 1.000 | 6 | 145.381 |
| Diagnóstico de la salida cortada (N = 50, tope 1.500) | 1 | 5.680 |
| Comprobación con tope 4.000 (N = 50) | 1 | 6.454 |
| Verificación con la muestra de 150 (de 1.000) y tope 4.000 | 1 | 12.343 |
| **Total** | **9** | **169.858** |

La verificación con 150 comentarios llegó completa (`finish_reason: stop`): 10.307 tokens de entrada,
2.036 de salida, 10 hallazgos (5 fortalezas y 5 oportunidades) con 38 citas, todas de comentarios
enviados, y un resumen de tres frases. Con 4.000 de tope sobra margen.

## Recomendación operativa

- **Modelo.** Seguir con el modelo local por reglas por defecto. Si se activa un proveedor externo, el
  modelo es configurable (`DOCENTIA_IA_MODELO`); antes de pasar a uno más pequeño, correr
  `ValidacionAnalisisTest` y `ProveedorRealOpcionalTest` y comparar. Un modelo que razona antes de
  responder necesita salida de 4.000.
- **Presupuesto.** Dejar los valores por defecto y revisar `GET /analisis-comentarios/uso` al final de
  cada recolección.
- **Cuándo regenerar.** Solo si cambiaron los comentarios (una carga corregida), el modelo o la
  instrucción. Regenerar sin cambios no gasta, pero tampoco aporta.
- **Si se agota el presupuesto.** Pedir el análisis con `"modelo": "LOCAL"`; nunca se cambia de modelo
  en silencio.
