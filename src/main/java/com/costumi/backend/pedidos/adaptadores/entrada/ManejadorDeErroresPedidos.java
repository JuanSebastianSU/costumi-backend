package com.costumi.backend.pedidos.adaptadores.entrada;

import com.costumi.backend.pedidos.aplicacion.CarritoNoEncontrado;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores del módulo Pedidos/Carrito en Problem Details (RFC 7807, §5.6). */
@RestControllerAdvice
class ManejadorDeErroresPedidos {

	@ExceptionHandler(CarritoNoEncontrado.class)
	ProblemDetail carritoNoEncontrado(CarritoNoEncontrado ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problema.setTitle("Carrito no encontrado");
		return problema;
	}
}
