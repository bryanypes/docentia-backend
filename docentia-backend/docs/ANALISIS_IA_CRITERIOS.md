# Criterios del análisis de comentarios

Cómo el modelo local convierte comentarios en temas, fortalezas, oportunidades y tendencias. Los
valores por defecto están en `docentia.ia.analisis.*`; cada resultado guarda los que usó en
`resultado.parametros`.

## De comentario a tema

1. **Normalización.** Se quitan tildes, mayúsculas y letras repetidas (`excelenteeee`) y se corrigen
   formas frecuentes de escribir (`xq`, `exelente`, `profe`), según la sección CORRECCION del léxico.
2. **Cláusulas.** El comentario se parte en frases y en `pero`, `aunque`, `sin embargo`, `en cambio`.
   "Explica bien pero las notas se demoran" da dos cláusulas con polaridades distintas.
3. **Polaridad.** Cada cláusula suma los términos POSITIVO y NEGATIVO del léxico con su peso:
   - una negación (`no`, `nunca`, `sin`, `poco`…) en las 3 palabras previas invierte el término;
   - un intensificador (`muy`, `demasiado`, `súper`…) multiplica su peso; `un poco` lo atenúa;
   - una expresión irónica (`sí claro`, `gracias por nada`) vuelve negativo lo positivo.
   Por encima de 0,25 es POSITIVO, por debajo de -0,25 NEGATIVO, en medio NEUTRO.
4. **Descriptor.** Los términos de cada descriptor (D02 a D12) ubican la cláusula en el Anexo 1. El
   criterio sale del descriptor. Una cláusula sin descriptor cuenta para el sentimiento, no para temas.

| Criterio | Descriptores |
|----------|--------------|
| C2 Ejecutar procesos de docencia | D02, D03, D04, D05, D08 |
| C3 Evaluar procesos de docencia | D06, D07 |
| C4 Sensibilidad social | D09, D10 |
| C5 Creatividad, dinamismo e innovación | D11, D12 |

C1 y C6 no aparecen: son descriptores de autoevaluación y coevaluación que un estudiante no observa.

**Tema** = (criterio, descriptor, polaridad). Un comentario cuenta una vez por tema.

**Sentimiento del comentario.** MIXTO si tiene cláusulas positivas y negativas; si no, la que tenga.

## Recurrencia y relevancia

- **Recurrente.** El tema aparece en al menos `max(3, 20 % de los comentarios útiles)` comentarios.
  Con 11 útiles el umbral es 3; con 40, 8.
- **Intensidad.** Promedio del peso absoluto de las cláusulas del tema.
- **Relevancia.** `frecuencia × (1 + intensidad)`. Ordena los temas.
- **Fortalezas y oportunidades.** Los temas recurrentes positivos y negativos, por relevancia, hasta 5
  de cada uno. Cada uno trae citas textuales verificadas.
- **Índice de sentimiento.** `(positivos − negativos) / útiles`, de -1 a 1.

## Tendencias

Contra el resultado vigente del periodo anterior del mismo docente, elegido por fecha de inicio.

| Cambio | Cuándo |
|--------|--------|
| NUEVO | El tema no estaba en el periodo anterior |
| SE_MANTIENE | La diferencia de porcentaje es menor a 10 puntos |
| EN_AUMENTO | Subió 10 puntos o más |
| EN_DISMINUCION | Bajó 10 puntos o más, o desapareció |
| SIN_REFERENCIA | No hay periodo anterior analizado, o tuvo menos de 5 comentarios útiles |

## Exclusiones

| Motivo | Qué es | Se cita |
|--------|--------|---------|
| VACIO, SIN_TEXTO | Vacío, solo espacios, emojis o puntuación | No |
| MUY_CORTO | Menos de 3 palabras | No |
| DUPLICADO | Copias idénticas más allá de 3 | No |
| INSTRUCCION_SOSPECHOSA | Intenta dar órdenes al modelo | No |
| INSULTO, APARIENCIA, EDAD, GENERO, VIDA_PRIVADA | Ofensivo o sobre la persona (E2) | No |
| ACOSO, AMENAZA, CONDUCTA_GRAVE | Posible situación grave | No |

Los tres últimos y las instrucciones generan una **advertencia** para coordinación con su cantidad. Las
situaciones graves no se resumen ni se citan: se revisan por el canal que defina la institución. Los
comentarios de más de 1.000 caracteres se recortan y se advierte `TRUNCADO`.

## Mínimos

- Menos de 10 encuestas (o respuestas del formulario): INSUFICIENTE, sin resultado.
- FORMULARIO exige además el mínimo de 5 respuestas de la heteroevaluación; como 10 es mayor, el
  mínimo efectivo del formulario es 10.
- Con menos de 5 comentarios útiles se advierte `POCOS_COMENTARIOS`.
- El resumen agregado necesita al menos 3 docentes con análisis.
