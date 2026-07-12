package com.costumi.backend.pagos.adaptadores.entrada;

import com.costumi.backend.pagos.aplicacion.DecisionDeReembolsoNoPermitida;
import com.costumi.backend.pagos.aplicacion.ItemNoDevuelto;
import com.costumi.backend.pagos.aplicacion.PagoEnLineaDeshabilitado;
import com.costumi.backend.pagos.aplicacion.ReembolsoNoAutorizado;
import com.costumi.backend.pagos.aplicacion.SolicitudDeReembolsoInvalida;
import com.costumi.backend.pagos.aplicacion.SolicitudDeReembolsoNoEncontrada;
import com.costumi.backend.pagos.aplicacion.VerificacionDePagoFallida;
import com.costumi.backend.pagos.dominio.PasarelaNoConfigurada;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores del módulo Pagos en Problem Details (RFC 7807, §5.6). */
@RestControllerAdvice
class ManejadorDeErroresPagos {

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail argumentoInvalido(IllegalArgumentException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		problema.setTitle("Solicitud inválida");
		return problema;
	}

	@ExceptionHandler(PagoEnLineaDeshabilitado.class)
	ProblemDetail pagoEnLineaDeshabilitado(PagoEnLineaDeshabilitado ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problema.setTitle("Pago en línea deshabilitado");
		return problema;
	}

	@ExceptionHandler(PasarelaNoConfigurada.class)
	ProblemDetail pasarelaNoConfigurada(PasarelaNoConfigurada ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
		problema.setTitle("Pasarela de pago no configurada");
		return problema;
	}

	@ExceptionHandler(VerificacionDePagoFallida.class)
	ProblemDetail verificacionDePagoFallida(VerificacionDePagoFallida ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problema.setTitle("Verificación de pago fallida");
		return problema;
	}

	@ExceptionHandler(SolicitudDeReembolsoNoEncontrada.class)
	ProblemDetail solicitudNoEncontrada(SolicitudDeReembolsoNoEncontrada ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problema.setTitle("Solicitud de reembolso no encontrada");
		return problema;
	}

	@ExceptionHandler(SolicitudDeReembolsoInvalida.class)
	ProblemDetail solicitudInvalida(SolicitudDeReembolsoInvalida ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problema.setTitle("Solicitud de reembolso inválida");
		return problema;
	}

	@ExceptionHandler(ItemNoDevuelto.class)
	ProblemDetail itemNoDevuelto(ItemNoDevuelto ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
		problema.setTitle("Ítem no devuelto");
		return problema;
	}

	@ExceptionHandler(DecisionDeReembolsoNoPermitida.class)
	ProblemDetail decisionNoPermitida(DecisionDeReembolsoNoPermitida ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
		problema.setTitle("Decisión de reembolso no permitida");
		return problema;
	}

	@ExceptionHandler(ReembolsoNoAutorizado.class)
	ProblemDetail reembolsoNoAutorizado(ReembolsoNoAutorizado ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
		problema.setTitle("Reembolso no autorizado");
		return problema;
	}

	@ExceptionHandler(FirmaDeWebhookInvalida.class)
	ProblemDetail firmaDeWebhookInvalida(FirmaDeWebhookInvalida ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
		problema.setTitle("Firma de webhook inválida");
		return problema;
	}

	@ExceptionHandler(FirmaDeWebhookNoConfigurada.class)
	ProblemDetail firmaDeWebhookNoConfigurada(FirmaDeWebhookNoConfigurada ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
		problema.setTitle("Webhook de pagos no configurado");
		return problema;
	}
}
