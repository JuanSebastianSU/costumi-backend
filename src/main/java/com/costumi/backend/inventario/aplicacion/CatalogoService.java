package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.dominio.CatalogoDeInventarioReadRepository;
import com.costumi.backend.inventario.dominio.PrendaDeCatalogo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Lista el catálogo del tenant (solo lectura), acotado a la empresa del token. */
@Service
class CatalogoService implements ConsultarCatalogo {

	private final CatalogoDeInventarioReadRepository catalogo;

	CatalogoService(CatalogoDeInventarioReadRepository catalogo) {
		this.catalogo = catalogo;
	}

	@Override
	@Transactional(readOnly = true)
	public List<PrendaDeCatalogo> ejecutar(UUID empresaId, UUID categoriaId, Map<UUID, Set<UUID>> etiquetas) {
		return catalogo.buscar(empresaId, categoriaId, etiquetas == null ? Map.of() : etiquetas);
	}
}
