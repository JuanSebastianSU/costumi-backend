package com.costumi.backend.clientes.adaptadores.entrada;

import com.costumi.backend.clientes.aplicacion.ClienteNoEncontrado;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores del módulo Clientes en Problem Details (RFC 7807, §5.6). */
@RestControllerAdvice
class ManejadorDeErroresClientes {

	@ExceptionHandler(ClienteNoEncontrado.class)
	ProblemDetail clienteNoEncontrado(ClienteNoEncontrado ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problema.setTitle("Cliente no encontrado");
		return problema;
	}
}
