// Sin caracteres que se confunden al dictarlos o copiarlos a mano: 0/O, 1/l/I.
const MINUSCULAS = 'abcdefghijkmnpqrstuvwxyz'
const MAYUSCULAS = 'ABCDEFGHJKLMNPQRSTUVWXYZ'
const DIGITOS = '23456789'
const SIMBOLOS = '*#$%&?!'
const TODOS = MINUSCULAS + MAYUSCULAS + DIGITOS + SIMBOLOS

function aleatorio(maximo) {
  const valor = new Uint32Array(1)
  crypto.getRandomValues(valor)
  return valor[0] % maximo
}

const elegir = (caracteres) => caracteres[aleatorio(caracteres.length)]

/** Contraseña temporal de `longitud` caracteres con al menos una minúscula, mayúscula, dígito y símbolo. */
export function generarContrasenaTemporal(longitud = 12) {
  const caracteres = [elegir(MINUSCULAS), elegir(MAYUSCULAS), elegir(DIGITOS), elegir(SIMBOLOS)]
  while (caracteres.length < longitud) caracteres.push(elegir(TODOS))
  // Fisher-Yates para que los obligatorios no queden siempre al principio.
  for (let i = caracteres.length - 1; i > 0; i--) {
    const j = aleatorio(i + 1)
    ;[caracteres[i], caracteres[j]] = [caracteres[j], caracteres[i]]
  }
  return caracteres.join('')
}
