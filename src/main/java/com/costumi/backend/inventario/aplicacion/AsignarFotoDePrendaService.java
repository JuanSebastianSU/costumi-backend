package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.dominio.Prenda;
import com.costumi.backend.inventario.dominio.PrendaRepository;
import com.costumi.backend.inventario.dominio.TipoDeImagen;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Sube la imagen al almacén (S3) y guarda su URL en la prenda (RF-2.9). Scoped por tenant: solo
 * sobre prendas de la empresa del token.
 */
@Service
class AsignarFotoDePrendaService implements AsignarFotoDePrenda {

	private final PrendaRepository prendas;
	private final AlmacenDeImagenes almacen;

	AsignarFotoDePrendaService(PrendaRepository prendas, AlmacenDeImagenes almacen) {
		this.prendas = prendas;
		this.almacen = almacen;
	}

	@Override
	@Transactional
	public Prenda ejecutar(UUID empresaId, UUID prendaId, byte[] contenido) {
		Prenda prenda = prendas.buscarPorId(prendaId)
				.filter(p -> p.empresaId().equals(empresaId))
				.orElseThrow(() -> new PrendaNoEncontrada(prendaId));
		// C1: solo imágenes reales (por magic bytes); el tipo/extensión salen del contenido, no del cliente.
		TipoDeImagen tipo = TipoDeImagen.detectar(contenido).orElseThrow(FormatoDeImagenNoSoportado::new);
		String clave = "prendas/" + empresaId + "/" + prendaId + "/" + UUID.randomUUID() + tipo.extension();
		String url = almacen.subir(contenido, tipo.contentType(), clave);
		prenda.asignarFoto(url);
		return prendas.guardar(prenda);
	}
}
