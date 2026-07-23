package com.costumi.backend.disfraces.adaptadores.entrada;

import com.costumi.backend.disfraces.aplicacion.CategoriaDeDisfrazNoEncontrada;
import com.costumi.backend.disfraces.aplicacion.DisfrazNoEncontrado;
import com.costumi.backend.disfraces.aplicacion.DisfrazSinTipoPosible;
import com.costumi.backend.disfraces.aplicacion.PrendaNoSirveParaElDisfraz;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores del módulo Disfraces en Problem Details (RFC 7807, §5.6). */
@RestControllerAdvice
class ManejadorDeErroresDisfraces {

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problema.setTitle("Solicitud inválida");
		return problema;
	}

	/**
	 * La pieza no sirve para lo que sirve el disfraz (RF-2.1 + RF-2.3): 422, porque la petición está bien
	 * formada pero pide algo que el catálogo no permite; el mensaje dice las dos salidas posibles.
	 */
	@ExceptionHandler(PrendaNoSirveParaElDisfraz.class)
	ProblemDetail prendaNoSirve(PrendaNoSirveParaElDisfraz ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
		problema.setTitle("La prenda no sirve para este disfraz");
		return problema;
	}

	/** Mezcla de piezas de solo renta con piezas de solo venta: el disfraz no serviría para nada. */
	@ExceptionHandler(DisfrazSinTipoPosible.class)
	ProblemDetail sinTipoPosible(DisfrazSinTipoPosible ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
		problema.setTitle("El disfraz no se podría rentar ni vender");
		return problema;
	}

	@ExceptionHandler(DisfrazNoEncontrado.class)
	ProblemDetail disfrazNoEncontrado(DisfrazNoEncontrado ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problema.setTitle("Disfraz no encontrado");
		return problema;
	}

	@ExceptionHandler(CategoriaDeDisfrazNoEncontrada.class)
	ProblemDetail categoriaNoEncontrada(CategoriaDeDisfrazNoEncontrada ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problema.setTitle("Categoría de disfraz no encontrada");
		return problema;
	}
}
