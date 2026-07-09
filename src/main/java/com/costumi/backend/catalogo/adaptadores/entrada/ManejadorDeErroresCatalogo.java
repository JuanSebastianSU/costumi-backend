package com.costumi.backend.catalogo.adaptadores.entrada;

import com.costumi.backend.catalogo.aplicacion.CategoriaDeTipoInvalida;
import com.costumi.backend.catalogo.aplicacion.CategoriaNoEncontrada;
import com.costumi.backend.catalogo.aplicacion.TipoEtiquetaNoEncontrado;
import com.costumi.backend.catalogo.aplicacion.ValorEtiquetaNoEncontrado;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores del módulo Catálogo en formato Problem Details (RFC 7807, §5.6). */
@RestControllerAdvice
class ManejadorDeErroresCatalogo {

	@ExceptionHandler(CategoriaNoEncontrada.class)
	ProblemDetail categoriaNoEncontrada(CategoriaNoEncontrada ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problema.setTitle("Categoría no encontrada");
		return problema;
	}

	@ExceptionHandler(TipoEtiquetaNoEncontrado.class)
	ProblemDetail tipoEtiquetaNoEncontrado(TipoEtiquetaNoEncontrado ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problema.setTitle("Tipo de etiqueta no encontrado");
		return problema;
	}

	@ExceptionHandler(CategoriaDeTipoInvalida.class)
	ProblemDetail categoriaDeTipoInvalida(CategoriaDeTipoInvalida ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problema.setTitle("Categoría inválida para el tipo de etiqueta");
		return problema;
	}

	@ExceptionHandler(ValorEtiquetaNoEncontrado.class)
	ProblemDetail valorEtiquetaNoEncontrado(ValorEtiquetaNoEncontrado ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problema.setTitle("Valor de etiqueta no encontrado");
		return problema;
	}
}
