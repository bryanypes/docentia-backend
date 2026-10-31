# Seguridad

## Modelo de amenazas

| Actor | Puede | Amenaza principal |
|-------|-------|-------------------|
| Docente | Ver lo propio | Ver notas por fuente de su evaluación o datos de un colega |
| Coordinador | Gestionar su facultad | Salir de su alcance hacia otra facultad |
| Vicerrector | Consultar toda la institución | Sobreexposición de datos individuales |
| Sin sesión | Iniciar sesión y verificar un código | Fuerza bruta, enumeración de usuarios |

**Datos sensibles:** notas por fuente, promedio, categoría, comentarios de estudiantes, hallazgos y
actas. La regla A4 impide que el docente vea notas individuales de cada fuente o un promedio parcial.

## Controles

| Riesgo | Control | Evidencia |
|--------|---------|-----------|
| Acceso indebido | Autorización por método y alcance por facultad | `MatrizAutorizacionTest`, `AlcanceRecursosTest` |
| Token forjado | HS256 explícito; secreto de 32 bytes o más; `prod` rechaza secretos de desarrollo | `SeguridadHttpTest`, `ValidacionProduccion` |
| Fuerza bruta | 5 fallos por correo y 30 por IP en 10 minutos, respuesta 429 genérica | `SeguridadHttpTest` |
| Enumeración | Mismo mensaje y siempre se compara un hash BCrypt | `AutenticacionService` |
| Fuga en errores | RFC 7807 sin trazas, con `correlationId` | `SeguridadHttpTest` |
| Fuga por correo | Textos fijos por tipo | `NotificacionesTest` |
| Clickjacking y MIME | CSP, `X-Frame-Options`, `nosniff`, `Referrer-Policy` en API y nginx | `SeguridadHttpTest`, `nginx.conf` |
| Superficie operativa | Actuator expone solo `health` e `info`; no hay Swagger | `SeguridadHttpTest` |
| Secretos | Solo por entorno; gitleaks en CI | `.github/workflows/ci.yml` |
| CSV malicioso | Límite de tamaño, filas, tipo y codificación | `ImportacionTest` |

No hay exportación a CSV en el sistema. Por eso no aplica la protección contra inyección de fórmulas;
si se agrega una exportación, debe prefijar `'` a los valores que empiecen por `=`, `+`, `-` o `@`.

## Riesgos aceptados

| Riesgo | Motivo | Mitigación futura |
|--------|--------|-------------------|
| El límite de intentos vive en memoria | Una sola instancia en la demo | Redis en producción |
| CVE-2026-47884, Spring MVC `XsltView` | La corrección solo existe en 7.0.9 y exige cambiar de versión mayor. El proyecto no usa `XsltView` | Migrar a Spring Boot 4 |
| CVE-2026-47890, SSE con fragmentos de vista | Igual que el anterior. El proyecto no usa SSE | Migrar a Spring Boot 4 |
| JWT sin revocación | Vigencia de 8 horas | Lista de revocación |
| `style-src 'unsafe-inline'` en nginx | Lo requiere Google Identity | Nonce o retirar Google |

## Datos personales

La Ley 1581 de 2012 orienta la minimización y la finalidad del tratamiento. El sistema las aplica así:
guarda solo los datos de la evaluación, anonimiza comentarios antes del análisis, deja la IA externa
apagada por defecto y no incluye datos de evaluación en los correos. Este documento describe controles
técnicos; no afirma cumplimiento legal, que debe revisar la institución.
