package comun;

import java.io.Serializable;

//Clase usada para representar los mensajes intercambiados entre cliente y servidor
//Cada mensaje tiene un tipo, que es la acción a realizar, y un contenido, que es el mensaje que se envía
//Implementa Serializable para poder tratarlo como objetos
public class Mensaje implements Serializable {
	private static final long serialVersionUID = 1L;
	// Mensajes que envía el cliente al servidor:
	public static final String CONECTAR = "conectar";
	public static final String ELEGIR_MODO = "elegir_modo";
	public static final String INTENTAR_LETRA = "intentar_letra";
	public static final String DESCONECTAR = "desconectar";
	public static final String ENVIAR_NOMBRE = "enviar_nombre";
	public static final String RESPUESTA_CONTINUAR = "respuesta_continuar";

	// Mensajes que envía el servidor al cliente:
	public static final String BIENVENIDA = "bienvenida";
	public static final String SOLICITAR_MODO = "solicitar_modo";
	public static final String TU_TURNO = "tu_turno";
	public static final String ESPERAR_TURNO = "esperar_turno";
	public static final String ESTADO_JUEGO = "estado_juego";
	public static final String RESULTADO_INTENTO = "resultado_intento";
	public static final String PARTIDA_TERMINADA = "partida_terminada";
	public static final String ERROR = "error";
	public static final String PREGUNTAR_CONTINUAR = "preguntar_continuar";
	public static final String LETRA_USADA = "letra_usada";
	
	private String tipo;
	private String contenido;

	public Mensaje(String tipo, String contenido) {
		this.tipo = tipo;
		this.contenido = contenido;
	}

	public String getTipo() {
		return tipo;
	}

	public String getContenido() {
		return contenido;
	}

	public String toString() {
		return "Mensaje{tipo=" + tipo + ", contenido='" + contenido + "'}";
	}
}
