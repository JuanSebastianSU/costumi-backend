package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Empresa;

import java.util.UUID;

/**
 * Puerto de entrada: la <b>propia</b> empresa del usuario autenticado. Hasta ahora la única forma de
 * leer una empresa era el listado del SuperAdmin (RF-15.3), así que un DUENO no tenía manera de saber
 * ni el nombre de su tienda: la app no podía mostrarlo en ningún lado.
 *
 * <p>La empresa sale del token, nunca de la ruta: no hay forma de pedir la de otro (§5.4).
 */
public interface ConsultarMiEmpresa {

	Empresa ejecutar(UUID empresaId);
}
