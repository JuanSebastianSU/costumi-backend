package com.costumi.backend.reportes.dominio;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Puerto de salida: modelo de lectura de ingresos (consulta la data existente, sin escritura). */
public interface IngresosReadRepository {

	/** Ingresos de la empresa (opcionalmente por sucursal y por rango {@code [desde, hasta]} de pago). */
	ResumenDeIngresos deEmpresa(UUID empresaId, UUID sucursalId, LocalDate desde, LocalDate hasta);

	/** Ingreso total por día (para la serie/tendencia del panel), en el rango y sucursal dados. */
	List<IngresoDelDia> porDia(UUID empresaId, UUID sucursalId, LocalDate desde, LocalDate hasta);
}
