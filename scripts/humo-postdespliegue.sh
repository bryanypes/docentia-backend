#!/bin/sh
# Pruebas de humo después de desplegar DOCENTIA. Solo lecturas: no crea, cambia ni borra nada.
#
# Uso:
#   sh scripts/humo-postdespliegue.sh https://nova.uniautonoma.edu.co/Evadocente-app
#   CORREO=persona@uniautonoma.edu.co CLAVE='...' sh scripts/humo-postdespliegue.sh URL
#
# Con CORREO y CLAVE también prueba un login real con contraseña (no guarda la sesión ni la muestra).
# La contraseña se lee del entorno para que no quede en el historial de la terminal.
set -u
BASE="${1:?Indica la URL pública, por ejemplo https://nova.uniautonoma.edu.co/Evadocente-app}"
BASE="${BASE%/}"
API="$BASE/api"
FALLAS=0
AVISOS=0

estado() { curl -s -o /dev/null -w '%{http_code}' --max-time 20 "$@"; }
revisar() {
  nombre="$1"; obtenido="$2"; esperado="$3"
  if echo " $esperado " | grep -q " $obtenido "; then
    echo "OK    $nombre ($obtenido)"
  else
    echo "FALLA $nombre: respondió $obtenido, se esperaba $esperado"
    FALLAS=$((FALLAS + 1))
  fi
}

revisar "web: página de inicio" "$(estado "$BASE/")" "200"
revisar "web: ruta profunda /inicio (recarga)" "$(estado "$BASE/inicio")" "200"
revisar "api: salud" "$(estado "$API/actuator/health")" "200"
revisar "api: lista para atender (readiness)" "$(estado "$API/actuator/health/readiness")" "200"
VERSION=$(curl -s --max-time 20 "$API/actuator/info" | sed -n 's/.*"version":"\([^"]*\)".*/\1/p')
echo "INFO  versión publicada: ${VERSION:-no informada}"
revisar "api: ruta protegida sin token" "$(estado "$API/docentes/seguimiento")" "401"
revisar "api: periodo activo sin token" "$(estado "$API/periodos-academicos/activo")" "401"
revisar "api: actuator interno cerrado" "$(estado "$API/actuator/env")" "401 404"
revisar "api: login con datos inválidos" "$(estado -X POST -H 'Content-Type: application/json' -d '{"correo":"humo.no.existe@uniautonoma.edu.co","contrasena":"no-es-una-clave-valida"}' "$API/sesiones")" "401"
GOOGLE=$(estado -X POST -H 'Content-Type: application/json' -d '{"idToken":"token-de-prueba-invalido"}' "$API/sesiones/google")
if [ "$GOOGLE" = "503" ]; then
  echo "AVISO google: responde 503, falta DOCENTIA_GOOGLE_CLIENT_ID; los estudiantes no podrán entrar"
  AVISOS=$((AVISOS + 1))
else
  revisar "api: Google configurado (token inválido rechazado)" "$GOOGLE" "400 401"
fi
if [ -n "${CORREO:-}" ] && [ -n "${CLAVE:-}" ]; then
  CUERPO=$(printf '{"correo":"%s","contrasena":"%s"}' "$CORREO" "$CLAVE")
  revisar "api: login real con contraseña" "$(estado -X POST -H 'Content-Type: application/json' -d "$CUERPO" "$API/sesiones")" "200 201"
fi

if [ "$FALLAS" -gt 0 ]; then
  echo "Resultado: $FALLAS problema(s). Revisa OPERACION.md, sección 6 (vuelta atrás)."
  exit 1
fi
if [ "$AVISOS" -gt 0 ]; then
  echo "Resultado: sin fallas, con $AVISOS aviso(s) de configuración que se corrigen con su variable."
else
  echo "Resultado: todo responde como se espera."
fi
