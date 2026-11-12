package com.uniautonoma.docentia.dto;

/**
 * Firma del acta.
 *
 * @param confirmacion verdadero si quien firma confirmó que es irreversible
 */
public record FirmaActaRequest(boolean confirmacion) {}
