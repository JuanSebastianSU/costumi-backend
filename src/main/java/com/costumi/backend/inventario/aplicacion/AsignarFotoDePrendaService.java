package com.costumi.backend.inventario.aplicacion;

import com.costumi.backend.inventario.dominio.Prenda;
import com.costumi.backend.inventario.dominio.PrendaRepository;
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
	public Prenda ejecutar(UUID empresaId, UUID prendaId, byte[] contenido, String contentType, String nombreArchivo) {
		Prenda prenda = prendas.buscarPorId(prendaId)
				.filter(p -> p.empresaId().equals(empresaId))
				.orElseThrow(() -> new PrendaNoEncontrada(prendaId));
		String clave = "prendas/" + empresaId + "/" + prendaId + "/" + UUID.randomUUID() + extension(nombreArchivo);
		String url = almacen.subir(contenido, contentType, clave);
		prenda.asignarFoto(url);
		return prendas.guardar(prenda);
	}

	private static String extension(String nombreArchivo) {
		if (nombreArchivo == null) {
			return "";
		}
		int punto = nombreArchivo.lastIndexOf('.');
		return (punto >= 0) ? nombreArchivo.substring(punto).toLowerCase() : "";
	}
}
