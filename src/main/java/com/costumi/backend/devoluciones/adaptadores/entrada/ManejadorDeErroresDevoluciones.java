package com.costumi.backend.devoluciones.adaptadores.entrada;

import com.costumi.backend.inventario.StockInsuficiente;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores del módulo Devoluciones en Problem Details (RFC 7807, §5.6). */
@RestControllerAdvice
class ManejadorDeErroresDevoluciones {

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problema.setTitle("Solicitud inválida");
		return problema;
	}

	@ExceptionHandler(StockInsuficiente.class)
	ProblemDetail stockInsuficiente(StockInsuficiente ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problema.setTitle("Stock insuficiente");
		return problema;
	}
}
