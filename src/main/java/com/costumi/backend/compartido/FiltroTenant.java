package com.costumi.backend.compartido;

/**
 * Nombres del filtro Hibernate de aislamiento multi-tenant (§5.4). El filtro {@link #NOMBRE} se define
 * una vez ({@code @FilterDef}) y se aplica ({@code @Filter}) a toda entidad con {@code empresa_id}; el
 * aspecto {@code FiltroDeTenantAspect} lo activa por sesión con el {@code empresa_id} del token.
 */
public final class FiltroTenant {

	public static final String NOMBRE = "filtroTenant";
	public static final String PARAM_EMPRESA = "empresaId";
	public static final String CONDICION = "empresa_id = :empresaId";

	private FiltroTenant() {
	}
}
