package com.costumi.backend.inventario.adaptadores.entrada;

import com.costumi.backend.inventario.aplicacion.GrupoDeStockNoEncontrado;
import com.costumi.backend.inventario.aplicacion.PrendaNoEncontrada;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Errores del módulo Inventario en Problem Details (RFC 7807, §5.6). Las violaciones de reglas
 * de dominio (p. ej. precio obligatorio o mover más unidades de las que hay) se lanzan como
 * {@link IllegalArgumentException} y se traducen a 400.
 */
@RestControllerAdvice
class ManejadorDeErroresInventario {

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problema.setTitle("Solicitud inválida");
		return problema;
	}

	@ExceptionHandler(PrendaNoEncontrada.class)
	ProblemDetail prendaNoEncontrada(PrendaNoEncontrada ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problema.setTitle("Prenda no encontrada");
		return problema;
	}

	@ExceptionHandler(GrupoDeStockNoEncontrado.class)
	ProblemDetail grupoNoEncontrado(GrupoDeStockNoEncontrado ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problema.setTitle("Grupo de stock no encontrado");
		return problema;
	}
}
