package com.costumi.backend.identidad.dominio;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Plantilla de permisos por rol (RF-1.3/1.5): lo que cada rol puede por defecto. Es la base sobre la
 * que el dueño activa/desactiva casillas puntuales por empleado. El Dueño y el SuperAdmin tienen todo.
 * Las {@code ACCION} reflejan la autorización por rol ya existente; las {@code VER} son abiertas al
 * personal de la empresa salvo Reportes/Configuración (solo Dueño/Encargado).
 */
public final class PlantillaDeRol {

	private PlantillaDeRol() {
	}

	/** Permisos por defecto del rol (matriz completa efectiva antes de overrides por empleado). */
	public static Set<Permiso> permisosDe(Rol rol) {
		return switch (rol) {
			case SUPERADMIN, DUENO, ENCARGADO -> todos();
			case MOSTRADOR, ATENCION -> unir(verOperativas(),
					acciones(Seccion.VENTAS, Seccion.RENTAS, Seccion.DEVOLUCIONES, Seccion.PAGOS, Seccion.CAJA,
							Seccion.CLIENTES));
			case BODEGA -> unir(verOperativas(), acciones(Seccion.INVENTARIO, Seccion.DISFRACES));
			case CLIENTE -> Set.of();
		};
	}

	private static Set<Permiso> todos() {
		return Arrays.stream(Seccion.values())
				.flatMap(s -> Arrays.stream(AccionDePermiso.values()).map(a -> new Permiso(s, a)))
				.collect(Collectors.toSet());
	}

	/** VER de todas las secciones operativas del personal salvo Reportes y Configuración. */
	private static Set<Permiso> verOperativas() {
		return Arrays.stream(Seccion.values())
				.filter(s -> s != Seccion.REPORTES && s != Seccion.CONFIGURACION && s != Seccion.EMPLEADOS)
				.map(s -> new Permiso(s, AccionDePermiso.VER))
				.collect(Collectors.toSet());
	}

	private static Set<Permiso> acciones(Seccion... secciones) {
		return EnumSet.copyOf(Arrays.asList(secciones)).stream()
				.map(s -> new Permiso(s, AccionDePermiso.ACCION))
				.collect(Collectors.toSet());
	}

	private static Set<Permiso> unir(Set<Permiso> a, Set<Permiso> b) {
		return java.util.stream.Stream.concat(a.stream(), b.stream()).collect(Collectors.toSet());
	}
}
