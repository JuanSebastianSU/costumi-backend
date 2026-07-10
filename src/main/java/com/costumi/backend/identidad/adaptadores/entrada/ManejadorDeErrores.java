package com.costumi.backend.identidad.adaptadores.entrada;

import com.costumi.backend.identidad.aplicacion.AccesoAlTenantDenegado;
import com.costumi.backend.identidad.aplicacion.CredencialesInvalidas;
import com.costumi.backend.identidad.aplicacion.CuentaDesactivada;
import com.costumi.backend.identidad.aplicacion.EmailYaRegistrado;
import com.costumi.backend.identidad.aplicacion.EmpleadoNoEncontrado;
import com.costumi.backend.identidad.aplicacion.EmpresaNoEncontrada;
import com.costumi.backend.identidad.aplicacion.EmpresaNoOperativa;
import com.costumi.backend.identidad.aplicacion.GestionDeEmpleadoNoPermitida;
import com.costumi.backend.identidad.aplicacion.LimiteDeSucursales;
import com.costumi.backend.identidad.aplicacion.RefreshInvalido;
import com.costumi.backend.identidad.aplicacion.SucursalConDependencias;
import com.costumi.backend.identidad.aplicacion.SucursalNoEncontrada;
import com.costumi.backend.identidad.aplicacion.TokenDeRecuperacionInvalido;
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

	@ExceptionHandler(EmpleadoNoEncontrado.class)
	ProblemDetail empleadoNoEncontrado(EmpleadoNoEncontrado ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problema.setTitle("Empleado no encontrado");
		return problema;
	}

	@ExceptionHandler(CuentaDesactivada.class)
	ProblemDetail cuentaDesactivada(CuentaDesactivada ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
		problema.setTitle("Cuenta desactivada");
		return problema;
	}

	@ExceptionHandler(SucursalNoEncontrada.class)
	ProblemDetail sucursalNoEncontrada(SucursalNoEncontrada ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problema.setTitle("Sucursal no encontrada");
		return problema;
	}

	@ExceptionHandler(SucursalConDependencias.class)
	ProblemDetail sucursalConDependencias(SucursalConDependencias ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problema.setTitle("Sucursal con dependencias");
		problema.setProperty("unidadesStock", ex.unidadesStock());
		problema.setProperty("rentasVigentes", ex.rentasVigentes());
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

	@ExceptionHandler(LimiteDeSucursales.class)
	ProblemDetail limiteDeSucursales(LimiteDeSucursales ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problema.setTitle("Multi-sucursal desactivado");
		return problema;
	}

	@ExceptionHandler(EmailYaRegistrado.class)
	ProblemDetail emailYaRegistrado(EmailYaRegistrado ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problema.setTitle("Correo ya registrado");
		return problema;
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problema.setTitle("Solicitud inválida");
		return problema;
	}

	@ExceptionHandler(TokenDeRecuperacionInvalido.class)
	ProblemDetail tokenDeRecuperacionInvalido(TokenDeRecuperacionInvalido ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problema.setTitle("Token de recuperación inválido");
		return problema;
	}

	@ExceptionHandler(CredencialesInvalidas.class)
	ProblemDetail credencialesInvalidas(CredencialesInvalidas ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
		problema.setTitle("Credenciales inválidas");
		return problema;
	}

	@ExceptionHandler(RefreshInvalido.class)
	ProblemDetail refreshInvalido(RefreshInvalido ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
		problema.setTitle("Token de refresco inválido");
		return problema;
	}

	@ExceptionHandler(AccesoAlTenantDenegado.class)
	ProblemDetail accesoAlTenantDenegado(AccesoAlTenantDenegado ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
		problema.setTitle("Acceso denegado");
		return problema;
	}

	@ExceptionHandler(GestionDeEmpleadoNoPermitida.class)
	ProblemDetail gestionDeEmpleadoNoPermitida(GestionDeEmpleadoNoPermitida ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
		problema.setTitle("Gestión de empleado no permitida");
		return problema;
	}
}
