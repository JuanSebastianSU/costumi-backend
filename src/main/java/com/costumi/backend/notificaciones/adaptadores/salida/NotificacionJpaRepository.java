package com.costumi.backend.notificaciones.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface NotificacionJpaRepository extends JpaRepository<NotificacionJpaEntity, UUID> {

	List<NotificacionJpaEntity> findByEmpresaId(UUID empresaId);
}
