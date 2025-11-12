package comun;

import java.io.Serializable;

public class Mensaje implements Serializable {
    private static final long serialVersionUID = 1L;
    public static String CONECTAR = "CONECTAR";
    public static String ELEGIR_MODO = "ELEGIR MODO";
    public static String INTENTAR_LETRA = "INTENTAR LETRA";
    public static String DESCONECTAR = "DESCONECTAR";
    
    public static String BIENVENIDA = "BIENVENIDA";
    public static String SOLICITAR_MODO = "SOLICITAR MODO";
    public static String TU_TURNO = "TU TURNO";
    public static String ESPERAR_TURNO = "ESPERAR TURNO";
    public static String ESTADO_JUEGO = "ESTADO JUEGO";
    public static String RESULTADO_INTENTO = "RESULTADO INTENTO";
    public static String PARTIDA_TERMINADA = "PARTIDA TERMINADA";
    public static String ERROR = "ERROR";
    
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
