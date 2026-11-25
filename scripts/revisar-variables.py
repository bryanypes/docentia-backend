#!/usr/bin/env python3
"""Revisa que toda variable de entorno que lee la API llegue al contenedor.

El docker-compose.yml no usa env_file: solo pasa al contenedor las variables que nombra en el
`environment` de `api`. Una variable del application.yml que no esté ahí no surte efecto aunque se
ponga en el .env del servidor. Este script falla si falta alguna, o si el valor por defecto del
compose no es el mismo que el del application.yml (salvo las diferencias locales a propósito).

Uso, desde la raíz del repositorio:  python3 scripts/revisar-variables.py
"""

import re
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
RECURSOS = RAIZ / "docentia-backend/src/main/resources"
COMPOSE = RAIZ / "docker-compose.yml"

# Diferencias a propósito: el compose arma un ambiente local listo para usar. En producción el .env
# del servidor fija estos valores.
DEFECTOS_LOCALES = {
    "DOCENTIA_IA_HABILITADO": "en local el análisis de comentarios queda encendido",
    "DOCENTIA_JWT_SECRETO": "clave de desarrollo; con el perfil prod la API exige una propia",
    "DOCENTIA_MAIL_HOST": "en local los correos van a Mailpit",
    "DOCENTIA_MAIL_PORT": "puerto de Mailpit",
    "DOCENTIA_MAIL_TLS": "Mailpit no usa TLS",
}

PATRON_YML = re.compile(r"\$\{((?:DOCENTIA|SPRING)_[A-Z0-9_]+)(?::([^}]*))?\}")
PATRON_COMPOSE = re.compile(r"^\s+((?:DOCENTIA|SPRING)_[A-Z0-9_]+): (.*)$", re.M)


def variables_de_la_api():
    variables = {}
    for archivo in sorted(RECURSOS.glob("application*.yml")):
        if archivo.name == "application-test.yml":
            continue
        for nombre, defecto in PATRON_YML.findall(archivo.read_text(encoding="utf-8")):
            variables.setdefault(nombre, defecto)
    return variables


def variables_del_compose():
    """Variables del servicio api: nombre -> valor por defecto si es ${NOMBRE:-x}, o None si el
    compose la arma de otra forma (por ejemplo la base, desde POSTGRES_*)."""
    texto = COMPOSE.read_text(encoding="utf-8")
    api = texto[texto.index("\n  api:") :]
    if "\n  web:" in api:
        api = api[: api.index("\n  web:")]
    variables = {}
    for nombre, valor in PATRON_COMPOSE.findall(api):
        propio = re.fullmatch(r"\$\{" + nombre + r":-([^}]*)\}", valor.strip())
        variables[nombre] = propio.group(1) if propio else None
    return variables


def main():
    api = variables_de_la_api()
    compose = variables_del_compose()
    faltan = sorted(v for v in api if v not in compose)
    distintos = sorted(
        f"{v}: application.yml «{api[v]}», compose «{compose[v]}»"
        for v in api
        if compose.get(v) is not None and api[v] != compose[v] and v not in DEFECTOS_LOCALES
    )
    if faltan or distintos:
        for v in faltan:
            print(f"No llega al contenedor: {v}")
        for d in distintos:
            print(f"Valor por defecto distinto: {d}")
        sys.exit(1)
    print(f"Las {len(api)} variables de la API llegan al contenedor con el mismo valor por defecto")


if __name__ == "__main__":
    main()
