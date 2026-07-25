package com.costumi.backend.identidad.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: membresías persona↔tienda (Fase B). Cruza empresas por usuario (NO se filtra por tenant). */
public interface MembresiaRepository {

	/** Todas las membresías del usuario (en todas las tiendas). */
	List<Membresia> deUsuario(UUID usuarioId);

	/** La membresía del usuario en esa empresa, si existe (para validar el cambio de contexto). */
	Optional<Membresia> buscar(UUID usuarioId, UUID empresaId);

	/** La (única) membresía de trabajo ACTIVA del usuario, si tiene una (para entrar a «Trabajando»). */
	Optional<Membresia> activaDeUsuario(UUID usuarioId);

	/** Crea o actualiza la membresía (upsert por usuario+empresa). */
	Membresia guardar(Membresia membresia);
}
