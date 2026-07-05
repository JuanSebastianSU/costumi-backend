package com.costumi.backend.disfraces.adaptadores.entrada;

import com.costumi.backend.disfraces.dominio.Disfraz;
import com.costumi.backend.disfraces.dominio.ModoDeDisfraz;
import com.costumi.backend.disfraces.dominio.Slot;

import java.util.List;
import java.util.UUID;

/** DTO de salida del Disfraz con sus slots (Capa 3). La disponibilidad se consulta aparte (derivada). */
public record DisfrazResponse(UUID id, UUID empresaId, String nombre, ModoDeDisfraz modo, UUID prendaFijaId,
		List<SlotDto> slots) {

	static DisfrazResponse desde(Disfraz d) {
		List<SlotDto> slots = d.slots().stream().map(DisfrazResponse::aSlotDto).toList();
		return new DisfrazResponse(d.id(), d.empresaId(), d.nombre(), d.modo(), d.prendaFijaId(), slots);
	}

	private static SlotDto aSlotDto(Slot s) {
		PoolDto pool = null;
		if (s.pool() != null) {
			List<EtiquetaPermitidaDto> etiquetas = s.pool().etiquetasPermitidas().entrySet().stream()
					.map(e -> new EtiquetaPermitidaDto(e.getKey(), List.copyOf(e.getValue())))
					.toList();
			pool = new PoolDto(s.pool().categoriaId(), etiquetas);
		}
		return new SlotDto(s.orden(), s.nombre(), s.ejeTalla(), s.tallaFija(), s.ejePrenda(), s.prendaFijaId(),
				pool, s.opcional());
	}
}
