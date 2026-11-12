package com.uniautonoma.docentia.dto;

/**
 * Gasto del proveedor externo en un periodo. Sin texto de comentarios ni docentes.
 *
 * @param tokensAhorradosCache tokens que habrían costado las respuestas servidas desde la caché
 * @param ahorroCachePct ahorrados por caché sobre (gastados + ahorrados), de 0 a 100
 * @param ahorroMuestreoPct entrada evitada por topes y muestreo sobre la entrada sin topes, de 0 a
 *     100
 * @param llamadasEstimadas llamadas cuyo uso se estimó porque el proveedor no lo informó
 * @param costoEstimado nulo si no hay precios configurados
 */
public record UsoIaResponse(
    Long periodoId,
    long llamadas,
    long aciertosCache,
    long rechazadosPresupuesto,
    long errores,
    long tokensEntrada,
    long tokensSalida,
    long tokensCacheProveedor,
    long tokensAhorradosCache,
    double ahorroCachePct,
    double ahorroMuestreoPct,
    long llamadasEstimadas,
    Double costoEstimado) {}
