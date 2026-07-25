package com.costumi.backend.identidad.aplicacion;

import com.costumi.backend.identidad.dominio.Empresa;

import java.util.UUID;

/**
 * Puerto de entrada: el <b>Dueño</b> edita la identidad/marca de su propia tienda (A7) — textos y las
 * imágenes de logo y portada. Distinto de {@link GestionarEmpresa}, que es el ciclo de vida del SuperAdmin.
 * La empresa siempre es la del token (nunca un id del request), igual que {@code GET /empresas/mia}.
 */
public interface GestionarIdentidadDeTienda {

	Empresa editar(UUID empresaId, String nombre, String ubicacion, String contacto, String descripcion,
			String ciudad);

	/** Sube y asigna el logo de la tienda; devuelve la empresa con la URL ya puesta. */
	Empresa asignarLogo(UUID empresaId, byte[] contenido);

	/** Sube y asigna la portada/banner de la tienda. */
	Empresa asignarPortada(UUID empresaId, byte[] contenido);
}
