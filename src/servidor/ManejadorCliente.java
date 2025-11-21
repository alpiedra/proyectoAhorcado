package servidor;
import comun.Mensaje;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
//Cada cliente tiene su propio manejador 
//Se comunica con el cliente

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
    //Trabaja con los streams de entrada y salida
   //Bienvenida, pide datos del juego y empieza el bucle infinito del juego
    @Override
    public void run() {
    	try {
            salida = new ObjectOutputStream(socket.getOutputStream());
            entrada = new ObjectInputStream(socket.getInputStream());
            
           enviarMensaje(Mensaje.BIENVENIDA, 
                "¡Bienvenido al Ahorcado!");
           
           enviarMensaje(Mensaje.SOLICITAR_MODO, "Por favor, ingresa tu nombre:");
           Mensaje mensajeNombre = (Mensaje) entrada.readObject();
           this.nombreJugador = mensajeNombre.getContenido();
           if (nombreJugador.isEmpty()) {nombreJugador = "Jugador";}
           enviarMensaje(Mensaje.ESTADO_JUEGO,
               "Hola, " + nombreJugador + " Espera un momento mientras se prepara el juego");
           
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
    //Recibe mensaje del cliente y hace la acción que corresponde
    private void procesarMensaje(Mensaje mensaje) {
        String tipo = mensaje.getTipo();
        if (tipo.equals(Mensaje.ELEGIR_MODO)) {
            modoJuego = mensaje.getContenido();
            if ("turnos".equals(modoJuego)) {
                servidor.unirseModoTurnos(this);
                Juego juego = servidor.getJuegoTurnos();//Juego compartido
                String estadoInicial = construirEstadoJuego(juego);
                servidor.notificarEstadoATodos(nombreJugador + " se ha unido al modo por turnos.\n" +
                        estadoInicial);
             } else if ("concurrente".equals(modoJuego)) {
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
            //Tenemos la letra y obtenemos el juego compartido para usarla
            char letra = letraStr.charAt(0);
            Juego juego = servidor.getJuegoTurnos();
            Juego.ResultadoIntento resultado = juego.intentarLetra(letra);
            //Mensaje del resultado
            String mensajeResultado = construirMensajeResultado(resultado, juego);
            servidor.notificarEstadoATodos("\nTurno de: " + nombreJugador + "\n" + mensajeResultado);
            if (resultado.ganado || resultado.perdido) {
            	String mensajeFinal = resultado.ganado
                        ? "¡" + nombreJugador + " ha ganado! La palabra era: " + juego.getPalabraSecreta()
                        : "Se acabaron los intentos. La palabra era: " + juego.getPalabraSecreta();
            	servidor.notificarEstadoATodos(mensajeFinal);    
            //Reinicia después de tres hilos, se usa otro hilo para no bloquear
            new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        servidor.reiniciarJuego();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
          } else {
        	  //si no termina pasa al siguiente turno
              servidor.siguienteTurno();
          }
      }
}
    //Envio el objeto mensaje al cliente
    //Synchronized evita que varios hilos envien a la vez mensajes al mismo cliente
   public synchronized void enviarMensaje(String tipo, String contenido) {
       try {
    	   //Serializa y envia el mensaje, con flush() forzamos el envio
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
           enviarMensaje(Mensaje.TU_TURNO, "Es tu turno, " + nombreJugador + " Ingresa una letra:\n");
       } else {
           enviarMensaje(Mensaje.ESPERAR_TURNO, "Esperando tu turno, " + nombreJugador+ "\n" );
       }
   }
   //Se crea String con el estado del juego
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
   //Cierra recursos cuando el cliente se desconecta
     private void cerrarConexion() {
        try {
        	if (entrada != null) { entrada.close();}
            if (salida != null) { salida.close();}
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            servidor.eliminarCliente(this);
        } catch (IOException e) {
           e.printStackTrace();
        }
    }
}