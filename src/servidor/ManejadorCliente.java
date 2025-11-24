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
    private volatile boolean cerrado = false;
    
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
           while (!cerrado) {
        	   try {
        	        Mensaje mensaje = (Mensaje) entrada.readObject();
        	        
        	        if (mensaje.getTipo().equals(Mensaje.DESCONECTAR)) {
        	            break;
        	        }
        	        
        	        procesarMensaje(mensaje);
        	        
        	    } catch (EOFException e) {
        	        e.printStackTrace();
        	        break;
        	    }
            }
        } catch (IOException | ClassNotFoundException e) {
           e.printStackTrace();
        } finally {
            cerrarConexion();
        }
    }
    //Recibe mensaje del cliente y hace la acción que corresponde
    private void procesarMensaje(Mensaje mensaje) throws IOException {
        String tipo = mensaje.getTipo();
        if (tipo.equals(Mensaje.ELEGIR_MODO)) {
        	modoJuego = mensaje.getContenido();

            if ("turnos".equals(modoJuego)) {
                servidor.unirseModoTurnos(this);

            } else if ("concurrente".equals(modoJuego)) {
                servidor.unirseModoConcurrente(this);
            }
            return;
        }
        if (tipo.equals(Mensaje.RESPUESTA_CONTINUAR)) {
            String respuesta = mensaje.getContenido();

            if ("turnos".equals(modoJuego)) {
                servidor.procesarRespuestaContinuarTurnos(this, respuesta);
            } else if ("concurrente".equals(modoJuego)) {
                servidor.procesarRespuestaContinuarConcurrente(this, respuesta);
            }
            return;
        }
        if (tipo.equals(Mensaje.INTENTAR_LETRA)) {

            if ("turnos".equals(modoJuego)) {

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

                servidor.notificarEstadoATodos(
                        "\nTurno de: " + nombreJugador + "\n" +
                        construirMensajeResultado(resultado, juego)
                );

                if (resultado.ganado || resultado.perdido) {

                    servidor.notificarEstadoATodos(
                            resultado.ganado
                                    ? "¡" + nombreJugador + " ha ganado! La palabra era: " + juego.getPalabraSecreta()
                                    : "Se acabaron los intentos. La palabra era: " + juego.getPalabraSecreta()
                    );

                    servidor.preguntarContinuarTurnos();
                } else {
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
           if (salida != null && !cerrado) {
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
   private void procesarLetraConcurrente(Mensaje mensaje) throws IOException {
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
	        servidor.preguntarContinuarConcurrente();

	    }
	    
	    else if (resultado.perdido) {
	        String anuncio = "Se acabaron los intentos.\n" +
	                       "Palabra: " + juego.getPalabraSecreta() + "\n" +
	                       "Nadie ganó esta ronda.\n";
	        servidor.notificarConcurrentesATodos(anuncio);
	        servidor.preguntarContinuarConcurrente();

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
	    if (cerrado) return;
	    cerrado = true;
	    
	    try { if (entrada != null) entrada.close(); } catch (Exception e) {}
	    try { if (salida != null) salida.close(); } catch (Exception e) {}
	    try { if (socket != null && !socket.isClosed()) socket.close(); } catch (Exception e) {}
	    
	    servidor.eliminarCliente(this);
	}
   public void cerrarConexionPublica() {
	    cerrarConexion();
	}
    }