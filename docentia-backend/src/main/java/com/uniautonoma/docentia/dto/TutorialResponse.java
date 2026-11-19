package com.uniautonoma.docentia.dto;

/**
 * Estado del tutorial guiado del usuario de la sesión.
 *
 * @param visto falso hasta que lo termina o lo salta; con falso se muestra al entrar
 */
public record TutorialResponse(boolean visto) {}
