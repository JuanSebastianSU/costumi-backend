package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.AccesoAlTenantDenegado;
import com.costumi.backend.identidad.aplicacion.CredencialesInvalidas;
import com.costumi.backend.identidad.aplicacion.EmpresaNoEncontrada;
import com.costumi.backend.identidad.aplicacion.EmpresaNoOperativa;
import com.costumi.backend.identidad.dominio.TransicionDeEstadoInvalida;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce errores de dominio/aplicación a respuestas Problem Details (RFC 7807, §5.6). */
@RestControllerAdvice
class ManejadorDeErrores {

	@ExceptionHandler(EmpresaNoEncontrada.class)
	ProblemDetail empresaNoEncontrada(EmpresaNoEncontrada ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problema.setTitle("Empresa no encontrada");
		return problema;
	}

	@ExceptionHandler(TransicionDeEstadoInvalida.class)
	ProblemDetail transicionInvalida(TransicionDeEstadoInvalida ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problema.setTitle("Transición de estado inválida");
		return problema;
	}

	@ExceptionHandler(EmpresaNoOperativa.class)
	ProblemDetail empresaNoOperativa(EmpresaNoOperativa ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problema.setTitle("Empresa no operativa");
		return problema;
	}

	@ExceptionHandler(CredencialesInvalidas.class)
	ProblemDetail credencialesInvalidas(CredencialesInvalidas ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
		problema.setTitle("Credenciales inválidas");
		return problema;
	}

	@ExceptionHandler(AccesoAlTenantDenegado.class)
	ProblemDetail accesoAlTenantDenegado(AccesoAlTenantDenegado ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
		problema.setTitle("Acceso denegado");
		return problema;
	}
}
