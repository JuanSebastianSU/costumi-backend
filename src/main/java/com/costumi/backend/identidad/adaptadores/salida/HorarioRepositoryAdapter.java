package com.costumi.backend.identidad.adaptadores.salida;

import com.costumi.backend.identidad.dominio.HorarioDeDia;
import com.costumi.backend.identidad.dominio.HorarioRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Adaptador de salida: implementa {@link HorarioRepository} con JPA (reemplazo completo por empresa). */
@Repository
class HorarioRepositoryAdapter implements HorarioRepository {

	private final HorarioJpaRepository jpa;

	HorarioRepositoryAdapter(HorarioJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public List<HorarioDeDia> deEmpresa(UUID empresaId) {
		return jpa.findByEmpresaIdOrderByDiaSemana(empresaId).stream()
				.map(e -> new HorarioDeDia(e.getDiaSemana(), e.getAbre(), e.getCierra()))
				.toList();
	}

	@Override
	@Transactional
	public void reemplazar(UUID empresaId, List<HorarioDeDia> horario) {
		// Reemplazo completo: se borran los días previos y se insertan los nuevos (un renglón por día).
		jpa.deleteByEmpresaId(empresaId);
		for (HorarioDeDia dia : horario) {
			jpa.save(new HorarioDeDiaJpaEntity(UUID.randomUUID(), empresaId, dia.diaSemana(), dia.abre(),
					dia.cierra()));
		}
	}
}
