package com.costumi.backend.compartido;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Utilidad compartida para generar PDFs (export de reportes RF-9.2, comprobante y contrato de renta
 * RF-3.4). Sin credenciales ni servicios externos: usa OpenPDF en memoria. Vive en {@code compartido}
 * porque la usan varios módulos (reportes, pagos, rentas).
 */
@Component
public class GeneradorDePdf {

	private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.DARK_GRAY);
	private static final Font ENCABEZADO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
	private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10);

	/** PDF con un título y una tabla (encabezados + filas). Para exports de reportes. */
	public byte[] tabla(String titulo, String[] columnas, List<String[]> filas) {
		return construir(doc -> {
			doc.add(new Paragraph(titulo, TITULO));
			doc.add(new Paragraph(" "));
			PdfPTable tabla = new PdfPTable(columnas.length);
			tabla.setWidthPercentage(100);
			for (String col : columnas) {
				PdfPCell celda = new PdfPCell(new Phrase(col, ENCABEZADO));
				celda.setBackgroundColor(new Color(90, 60, 160));
				celda.setPadding(5);
				tabla.addCell(celda);
			}
			for (String[] fila : filas) {
				for (String valor : fila) {
					PdfPCell celda = new PdfPCell(new Phrase(valor == null ? "" : valor, NORMAL));
					celda.setPadding(4);
					tabla.addCell(celda);
				}
			}
			doc.add(tabla);
		});
	}

	/** PDF con un título y líneas de texto. Para comprobantes y contratos. */
	public byte[] documento(String titulo, List<String> lineas) {
		return construir(doc -> {
			doc.add(new Paragraph(titulo, TITULO));
			doc.add(new Paragraph(" "));
			for (String linea : lineas) {
				Paragraph p = new Paragraph(linea, NORMAL);
				p.setAlignment(Element.ALIGN_LEFT);
				doc.add(p);
			}
		});
	}

	private byte[] construir(ContenidoDelDocumento contenido) {
		try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
			Document doc = new Document();
			PdfWriter.getInstance(doc, salida);
			doc.open();
			contenido.escribir(doc);
			doc.close();
			return salida.toByteArray();
		} catch (Exception e) {
			throw new IllegalStateException("No se pudo generar el PDF", e);
		}
	}

	@FunctionalInterface
	private interface ContenidoDelDocumento {
		void escribir(Document doc) throws Exception;
	}
}
