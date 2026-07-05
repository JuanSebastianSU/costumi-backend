package com.costumi.backend.rentas.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface RentaJpaRepository extends JpaRepository<RentaJpaEntity, UUID> {

	List<RentaJpaEntity> findByEmpresaId(UUID empresaId);

	List<RentaJpaEntity> findByEmpresaIdAndClienteId(UUID empresaId, UUID clienteId);
}
