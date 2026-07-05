package com.costumi.backend.disfraces.adaptadores.entrada;

import com.costumi.backend.disfraces.aplicacion.DisfrazNoEncontrado;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores del módulo Disfraces en Problem Details (RFC 7807, §5.6). */
@RestControllerAdvice
class ManejadorDeErroresDisfraces {

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problema.setTitle("Solicitud inválida");
		return problema;
	}

	@ExceptionHandler(DisfrazNoEncontrado.class)
	ProblemDetail disfrazNoEncontrado(DisfrazNoEncontrado ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problema.setTitle("Disfraz no encontrado");
		return problema;
	}
}
