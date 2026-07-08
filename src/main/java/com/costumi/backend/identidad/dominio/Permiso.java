package com.costumi.backend.identidad.dominio;

/** Un permiso concreto: una acción (ver/actuar) sobre una sección (RF-1.5). */
public record Permiso(Seccion seccion, AccionDePermiso accion) {
}
