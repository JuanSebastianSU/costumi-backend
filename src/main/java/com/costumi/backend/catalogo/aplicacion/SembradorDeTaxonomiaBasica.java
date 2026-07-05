package com.costumi.backend.catalogo.aplicacion;

import com.costumi.backend.catalogo.dominio.Categoria;
import com.costumi.backend.catalogo.dominio.CategoriaRepository;
import com.costumi.backend.catalogo.dominio.TipoEtiqueta;
import com.costumi.backend.catalogo.dominio.TipoEtiquetaRepository;
import com.costumi.backend.catalogo.dominio.ValorEtiqueta;
import com.costumi.backend.catalogo.dominio.ValorEtiquetaRepository;
import com.costumi.backend.identidad.EmpresaAprobada;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Siembra la <b>taxonomía básica</b> de una empresa cuando se aprueba (RF-2.7.7 / RF-13.5): categorías
 * comunes y los tipos de variante "Color" y "Talla" con valores por defecto. Reacciona al evento
 * {@link EmpresaAprobada} de Identidad (§5.5), de forma síncrona dentro de su transacción.
 */
@Component
class SembradorDeTaxonomiaBasica {

	private static final List<String> CATEGORIAS = List.of(
			"Camisa", "Pantalón", "Vestido", "Sombrero", "Zapatos", "Accesorio");
	private static final List<String> COLORES = List.of("Rojo", "Azul", "Negro", "Blanco");
	private static final List<String> TALLAS = List.of("S", "M", "L", "XL");

	private final CategoriaRepository categorias;
	private final TipoEtiquetaRepository tipos;
	private final ValorEtiquetaRepository valores;

	SembradorDeTaxonomiaBasica(CategoriaRepository categorias, TipoEtiquetaRepository tipos,
			ValorEtiquetaRepository valores) {
		this.categorias = categorias;
		this.tipos = tipos;
		this.valores = valores;
	}

	@EventListener
	@Transactional
	void sembrar(EmpresaAprobada evento) {
		UUID empresaId = evento.empresaId();
		CATEGORIAS.forEach(nombre -> categorias.guardar(Categoria.crear(empresaId, nombre)));
		sembrarTipoConValores(empresaId, "Color", COLORES);
		sembrarTipoConValores(empresaId, "Talla", TALLAS);
	}

	private void sembrarTipoConValores(UUID empresaId, String nombre, List<String> valoresIniciales) {
		TipoEtiqueta tipo = tipos.guardar(TipoEtiqueta.crear(empresaId, nombre, true, true));
		valoresIniciales.forEach(valor -> valores.guardar(ValorEtiqueta.crear(empresaId, tipo.id(), valor)));
	}
}
