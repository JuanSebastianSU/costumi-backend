package com.costumi.backend.operaciones.adaptadores.salida;

import com.costumi.backend.identidad.DependenciasDeSucursal;
import com.costumi.backend.inventario.ConsultaDeInventario;
import com.costumi.backend.rentas.ConsultaDeRentas;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implementa el puerto {@link DependenciasDeSucursal} de Identidad componiendo las APIs públicas de
 * Inventario (unidades de stock en la sucursal) y Rentas (rentas vigentes en la sucursal).
 *
 * <p>Vive en el módulo {@code operaciones} —no en Identidad— para invertir la dependencia: Identidad no
 * puede importar Inventario/Rentas sin cerrar el ciclo {@code catalogo → identidad → inventario → catalogo}.
 */
@Component
class DependenciasDeSucursalAdapter implements DependenciasDeSucursal {

	private final ConsultaDeInventario inventario;
	private final ConsultaDeRentas rentas;

	DependenciasDeSucursalAdapter(ConsultaDeInventario inventario, ConsultaDeRentas rentas) {
		this.inventario = inventario;
		this.rentas = rentas;
	}

	@Override
	public Conteo contar(UUID empresaId, UUID sucursalId) {
		int unidades = inventario.contarUnidadesEnSucursal(empresaId, sucursalId);
		int rentasVigentes = rentas.contarRentasVigentesEnSucursal(empresaId, sucursalId);
		return new Conteo(unidades, rentasVigentes);
	}
}
