package com.costumi.backend.identidad.adaptadores.salida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface TokenRecuperacionJpaRepository extends JpaRepository<TokenRecuperacionJpaEntity, UUID> {

	Optional<TokenRecuperacionJpaEntity> findFirstByTokenHash(String tokenHash);
}
