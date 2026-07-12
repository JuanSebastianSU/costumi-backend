package com.costumi.backend.pagos.aplicacion;

/**
 * El actor no puede decidir sobre esta solicitud: para revertir una rechazada hace falta un rol superior en
 * la pirámide (escalamiento, B3). Se traduce a 403.
 */
public class DecisionDeReembolsoNoPermitida extends RuntimeException {

	public DecisionDeReembolsoNoPermitida() {
		super("Para revertir esta decisión hace falta un rol superior al que la rechazó");
	}
}
