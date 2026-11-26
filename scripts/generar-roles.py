#!/usr/bin/env python3
"""Genera docentia-backend/docs/ROLES.md desde el código.

- El menú por rol sale de DocenteIA/src/shared/components/layout/Sidebar/Sidebar.jsx.
- La API por rol sale de la matriz de MatrizAutorizacionTest, que falla si una ruta del backend no
  está clasificada o si un rol recibe algo distinto de lo que dice la matriz.

Uso, desde la raíz del repositorio:

    python3 scripts/generar-roles.py            # reescribe ROLES.md
    python3 scripts/generar-roles.py --revisar  # sale con 1 si ROLES.md no está al día
"""

import re
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
MATRIZ = RAIZ / "docentia-backend/src/test/java/com/uniautonoma/docentia/controller/MatrizAutorizacionTest.java"
MENU = RAIZ / "DocenteIA/src/shared/components/layout/Sidebar/Sidebar.jsx"
DESTINO = RAIZ / "docentia-backend/docs/ROLES.md"

# Letra de la matriz, constante de ROLES en el frontend y nombre en el documento, en este orden.
ROLES = [
    ("E", "ESTUDIANTE", "Estudiante"),
    ("D", "DOCENTE", "Docente"),
    ("C", "COORDINADOR", "Coordinación"),
    ("V", "VICERRECTOR", "Vicerrectoría"),
    ("A", "ADMINISTRADOR", "Administración"),
    ("L", "VISUALIZADOR", "Visualización"),
]

ENCABEZADO = """# Roles y permisos

Generado desde el código con `scripts/generar-roles.py`: el menú sale de `Sidebar.jsx` y la API de
`MatrizAutorizacionTest`, que falla si una ruta del backend no está clasificada o si un rol recibe algo
distinto de lo que dice esta tabla. Para cambiar un permiso hay que cambiar el `@PreAuthorize` y la
matriz de la prueba, y volver a generar este archivo.

Retirado en 0.4.0: la carga de evaluaciones por CSV (la API `/importaciones` ya no existe) y las
pantallas de matrícula, auditoría y facultades. Las API `/auditoria` y `/facultades` siguen sin pantalla.
"""


def leer_matriz():
    texto = MATRIZ.read_text(encoding="utf-8")
    filas = re.findall(r'\{"((?:GET|POST|PUT|PATCH|DELETE) [^"]+)", "([A-Z]+)"\}', texto)
    if not filas:
        sys.exit("No encontré filas en MatrizAutorizacionTest")
    return filas


def quien(letras):
    if letras == "P":
        return "Público (sin sesión)"
    return ", ".join(nombre for letra, _, nombre in ROLES if letra in letras)


def leer_menu():
    texto = MENU.read_text(encoding="utf-8")

    def etiquetas(bloque):
        return re.findall(r"etiqueta: '([^']+)'", bloque)

    # Secciones definidas como constantes (por ejemplo COMO_DOCENTE) para resolverlas por nombre.
    constantes = {}
    for nombre, cuerpo in re.findall(r"const ([A-Z_]+) = \{(.*?)\n\}", texto, re.S):
        titulo = re.search(r"titulo: '([^']+)'", cuerpo)
        constantes[nombre] = (titulo.group(1) if titulo else None, etiquetas(cuerpo))

    navegacion = texto[texto.index("const NAVEGACION") :]
    menu = {}
    posiciones = [(m.start(), m.group(1)) for m in re.finditer(r"\[ROLES\.([A-Z]+)\]:", navegacion)]
    for i, (inicio, rol) in enumerate(posiciones):
        fin = posiciones[i + 1][0] if i + 1 < len(posiciones) else navegacion.index("\n}\n")
        bloque = navegacion[inicio:fin]
        partes = []
        # Primera sección: los enlaces sin título.
        principal = re.split(r"\n\s*(?:\{\s*\n\s*titulo:|[A-Z_]+,)", bloque)[0]
        partes.append(", ".join(etiquetas(principal)))
        for titulo, cuerpo in re.findall(r"titulo: '([^']+)',\s*enlaces: \[(.*?)\]", bloque, re.S):
            partes.append(f"{titulo}: {', '.join(etiquetas(cuerpo))}")
        for nombre in re.findall(r"\n\s*([A-Z_]+),", bloque):
            if nombre in constantes:
                titulo, enlaces = constantes[nombre]
                partes.append(f"{titulo}: {', '.join(enlaces)}" if titulo else ", ".join(enlaces))
        menu[rol] = "; ".join(p for p in partes if p)
    return menu


def generar():
    filas = leer_matriz()
    menu = leer_menu()
    lineas = [ENCABEZADO, "## Menú por rol", "", "| Rol | Entradas del menú |", "| --- | --- |"]
    for _, constante, nombre in ROLES:
        lineas.append(f"| {nombre} | {menu.get(constante, '—')} |")
    lineas += [
        "",
        "Todos los roles con sesión tienen además Notificaciones y Mi perfil.",
        "",
        f"## API por rol ({len(filas)} rutas)",
        "",
        "Las rutas van bajo `/api`. Una ruta sin sesión responde 401; con un rol no permitido, 403. Un",
        "recurso que existe pero queda fuera del alcance del usuario responde 404, igual que uno que no existe.",
        "",
        "| Ruta | Quién la usa |",
        "| --- | --- |",
    ]
    lineas += [f"| `{ruta}` | {quien(letras)} |" for ruta, letras in filas]
    return "\n".join(lineas) + "\n"


if __name__ == "__main__":
    contenido = generar()
    if "--revisar" in sys.argv:
        actual = DESTINO.read_text(encoding="utf-8").replace("\r\n", "\n")
        if actual != contenido:
            sys.exit("ROLES.md no está al día: corre python3 scripts/generar-roles.py")
        print("ROLES.md al día")
    else:
        DESTINO.write_text(contenido, encoding="utf-8", newline="\n")
        print(f"ROLES.md generado: {contenido.count(chr(10) + '| `')} rutas")
