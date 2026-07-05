package com.costumi.backend.rentas.aplicacion;

import com.costumi.backend.rentas.dominio.Renta;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: lista Rentas de una empresa, opcionalmente por cliente (scoped por tenant). */
public interface ConsultarRentas {

	List<Renta> buscar(UUID empresaId, UUID clienteId);
}
