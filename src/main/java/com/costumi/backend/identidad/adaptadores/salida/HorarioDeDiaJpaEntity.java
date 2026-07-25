package com.costumi.backend.identidad.adaptadores.salida;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalTime;
import java.util.UUID;

/** Mapeo JPA de una franja de horario de atención de la tienda (A7). Lleva {@code empresa_id} (tenant). */
@Entity
@Table(name = "horario_atencion")
class HorarioDeDiaJpaEntity {

	@Id
	private UUID id;

	@Column(name = "empresa_id", nullable = false)
	private UUID empresaId;

	@Column(name = "dia_semana", nullable = false)
	private int diaSemana;

	@Column(nullable = false)
	private LocalTime abre;

	@Column(nullable = false)
	private LocalTime cierra;

	protected HorarioDeDiaJpaEntity() {
		// requerido por JPA
	}

	HorarioDeDiaJpaEntity(UUID id, UUID empresaId, int diaSemana, LocalTime abre, LocalTime cierra) {
		this.id = id;
		this.empresaId = empresaId;
		this.diaSemana = diaSemana;
		this.abre = abre;
		this.cierra = cierra;
	}

	int getDiaSemana() {
		return diaSemana;
	}

	LocalTime getAbre() {
		return abre;
	}

	LocalTime getCierra() {
		return cierra;
	}
}
