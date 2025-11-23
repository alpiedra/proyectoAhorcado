package comun;

import java.io.Serializable;
//Clase usada para representar los mensajes intercambiados entre cliente y servidor
//Cada mensaje tiene un tipo, que es como la acción a realizar, y un contenido, que es el mensaje que se envía
//Implementa Serializable para poder tratarlo como objetos
public class Mensaje implements Serializable {
    private static final long serialVersionUID = 1L;
   //Mensajes que envía el cliente al servidor:
    public static String CONECTAR="coonectar";
    public static String ELEGIR_MODO="elegir_modo";
    public static String INTENTAR_LETRA="intentar_linea";
    public static String DESCONECTAR="desconectar";
    public static String ENVIAR_NOMBRE="enviar_nombre";

    //Mensajes que envía el servidor al cliente:
    public static String BIENVENIDA="bienvenida";
    public static String SOLICITAR_MODO="solicitar_modo";
    public static String TU_TURNO="tu_turno";
    public static String ESPERAR_TURNO="esperar_turno";
    public static String ESTADO_JUEGO="estado_juego";
    public static String RESULTADO_INTENTO="resultado_intento";
    public static String PARTIDA_TERMINADA="partida_terminada";
    public static String ERROR="error";
  
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
