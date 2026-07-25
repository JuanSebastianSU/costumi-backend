package com.costumi.backend.reportes.dominio;

import java.time.LocalDate;
import java.util.UUID;

/** Puerto de salida: modelo de lectura de ingresos (consulta la data existente, sin escritura). */
public interface IngresosReadRepository {

	/** Ingresos de la empresa (opcionalmente por sucursal y por rango {@code [desde, hasta]} de pago). */
	ResumenDeIngresos deEmpresa(UUID empresaId, UUID sucursalId, LocalDate desde, LocalDate hasta);
}
