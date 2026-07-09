package com.costumi.backend.ventas.adaptadores.entrada;

import com.costumi.backend.inventario.StockInsuficiente;
import com.costumi.backend.ventas.aplicacion.VentaNoEncontrada;
import com.costumi.backend.ventas.dominio.ReembolsoNoPermitido;
import com.costumi.backend.ventas.dominio.VentaNoDevolvible;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores del módulo Ventas en Problem Details (RFC 7807, §5.6). */
@RestControllerAdvice
class ManejadorDeErroresVentas {

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

	@ExceptionHandler(VentaNoEncontrada.class)
	ProblemDetail ventaNoEncontrada(VentaNoEncontrada ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problema.setTitle("Venta no encontrada");
		return problema;
	}

	@ExceptionHandler(VentaNoDevolvible.class)
	ProblemDetail ventaNoDevolvible(VentaNoDevolvible ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problema.setTitle("Venta no devolvible");
		return problema;
	}

	@ExceptionHandler(ReembolsoNoPermitido.class)
	ProblemDetail reembolsoNoPermitido(ReembolsoNoPermitido ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problema.setTitle("Reembolso no permitido");
		return problema;
	}
}
