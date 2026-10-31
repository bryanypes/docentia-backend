package com.uniautonoma.docentia.dto;

import com.uniautonoma.docentia.security.UsuarioSesion;

public record SesionResponse(String token, UsuarioSesion usuario) {}
