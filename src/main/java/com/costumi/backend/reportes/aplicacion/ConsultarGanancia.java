package com.costumi.backend.reportes.aplicacion;

import com.costumi.backend.reportes.dominio.ResumenDeGanancia;

import java.util.UUID;

/** Puerto de entrada: consulta la ganancia de la empresa (ingreso − costo, RF-9). */
public interface ConsultarGanancia {

	ResumenDeGanancia gananciaDeEmpresa(UUID empresaId);
}
