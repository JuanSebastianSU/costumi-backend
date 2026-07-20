package com.costumi.backend.clientes.adaptadores.entrada;

import com.costumi.backend.clientes.dominio.CargaDeCliente;
import com.costumi.backend.clientes.dominio.Cliente;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de salida del Cliente. En el listado incluye además su carga económica ({@code saldoPendiente} y
 * {@code multaTotal}) para que la cartera muestre la cifra junto a cada cliente (RF-7/11.5); en respuestas
 * de una sola ficha (crear/editar/estado) va en 0 (no se calcula ahí).
 */
public record ClienteResponse(UUID id, UUID empresaId, String nombre, String telefono, String email,
		String documento, String direccion, boolean enListaNegra, boolean archivada,
		BigDecimal saldoPendiente, BigDecimal multaTotal) {

	static ClienteResponse desde(Cliente c) {
		return desde(c, null);
	}

	static ClienteResponse desde(Cliente c, CargaDeCliente carga) {
		CargaDeCliente cargaEfectiva = carga != null ? carga : CargaDeCliente.vacia();
		return new ClienteResponse(c.id(), c.empresaId(), c.nombre(), c.telefono(), c.email(), c.documento(),
				c.direccion(), c.enListaNegra(), c.archivada(),
				cargaEfectiva.saldoPendiente(), cargaEfectiva.multaTotal());
	}
}
