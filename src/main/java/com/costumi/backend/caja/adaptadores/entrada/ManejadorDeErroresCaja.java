package com.costumi.backend.caja.adaptadores.entrada;

import com.costumi.backend.caja.aplicacion.TurnoNoEncontrado;
import com.costumi.backend.caja.dominio.TurnoNoAbierto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores del módulo Caja en Problem Details (RFC 7807, §5.6). */
@RestControllerAdvice
class ManejadorDeErroresCaja {

	@ExceptionHandler(TurnoNoEncontrado.class)
	ProblemDetail turnoNoEncontrado(TurnoNoEncontrado ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problema.setTitle("Turno no encontrado");
		return problema;
	}

	@ExceptionHandler(TurnoNoAbierto.class)
	ProblemDetail turnoNoAbierto(TurnoNoAbierto ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problema.setTitle("Turno no abierto");
		return problema;
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problema.setTitle("Solicitud inválida");
		return problema;
	}
}
