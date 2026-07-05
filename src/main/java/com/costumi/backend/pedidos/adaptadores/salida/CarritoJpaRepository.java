package com.costumi.backend.pedidos.adaptadores.salida;

import com.costumi.backend.pedidos.dominio.EstadoCarrito;
import com.costumi.backend.pedidos.dominio.TipoPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface CarritoJpaRepository extends JpaRepository<CarritoJpaEntity, UUID> {

	Optional<CarritoJpaEntity> findByEmpresaIdAndSucursalIdAndClienteIdAndTipoAndEstado(
			UUID empresaId, UUID sucursalId, UUID clienteId, TipoPedido tipo, EstadoCarrito estado);

	/** Carga por PK como QUERY (no em.find) para que el @Filter multi-tenant la acote (§5.4). */
	Optional<CarritoJpaEntity> findFirstById(UUID id);
}
