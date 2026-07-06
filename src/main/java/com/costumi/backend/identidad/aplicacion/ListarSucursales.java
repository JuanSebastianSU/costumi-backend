package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Sucursal;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: listar las sucursales de una empresa (lectura, scoped por tenant). */
public interface ListarSucursales {

	List<Sucursal> deEmpresa(UUID empresaId);
}
