package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.EstadoEmpresa;

import java.util.UUID;

/**
 * Se intentó una operación que exige una Empresa ACTIVA sobre una que no lo está
 * (RF-15.4: una empresa PENDIENTE/SUSPENDIDA/RECHAZADA no puede operar ni configurar).
 */
public class EmpresaNoOperativa extends RuntimeException {

	public EmpresaNoOperativa(UUID empresaId, EstadoEmpresa estado) {
		super("La empresa " + empresaId + " no está activa (estado " + estado + "): no puede operar (RF-15.4)");
	}
}
