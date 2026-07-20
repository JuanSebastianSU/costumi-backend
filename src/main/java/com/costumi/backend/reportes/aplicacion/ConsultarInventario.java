package com.costumi.backend.reportes.aplicacion;

import com.costumi.backend.reportes.dominio.GrupoInventario;
import com.costumi.backend.reportes.dominio.ResumenInventario;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: reportes de inventario (tablero de estado y resumen), RF-9.1/9.3. */
public interface ConsultarInventario {

	List<GrupoInventario> tableroDeInventario(UUID empresaId, UUID sucursalId);

	ResumenInventario resumenDeInventario(UUID empresaId);
}
