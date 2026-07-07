package com.costumi.backend.inventario.adaptadores.salida;

import com.costumi.backend.inventario.aplicacion.AlmacenDeImagenes;
import com.costumi.backend.inventario.aplicacion.AlmacenDeImagenesNoConfigurado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Almacenamiento de imágenes en AWS S3 (RF-2.9), <b>gateado por configuración</b>: si no hay bucket
 * y región configurados, {@link #disponible()} es false y {@link #subir} lanza
 * {@link AlmacenDeImagenesNoConfigurado} (→ 503), sin romper el resto de la app.
 */
@Component
class AlmacenDeImagenesS3 implements AlmacenDeImagenes {

	private final String bucket;
	private final String region;

	// TODO(credenciales): AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY (cadena de credenciales por defecto del SDK)
	//                     + COSTUMI_S3_BUCKET y AWS_REGION. Sin bucket/región, subir es 503 (no configurado).
	AlmacenDeImagenesS3(@Value("${costumi.imagenes.s3.bucket:}") String bucket,
			@Value("${costumi.imagenes.s3.region:}") String region) {
		this.bucket = bucket;
		this.region = region;
	}

	@Override
	public boolean disponible() {
		return bucket != null && !bucket.isBlank() && region != null && !region.isBlank();
	}

	@Override
	public String subir(byte[] contenido, String contentType, String clave) {
		if (!disponible()) {
			throw new AlmacenDeImagenesNoConfigurado();
		}
		try (S3Client s3 = S3Client.builder().region(Region.of(region)).build()) {
			s3.putObject(PutObjectRequest.builder().bucket(bucket).key(clave).contentType(contentType).build(),
					RequestBody.fromBytes(contenido));
		}
		return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + clave;
	}
}
