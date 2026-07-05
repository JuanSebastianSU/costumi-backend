package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.dominio.Prenda;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: lista las Prendas de una empresa (scoped por tenant). */
public interface ConsultarPrendas {

	List<Prenda> deEmpresa(UUID empresaId);
}
