package com.costumi.backend.identidad;

import java.util.UUID;

/**
 * Puerto (API pública de Identidad) para conocer las <b>dependencias operativas</b> de una sucursal antes
 * de archivarla (RF-15.1): unidades de stock y rentas vigentes. Lo <b>necesita</b> Identidad, pero lo
 * <b>implementa</b> otro módulo componiendo Inventario + Rentas.
 *
 * <p>Es una <b>inversión de dependencias</b> deliberada: si Identidad llamara directamente a Inventario/
 * Rentas cerraría el ciclo {@code catalogo → identidad → inventario → catalogo} (Catálogo ya depende de
 * Identidad por el evento {@code EmpresaAprobada}), que Spring Modulith prohíbe. Con este puerto, Identidad
 * solo conoce su propia interfaz; el módulo {@code operaciones} aporta la implementación.
 */
public interface DependenciasDeSucursal {

	/** Conteo de dependencias operativas de una sucursal. */
	record Conteo(int unidadesStock, int rentasVigentes) {

		/** ¿La sucursal tiene alguna dependencia que impida archivarla? */
		public boolean hayDependencias() {
			return unidadesStock > 0 || rentasVigentes > 0;
		}
	}

	/** Cuenta las dependencias operativas de la sucursal (de la empresa/tenant). */
	Conteo contar(UUID empresaId, UUID sucursalId);
}
