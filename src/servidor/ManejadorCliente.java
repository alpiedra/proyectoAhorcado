package servidor;
import comun.Mensaje;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ManejadorCliente implements Runnable {
    private Socket socket;
    private Servidor servidor;
    private String modoJuego;
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private String nombreJugador;
    
   public ManejadorCliente(Socket socket, Servidor servidor) {
        this.socket = socket;
        this.servidor = servidor;
    }
    
    @Override
    public void run() {
    	try {
            salida = new ObjectOutputStream(socket.getOutputStream());
            entrada = new ObjectInputStream(socket.getInputStream());
            
           enviarMensaje(Mensaje.BIENVENIDA, 
                "¡Bienvenido al Ahorcado!");
           
           enviarMensaje(Mensaje.SOLICITAR_MODO, "Por favor, ingresa tu nombre:");
           Mensaje mensajeNombre = (Mensaje) entrada.readObject();
           this.nombreJugador = mensajeNombre.getContenido().trim();
           if (nombreJugador.isEmpty()) nombreJugador = "Jugador";

           enviarMensaje(Mensaje.ESTADO_JUEGO,
               "¡Hola, " + nombreJugador + "! Espera un momento mientras se prepara el juego...");
           
           enviarMensaje(Mensaje.SOLICITAR_MODO,
                   "\n¿Qué modo deseas jugar?\n" +
                   "   1️ Por turnos\n" +
                   "   2️ Concurrente\n" +
                   "Escribe 1 o 2 y presiona ENTER:");
           while (true) {
                Mensaje mensaje = (Mensaje) entrada.readObject();
                procesarMensaje(mensaje);
            }
        } catch (IOException | ClassNotFoundException e) {
           e.printStackTrace();
        } finally {
            cerrarConexion();
        }
    }
    
    private void procesarMensaje(Mensaje mensaje) {
        String tipo = mensaje.getTipo();
        if (tipo.equals(Mensaje.ELEGIR_MODO)) {
            modoJuego = mensaje.getContenido();
            if ("TURNOS".equals(modoJuego)) {
                servidor.unirseModoTurnos(this);
                Juego juego = servidor.getJuegoTurnos();
                String estadoInicial = construirEstadoJuego(juego);
                servidor.notificarEstadoATodos(nombreJugador + " se ha unido al modo POR TURNOS.\n" +
                        estadoInicial);
             } else if ("CONCURRENTE".equals(modoJuego)) {
                enviarMensaje(Mensaje.TU_TURNO, "¡Empieza a jugar, "+nombreJugador + "!");
            }
          } else if (tipo.equals(Mensaje.INTENTAR_LETRA)) {
            if (!"TURNOS".equals(modoJuego)) {
                return;
            }
            if (!servidor.esTuTurno(this)) {
                enviarMensaje(Mensaje.ERROR, "No es tu turno");
                return;
            }
            String letraStr = mensaje.getContenido();
            if (letraStr.isEmpty()) {
                enviarMensaje(Mensaje.ERROR, "Debes introducir una letra");
                return;
            }
            char letra = letraStr.charAt(0);
            Juego juego = servidor.getJuegoTurnos();
            Juego.ResultadoIntento resultado = juego.intentarLetra(letra);
            String mensajeResultado = construirMensajeResultado(resultado, juego);
            servidor.notificarEstadoATodos("\nTurno de: " + nombreJugador + "\n" + mensajeResultado);
            if (resultado.ganado || resultado.perdido) {
            	String mensajeFinal = resultado.ganado
                        ? "¡" + nombreJugador + " ha ganado! La palabra era: " + juego.getPalabraSecreta()
                        : "Se acabaron los intentos. La palabra era: " + juego.getPalabraSecreta();
            	servidor.notificarEstadoATodos(mensajeFinal);    
            
            new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        servidor.reiniciarJuego();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
          } else {
              servidor.siguienteTurno();
          }
      }
}
    
   public synchronized void enviarMensaje(String tipo, String contenido) {
       try {
           if (salida != null) {
               Mensaje mensaje = new Mensaje(tipo, contenido);
               salida.writeObject(mensaje);
               salida.flush();
           }
       } catch (IOException e) {
          e.printStackTrace();
       }
   }
   public void notificarTurno(boolean esTuTurno) {
	   if (esTuTurno) {
           enviarMensaje(Mensaje.TU_TURNO, "\nEs tu turno, " + nombreJugador + "! Ingresa una letra:");
       } else {
           enviarMensaje(Mensaje.ESPERAR_TURNO, "\nEsperando tu turno, " + nombreJugador + "...");
       }
   }
   public String construirEstadoJuego(Juego juego) {
       StringBuilder sb = new StringBuilder();
       sb.append("   Palabra: ").append(juego.getPalabraActual()).append("\n");
       sb.append("   Intentos restantes: ").append(juego.getIntentosRestantes()).append("\n");
       sb.append("   Letras usadas: ").append(juego.getLetrasUsadas()).append("\n");
       return sb.toString();
   }
   private String construirMensajeResultado(Juego.ResultadoIntento resultado, Juego juego) {
       StringBuilder sb = new StringBuilder();
       sb.append("\n").append(resultado.mensaje).append("\n");
       sb.append(construirEstadoJuego(juego));
       return sb.toString();
   }
   public void enviarEstado(String estado) {
       enviarMensaje(Mensaje.ESTADO_JUEGO, estado);
   }
   public String getNombreJugador() {
	    return nombreJugador != null ? nombreJugador : "Jugador";
   }
     private void cerrarConexion() {
        try {
        	if (entrada != null) entrada.close();
            if (salida != null) salida.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            servidor.eliminarCliente(this);
        } catch (IOException e) {
           e.printStackTrace();
        }
    }
}