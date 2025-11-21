package cliente;

import comun.Mensaje;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;
//Utilizo dos hilos en el cliente
//1. Escuchar mensajes del servidor
//2. Leer al usuario, usamos el hilo principal para esto
public class Cliente {
    private String HOST = "localhost";
    private int PUERTO = 5000;
    private boolean esperandoNombre = false; 
    //Comenzamos conexión con el servidor, lanzamos primer hilo
    
    public void conectar() {
        try (
            Socket socket = new Socket(HOST, PUERTO);
            ObjectOutputStream salida = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());
            Scanner scanner = new Scanner(System.in)
        ) {
        	//Creamos y lanzamos hilo para escuchar los mensajes
        	Thread hilo = new Thread(new Runnable() {
        	    @Override
        	    public void run() {
        	        recibirMensajes(entrada);
        	    }
        	});
        	hilo.start();
        	
        	//Hilo principal lee del teclado y envia
            enviarMensajes(salida, scanner);
            
            hilo.join();//Bloqueo hasta que termine
            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
    }
    //Mensajes del servidor
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
    
    //Maneja el mensaje del servidor según el tipo que sea
    private void procesarMensajeServidor(Mensaje mensaje) {
        String tipo = mensaje.getTipo();
        String contenido = mensaje.getContenido();

        if (tipo.equals(Mensaje.BIENVENIDA)) {
            System.out.println(contenido);

        } else if (tipo.equals(Mensaje.SOLICITAR_MODO)) {
            System.out.println("\n" + contenido);

            if (contenido.toLowerCase().contains("nombre")) {
                esperandoNombre = true;  
                System.out.print("Tu nombre: ");
            } else {
                esperandoNombre = false;  
                System.out.print("Elige una opción: ");
            }

        } else if (tipo.equals(Mensaje.TU_TURNO)) {
            System.out.println("\n" + contenido);
            System.out.print("Introduce una letra: ");

        } else if (tipo.equals(Mensaje.ESPERAR_TURNO)) {
            System.out.println(contenido);

        } else if (tipo.equals(Mensaje.ESTADO_JUEGO)) {
            System.out.println(contenido);

        } else if (tipo.equals(Mensaje.RESULTADO_INTENTO)) {
            System.out.println(contenido);

        } else if (tipo.equals(Mensaje.PARTIDA_TERMINADA)) {
            System.out.println("\n" + contenido);
            System.out.println("\nEscribe 'salir' para terminar.");

        } else if (tipo.equals(Mensaje.ERROR)) {
            System.err.println("Error: " + contenido);
        }

        System.out.flush();//Forzamos que se muestre el mensaje
    }
//Lee del teclado y se envía mensaje al servidor
    private void enviarMensajes(ObjectOutputStream salida, Scanner scanner) {
        try {
            while (true) {
                String input = scanner.nextLine();          
                if (input.isEmpty()) {
                    continue;
                }
                if (input.equalsIgnoreCase("salir")) {
                    enviarMensaje(salida, Mensaje.DESCONECTAR, "");
                    break;

                } else if (esperandoNombre) {
                    enviarMensaje(salida, Mensaje.ENVIAR_NOMBRE, input);
                    esperandoNombre = false;

                } else if (input.equals("1")) {
                    enviarMensaje(salida, Mensaje.ELEGIR_MODO, "turnos");

                } else if (input.equals("2")) {
                    enviarMensaje(salida, Mensaje.ELEGIR_MODO, "concurrente");

                } else if (input.length() == 1 && Character.isLetter(input.charAt(0))) {
                    enviarMensaje(salida, Mensaje.INTENTAR_LETRA, input.toUpperCase());

                } else {
                    System.out.println("Comando no reconocido. Escribe 'salir' para terminar.");
                }

            }
        } catch (Exception e) {
           e.printStackTrace();        }
    }
   // Crea un objeto y lo envia al servidor
    private void enviarMensaje(ObjectOutputStream salida, String tipo, String contenido) {
        try {
            Mensaje mensaje = new Mensaje(tipo, contenido);
            salida.writeObject(mensaje);
            salida.flush();
        } catch (IOException e) {
            System.err.println("Error al enviar mensaje: " + e.getMessage());
        }
    }
    //Crea cliente y lo conecta al servidor
    public static void main(String[] args) {
        Cliente cliente = new Cliente();
        cliente.conectar();
    }
}