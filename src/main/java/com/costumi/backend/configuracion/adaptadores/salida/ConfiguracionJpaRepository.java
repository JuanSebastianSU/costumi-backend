package com.costumi.backend.configuracion.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ConfiguracionJpaRepository extends JpaRepository<ConfiguracionJpaEntity, UUID> {
}
