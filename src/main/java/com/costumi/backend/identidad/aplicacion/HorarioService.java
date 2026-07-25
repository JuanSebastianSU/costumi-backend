package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.HorarioDeDia;
import com.costumi.backend.identidad.dominio.HorarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Casos de uso del horario de atención de la tienda (A7). */
@Service
class HorarioService implements GestionarHorario {

	private final HorarioRepository horarios;

	HorarioService(HorarioRepository horarios) {
		this.horarios = horarios;
	}

	@Override
	@Transactional(readOnly = true)
	public List<HorarioDeDia> deEmpresa(UUID empresaId) {
		return horarios.deEmpresa(empresaId);
	}

	@Override
	@Transactional
	public void fijar(UUID empresaId, List<HorarioDeDia> horario) {
		Set<Integer> dias = new HashSet<>();
		for (HorarioDeDia dia : horario) {
			if (!dias.add(dia.diaSemana())) {
				throw new IllegalArgumentException("El horario no puede repetir el mismo día");
			}
		}
		horarios.reemplazar(empresaId, horario);
	}
}
