package com.costumi.backend.reportes.adaptadores.entrada;

import com.costumi.backend.reportes.aplicacion.ConsultarIngresos;
import com.costumi.backend.reportes.dominio.ResumenDeIngresos;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

/** Reportes (RF-9), solo lectura y acotados al tenant del token. */
@RestController
@RequestMapping("/api/v1/reportes")
class ReporteController {

	private final ConsultarIngresos consultarIngresos;

	ReporteController(ConsultarIngresos consultarIngresos) {
		this.consultarIngresos = consultarIngresos;
	}

	@GetMapping("/ingresos")
	IngresosResponse ingresos(@AuthenticationPrincipal Jwt jwt) {
		String empresaId = jwt.getClaimAsString("empresa_id");
		ResumenDeIngresos resumen = (empresaId == null)
				? ResumenDeIngresos.de(BigDecimal.ZERO, BigDecimal.ZERO)
				: consultarIngresos.deEmpresa(UUID.fromString(empresaId));
		return IngresosResponse.desde(resumen);
	}
}
