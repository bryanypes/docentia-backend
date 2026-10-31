package com.uniautonoma.docentia.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** Página de resultados con un formato estable, independiente de Spring Data. */
public record PaginaResponse<T>(
    List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

  public static <E, T> PaginaResponse<T> desde(Page<E> pagina, Function<E, T> convertir) {
    return new PaginaResponse<>(
        pagina.getContent().stream().map(convertir).toList(),
        pagina.getNumber(),
        pagina.getSize(),
        pagina.getTotalElements(),
        pagina.getTotalPages());
  }
}
