# Firma de actas: cómo funciona hoy y qué la reforzaría

Este documento revisa la firma de actas tal como está en el código. No es una opinión jurídica. Para saber si esta firma es suficiente para un trámite concreto hay que consultar a la oficina jurídica de la universidad.

## Qué es la firma en DOCENTIA

Es una **firma electrónica simple**. Una persona con sesión iniciada en DOCENTIA confirma que leyó el acta y deja una imagen de su firma. **No es una firma digital con certificado**: no usa una llave privada propia de quien firma ni la emite una entidad de certificación.

## Quién puede firmar y cuándo

- Firman **la coordinación y el docente** (`POST /actas/{id}/firmas`, roles COORDINADOR y DOCENTE). Cada uno firma una sola vez.
- Cada uno ve solo las actas que le corresponden: el docente, las suyas; la coordinación, las de su programa.
- Para firmar se exige todo esto:
  - La etapa FIRMA_ACTAS está abierta en el periodo.
  - El docente tiene una cita de retroalimentación activa.
  - La coordinación registró la asistencia, salvo que se desactive `exigir-asistencia`.
  - El promedio está completo con los tres evaluadores (estudiantes, coordinación y autoevaluación).
  - Si la nota está por debajo del umbral, el acta tiene al menos un compromiso con plazo.
- Hay que marcar una casilla para confirmar la lectura. Sin esa confirmación, el servidor rechaza la firma.

## Qué se guarda de cada firma

| Dato | Para qué sirve |
| --- | --- |
| Usuario, rol, nombre y correo | Saber quién firmó con su cuenta. |
| Fecha y hora del servidor | Saber cuándo se firmó. |
| Dirección IP de la petición | Rastro técnico de la firma. Detrás de un proxy puede ser la del proxy. |
| Imagen de la firma (PNG) y su SHA-256 | Mostrar la firma en el PDF y comprobar que la imagen guardada no cambió. |

La imagen se puede dibujar, escribir con el nombre o subir como archivo PNG. En los tres casos el servidor aplica las mismas reglas:

- Acepta solo un **PNG real**. Lo comprueba por la cabecera del archivo, no por su nombre ni por el tipo que declara. SVG, JPG y otros formatos se rechazan.
- El archivo pesa como máximo **300 KB** y mide como máximo **2000 px por lado**.
- La imagen debe tener trazo. Un lienzo en blanco no es una firma.
- El servidor no guarda el archivo recibido. Guarda **una copia recodificada**, solo con los píxeles y sin metadatos (autor, GPS, textos incrustados). El hash se calcula sobre esa copia.
- La imagen nunca se ejecuta ni se inserta como HTML. En la página se pinta sobre un lienzo y en el PDF se dibuja como imagen.
- La autorización es la misma en los tres modos: el mismo endpoint, los mismos roles y las mismas condiciones.

## Qué protege el acta firmada

- Con las dos firmas se genera **un único PDF**. El PDF lleva dos datos:
  - el **SHA-256 del contenido** del acta;
  - un **código de verificación público**, que se consulta en `/verificar-acta/{codigo}` sin iniciar sesión.
- Además se guarda aparte el SHA-256 del archivo PDF. Así se puede comprobar que el PDF entregado es el mismo que se generó (ver `DECISIONES.md`).
- Un **disparador en la base de datos** impide modificar o borrar un acta firmada.
- Para corregir un acta firmada, la vicerrectoría crea una versión nueva y explica el motivo. La versión anterior se conserva.
- Las acciones quedan en la **auditoría**, que tampoco se puede modificar.

## Límites que hay que conocer

- **La identidad depende de la cuenta.** Si alguien conoce la contraseña de otra persona, puede firmar en su nombre. Hoy no se pide un segundo factor ni volver a escribir la contraseña al firmar.
- **La imagen no prueba la autoría.** Una firma dibujada o subida es una representación visual. Lo que da el valor probatorio es el registro de la cuenta, la hora y el hash, no el parecido del trazo.
- **No hay sello de tiempo de un tercero.** La hora la pone el reloj del servidor de DOCENTIA.
- **El código de verificación y los hashes dependen de este sistema.** Si se pierde la base de datos, se pierde también la manera de verificar.
- **La IP puede no ser la real** si hay un proxy que no envía la IP original.
- Los administradores de la base de datos tienen acceso técnico. El disparador evita cambios por error, pero no protege frente a alguien con control total del servidor.

## Qué reforzaría la validez

En orden de esfuerzo:

1. **Pedir de nuevo la contraseña o un código de un solo uso por correo** justo antes de firmar.
2. **Segundo factor** en las cuentas de coordinación y vicerrectoría.
3. **Copias de seguridad** de la base de datos y de los PDF, fuera del servidor y con retención definida.
4. **Sello de tiempo** de una autoridad externa (RFC 3161) sobre el hash del PDF.
5. **Firmar el PDF con un certificado institucional** (PAdES), para que cualquier lector de PDF pueda verificarlo sin DOCENTIA.
6. **Firma digital con certificado personal** de cada firmante, emitido por una entidad de certificación, si jurídica lo exige.

Los puntos 4 a 6 requieren decisiones y presupuesto institucionales. Están **por confirmar**.
