package com.costumi.backend.marketplace.aplicacion;

import com.costumi.backend.marketplace.dominio.EmpresaEnVitrina;

import java.util.List;

/** Puerto de entrada: descubrir las empresas ACTIVAS del marketplace (RF-18.1, RF-15.6). */
public interface DescubrirEmpresas {

	List<EmpresaEnVitrina> activas();
}
