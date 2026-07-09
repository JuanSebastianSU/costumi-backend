/**
 * Módulo <b>Operaciones</b>: consultas operativas <i>transversales</i> que componen varios módulos y que
 * otros necesitan sin acoplarse entre sí. Existe para <b>invertir dependencias y romper ciclos</b> entre
 * módulos (Spring Modulith).
 *
 * <p>Hoy provee una sola cosa: la implementación de {@code identidad.DependenciasDeSucursal}, que combina
 * el stock de una sucursal (Inventario) y sus rentas vigentes (Rentas) para que Identidad pueda impedir
 * archivar una sucursal con dependencias abiertas (RF-15.1) — sin que Identidad dependa de Inventario/Rentas.
 */
package com.costumi.backend.operaciones;
