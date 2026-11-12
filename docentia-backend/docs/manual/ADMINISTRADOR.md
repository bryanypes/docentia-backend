# Manual de la administración

La administración da de alta a las personas, mantiene los programas, fija las ponderaciones y ajusta las fechas de los periodos. No ve notas ni expedientes.

## Primer ingreso en un servidor nuevo

El primer administrador lo crea el despliegue con las variables `DOCENTIA_BOOTSTRAP_*` (ver `OPERACION.md`). Entra con la contraseña temporal y DOCENTIA le pide cambiarla. Ese administrador da de alta a los demás.

## Menú

| Entrada | Para qué |
|---|---|
| Usuarios | Crear, editar, activar o desactivar cuentas y asignar su rol |
| Programas | Programas académicos y su facultad |
| Ponderaciones | Peso de cada evaluador en el promedio del periodo activo |
| Periodos | Crear periodos, sus etapas y, si hace falta, ajustar el nombre calculado |

## Dar de alta a una persona

1. En **Usuarios** pulsa **Nuevo usuario** y escribe el nombre y el correo `@uniautonoma.edu.co`.
2. Elige el rol. Cada persona tiene un solo rol:
   - **Coordinación**: se vincula a una facultad y ve los programas de esa facultad.
   - **Docente** y **estudiante**: se vinculan a un programa.
   - **Vicerrectoría**, **administración** y **visualización**: no se vinculan, ven toda la universidad.
3. Guarda. La persona recibe una contraseña temporal que debe cambiar al entrar, o entra con Google.

## Ponderaciones

Los pesos por defecto son 50 % estudiantes, 30 % coordinación y 20 % autoevaluación. Deben sumar 100 %. Cambiarlos recalcula los promedios del periodo activo.

## Periodos

El nombre del periodo se calcula desde la fecha de inicio (AAAA-1 o AAAA-2). Solo la administración puede escribir otro, y solo con ese formato y sin repetir uno existente.
