package com.costumi.backend.compartido;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores transversales en Problem Details (RFC 7807, §5.6). */
@RestControllerAdvice
class ManejadorDeErroresCompartido {

	@ExceptionHandler(AccesoSinEmpresa.class)
	ProblemDetail accesoSinEmpresa(AccesoSinEmpresa ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
		problema.setTitle("Acceso denegado");
		return problema;
	}

	@ExceptionHandler(SucursalNoIndicada.class)
	ProblemDetail sucursalNoIndicada(SucursalNoIndicada ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problema.setTitle("Sucursal no indicada");
		return problema;
	}
}
