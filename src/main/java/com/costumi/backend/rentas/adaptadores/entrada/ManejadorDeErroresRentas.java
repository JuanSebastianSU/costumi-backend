package com.costumi.backend.rentas.adaptadores.entrada;

import com.costumi.backend.rentas.aplicacion.RentaNoEncontrada;
import com.costumi.backend.rentas.dominio.TransicionDeRentaInvalida;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores del módulo Rentas en Problem Details (RFC 7807, §5.6). */
@RestControllerAdvice
class ManejadorDeErroresRentas {

	@ExceptionHandler(RentaNoEncontrada.class)
	ProblemDetail rentaNoEncontrada(RentaNoEncontrada ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problema.setTitle("Renta no encontrada");
		return problema;
	}

	@ExceptionHandler(TransicionDeRentaInvalida.class)
	ProblemDetail transicionInvalida(TransicionDeRentaInvalida ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problema.setTitle("Transición de renta inválida");
		return problema;
	}
}
