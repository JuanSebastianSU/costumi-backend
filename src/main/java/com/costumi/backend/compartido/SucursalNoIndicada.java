package com.costumi.backend.compartido;

/** La operación requiere una sucursal activa (cabecera {@code X-Sucursal-Id}) y no se indicó (RF-17.4). */
public class SucursalNoIndicada extends RuntimeException {

	public SucursalNoIndicada() {
		super("La operación requiere indicar la sucursal activa en la cabecera X-Sucursal-Id");
	}
}
