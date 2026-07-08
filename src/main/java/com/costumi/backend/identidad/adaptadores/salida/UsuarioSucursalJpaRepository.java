package com.costumi.backend.identidad.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface UsuarioSucursalJpaRepository extends JpaRepository<UsuarioSucursalJpaEntity, UUID> {

	List<UsuarioSucursalJpaEntity> findByUsuarioId(UUID usuarioId);

	void deleteByUsuarioId(UUID usuarioId);
}
