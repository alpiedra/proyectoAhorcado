package cliente;

import comun.Mensaje;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Cliente {
    private String HOST = "localhost";
    private int PUERTO = 5000;
    
    public void conectar() {
        try (
            Socket socket = new Socket(HOST, PUERTO);
            ObjectOutputStream salida = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());
            Scanner scanner = new Scanner(System.in)
        ) {
        	Thread hiloReceptor = new Thread(new Runnable() {
        	    @Override
        	    public void run() {
        	        recibirMensajes(entrada);
        	    }
        	});
        	hiloReceptor.start();
            enviarMensajes(salida, scanner);
            hiloReceptor.join();
            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
    }
    
    private void recibirMensajes(ObjectInputStream entrada) {
        try {
            while (true) {
                Mensaje mensaje = (Mensaje) entrada.readObject();
                procesarMensajeServidor(mensaje);
            }
        }catch (IOException | ClassNotFoundException e) {
           e.printStackTrace();
        }
    }
    
    private void procesarMensajeServidor(Mensaje mensaje) {
        String tipo = mensaje.getTipo();
        String contenido = mensaje.getContenido();
        
        if (tipo.equals(Mensaje.BIENVENIDA)) {
            System.out.println(contenido);
            
        } else if (tipo.equals(Mensaje.SOLICITAR_MODO)) {
            System.out.println("\n" + contenido);
            System.out.print("Elige una opción: ");
            
        } else if (tipo.equals(Mensaje.TU_TURNO)) {
            System.out.println("\n⭐ " + contenido);
            System.out.print("Introduce una letra: ");
            
        } else if (tipo.equals(Mensaje.ESPERAR_TURNO)) {
            System.out.println(contenido);
            
        } else if (tipo.equals(Mensaje.ESTADO_JUEGO)) {
            System.out.println("\n" + contenido);
            
        } else if (tipo.equals(Mensaje.RESULTADO_INTENTO)) {
            System.out.println(contenido);
            
        } else if (tipo.equals(Mensaje.PARTIDA_TERMINADA)) {
            System.out.println("\n" + contenido);
            
        } else if (tipo.equals(Mensaje.ERROR)) {
            System.err.println("Error: " + contenido);
        }
    }
    
    private void enviarMensajes(ObjectOutputStream salida, Scanner scanner) {
        try {
            while (true) {
                String input = scanner.nextLine().trim();
                
                if (input.isEmpty()) {
                    continue;
                }
                if (input.equals("1")) {
                    enviarMensaje(salida, Mensaje.ELEGIR_MODO, "TURNOS");
                    
                } else if (input.equals("2")) {
                    enviarMensaje(salida, Mensaje.ELEGIR_MODO, "CONCURRENTE");
                    
                } else if (input.length() == 1 && Character.isLetter(input.charAt(0))) {
                    enviarMensaje(salida, Mensaje.INTENTAR_LETRA, input.toUpperCase());
                    
                } else if (input.equalsIgnoreCase("salir")) {
                    enviarMensaje(salida, Mensaje.DESCONECTAR, "");
                    break;
                    
                } else {
                    System.out.println("Comando no reconocido. Escribe 'salir' para terminar.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error al enviar: " + e.getMessage());
        }
    }
    
    private void enviarMensaje(ObjectOutputStream salida, String tipo, String contenido) {
        try {
            Mensaje mensaje = new Mensaje(tipo, contenido);
            salida.writeObject(mensaje);
            salida.flush();
        } catch (IOException e) {
            System.err.println("Error al enviar mensaje: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        Cliente cliente = new Cliente();
        cliente.conectar();
    }
}