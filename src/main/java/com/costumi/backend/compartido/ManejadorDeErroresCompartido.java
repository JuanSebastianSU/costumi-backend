package com.costumi.backend.compartido;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;

/** Errores transversales en Problem Details (RFC 7807, §5.6). */
@RestControllerAdvice
class ManejadorDeErroresCompartido {

	private static final Logger log = LoggerFactory.getLogger(ManejadorDeErroresCompartido.class);

	/** Código SQLState de PostgreSQL para "unique_violation" (violación de índice/constraint único). */
	private static final String UNIQUE_VIOLATION = "23505";

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

	@ExceptionHandler(EmpleadoNoAsignadoASucursal.class)
	ProblemDetail empleadoNoAsignado(EmpleadoNoAsignadoASucursal ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
		problema.setTitle("Empleado no asignado a la sucursal");
		return problema;
	}

	/**
	 * Traduce las violaciones de integridad de la BD a <b>409 Conflict</b> en vez de un 500 crudo: un índice
	 * único duplicado (p. ej. dos categorías con el mismo nombre) es un conflicto del cliente, no un fallo del
	 * servidor. Cubre de una todos los índices únicos del sistema. Las no-unicidad se registran para diagnóstico.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail integridadDeDatos(DataIntegrityViolationException ex) {
		if (esViolacionDeUnicidad(ex)) {
			ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
					"Ya existe un registro con esos datos (viola una restricción de unicidad).");
			problema.setTitle("Conflicto de datos");
			return problema;
		}
		log.warn("Violación de integridad de datos", ex);
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"La operación viola una restricción de integridad de los datos.");
		problema.setTitle("Conflicto de integridad");
		return problema;
	}

	private static boolean esViolacionDeUnicidad(Throwable ex) {
		for (Throwable t = ex; t != null; t = t.getCause()) {
			if (t instanceof SQLException sql && UNIQUE_VIOLATION.equals(sql.getSQLState())) {
				return true;
			}
		}
		return false;
	}
}
