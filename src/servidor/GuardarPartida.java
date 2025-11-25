package servidor;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GuardarPartida {

	private static final String ARCHIVO_XML = "historial.xml";
	private static int contadorPartidas = 1;

//Guarda las partidas en el documento xml
	public static void guardarResultado(String modo, String jugadores, String palabra, boolean victoria,
			String ganador) {
		try {
			// Para crear el xml:
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc;
			Element raiz;
			// Comprobamos si existe el archivo
			File archivo = new File(ARCHIVO_XML);
			if (archivo.exists()) {
				// SÍ existe → lo leemos
				doc = db.parse(archivo);
				raiz = doc.getDocumentElement();
			} else {
				// NO existe: lo creamos
				doc = db.newDocument();
				raiz = doc.createElement("historialPartidas");
				doc.appendChild(raiz);
			}

			// Creamos ujna nueva partida
			Element partida = doc.createElement("partida");
			partida.setAttribute("id", "P" + contadorPartidas++);

			// Añadimos la fecha
			Element fecha = doc.createElement("fecha");
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			fecha.setTextContent(sdf.format(new Date()));
			partida.appendChild(fecha);

			// Añadimos el modo
			Element modoElement = doc.createElement("modo");
			modoElement.setTextContent(modo);
			partida.appendChild(modoElement);

			// Añadimos los jugadores
			Element jugadoresElement = doc.createElement("jugadores");
			jugadoresElement.setTextContent(jugadores);
			partida.appendChild(jugadoresElement);

			// Añadimos la palabra
			Element palabraElement = doc.createElement("palabra");
			palabraElement.setTextContent(palabra);
			partida.appendChild(palabraElement);

			// Añadimos el resultado
			Element resultado = doc.createElement("resultado");
			resultado.setAttribute("tipo", victoria ? "victoria" : "derrota");

			if (victoria && ganador != null) {
				resultado.setAttribute("ganador", ganador);
				resultado.setTextContent(ganador + " ganó la partida");
			} else {
				resultado.setTextContent("Se acabaron los intentos, nadie ganó");
			}
			partida.appendChild(resultado);

			// Añadimos la partida completa al historial
			raiz.appendChild(partida);

			// Guardamos todo en el archivo
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(archivo);
			transformer.transform(source, result);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

//	public static void main(String[] args) {
//		// Prueba 1: Victoria en modo turnos
//		guardarResultado("turnos", "Maria, Pedro, Luis", "CARRETERA", true, "Maria");
//
//		// Prueba 2: Victoria en modo concurrente
//		guardarResultado("concurrente", "Ana, Carlos", "SISTEMAS", true, "Carlos");
//
//		// Prueba 3: Derrota
//		guardarResultado("turnos", "Laura, Miguel", "DISTRIBUIDOS", false, null);
//}
}
