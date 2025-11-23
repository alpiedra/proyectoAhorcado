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
               "Hola, " + nombreJugador);
           enviarMensaje(Mensaje.SOLICITAR_MODO,
                   "¿Qué modo deseas jugar?\n" +
                   "   1️ Por turnos\n" +
                   "   2️ Concurrente\n" +
                   "Escribe 1 o 2");
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
                // Modo TURNOS
                servidor.unirseModoTurnos(this);
                Juego juego = servidor.getJuegoTurnos(); // Juego compartido
                String estadoInicial = construirEstadoJuego(juego);
            } else if ("concurrente".equals(modoJuego)) {
                // Modo CONCURRENTE
                servidor.unirseModoConcurrente(this);
            }   
       } else if (tipo.equals(Mensaje.INTENTAR_LETRA)) {
             if ("turnos".equals(modoJuego)) {
                // Verificar que sea su turno
                if (!servidor.esTuTurno(this)) {
                    enviarMensaje(Mensaje.ERROR, "No es tu turno");
                    return;
                } // Validar letra
                String letraStr = mensaje.getContenido();
                if (letraStr.isEmpty()) {
                    enviarMensaje(Mensaje.ERROR, "Debes introducir una letra");
                    return;
                }// Tenemos la letra y obtenemos el juego compartido para usarla
                char letra = letraStr.charAt(0);
                Juego juego = servidor.getJuegoTurnos();
                Juego.ResultadoIntento resultado = juego.intentarLetra(letra);
                // Mensaje del resultado
                String mensajeResultado = construirMensajeResultado(resultado, juego);
                servidor.notificarEstadoATodos("\nTurno de: " + nombreJugador + "\n" + mensajeResultado);
                 // Verificar si terminó la partida
                if (resultado.ganado || resultado.perdido) {
                    String mensajeFinal = resultado.ganado
                        ? "¡" + nombreJugador + " ha ganado! La palabra era: " + juego.getPalabraSecreta()
                        : "Se acabaron los intentos. La palabra era: " + juego.getPalabraSecreta();
                    servidor.notificarEstadoATodos(mensajeFinal);    
                    servidor.reiniciarJuego();

                    
                } else {
                    // Si no termina, pasa al siguiente turno
                    servidor.siguienteTurno();
                }
                
            } else if ("concurrente".equals(modoJuego)) {
                procesarLetraConcurrente(mensaje);
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
   private void procesarLetraConcurrente(Mensaje mensaje) {
	    //validamos letra
	    String letraStr = mensaje.getContenido();
	    if (letraStr.isEmpty()) {
	        enviarMensaje(Mensaje.ERROR, "Debes introducir una letra");
	        return;
	    }
	    
	    char letra = letraStr.charAt(0);
	    JuegoConcurrente juego = servidor.getJuegoConcurrente();
	    
	    //Intentar letra
	    // Método es synchronized, así que solo uno entra a la vez
	    JuegoConcurrente.ResultadoConcurrente resultado = 
	        juego.intentarLetra(nombreJugador, letra);
	    
	    String mensajeResultado = nombreJugador + " intentó: " + letra + "\n" +
	                             resultado.mensaje + "\n" +
	                             servidor.construirEstadoConcurrente(juego);
	    
	    servidor.notificarConcurrentesATodos(mensajeResultado);
	    if (resultado.ganador) {
	        String anuncio = nombreJugador + " ha ganado!\n" +
	                       "   Puso la última letra: " + letra + "\n" +
	                       "   Palabra completa: " + juego.getPalabraSecreta() + "\n";
	        servidor.notificarConcurrentesATodos(anuncio);
	        servidor.reiniciarJuegoConcurrente();

	    }
	    
	    else if (resultado.perdido) {
	        String anuncio = "Se acabaron los intentos.\n" +
	                       "Palabra: " + juego.getPalabraSecreta() + "\n" +
	                       "Nadie ganó esta ronda.\n";
	        servidor.notificarConcurrentesATodos(anuncio);
	        servidor.reiniciarJuegoConcurrente();

	}
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