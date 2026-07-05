package com.costumi.backend.catalogo.adaptadores.entrada;

import com.costumi.backend.catalogo.aplicacion.TipoEtiquetaNoEncontrado;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores del módulo Catálogo en formato Problem Details (RFC 7807, §5.6). */
@RestControllerAdvice
class ManejadorDeErroresCatalogo {

	@ExceptionHandler(TipoEtiquetaNoEncontrado.class)
	ProblemDetail tipoEtiquetaNoEncontrado(TipoEtiquetaNoEncontrado ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problema.setTitle("Tipo de etiqueta no encontrado");
		return problema;
	}
}
