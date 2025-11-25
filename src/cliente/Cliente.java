package cliente;

import comun.Mensaje;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

//Utilizo dos hilos en el cliente
//1. Escuchar mensajes del servidor
//2. Leer al usuario y envia al servidor, usamos el hilo principal para esto
public class Cliente {
	private String HOST = "localhost";
	private int PUERTO = 5000;
	private boolean esperandoNombre = false;
	private boolean cerrar = false;
	private boolean esperandoContinuar = false;
	// Comenzamos conexión con el servidor, lanzamos primer hilo

	public void conectar() {
		try (Socket socket = new Socket(HOST, PUERTO);
				ObjectOutputStream salida = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());
				Scanner scanner = new Scanner(System.in)) {
			// Creamos y lanzamos hilo para escuchar los mensajes
			Thread hilo = new Thread(new Runnable() {
				@Override
				public void run() {
					recibirMensajes(entrada);
				}
			});
			hilo.setDaemon(true);// Se cierra cuando principal termina
			hilo.start();

			// Hilo principal lee del teclado y envia
			enviarMensajes(salida, scanner);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Mensajes del servidor
	private void recibirMensajes(ObjectInputStream entrada) {
		try {
			while (true) {
				Mensaje mensaje = (Mensaje) entrada.readObject();
				procesarMensajeServidor(mensaje);
			}
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	// Maneja el mensaje del servidor según el tipo que sea
	private void procesarMensajeServidor(Mensaje mensaje) {
		String tipo = mensaje.getTipo();
		String contenido = mensaje.getContenido();
		esperandoContinuar = false;

		switch (tipo) {
		case Mensaje.BIENVENIDA:
			System.out.println(contenido);
			break;

		case Mensaje.SOLICITAR_MODO:
			System.out.println("\n" + contenido);
			if (contenido.toLowerCase().contains("nombre")) {
				esperandoNombre = true;
				System.out.print("Tu nombre: ");
			} else {
				esperandoNombre = false;
				System.out.print("Elige una opción: ");
			}
			break;

		case Mensaje.TU_TURNO:
			System.out.println("\n" + contenido);
			System.out.print("Introduce una letra: ");
			break;

		case Mensaje.ESPERAR_TURNO:
			System.out.println(contenido);
			break;

		case Mensaje.ESTADO_JUEGO:
			System.out.println(contenido);
			if (contenido.contains("Partida finalizada")) {
				System.out.println("Saliendo...");
				cerrar = true;
			}
			break;

		case Mensaje.RESULTADO_INTENTO:
			System.out.println(contenido);
			break;

		case Mensaje.PARTIDA_TERMINADA:
			System.out.println("\n" + contenido);
			break;

		case Mensaje.ERROR:
			System.err.println("Error: " + contenido);
			break;

		case Mensaje.PREGUNTAR_CONTINUAR:
			System.out.println("\n" + contenido);
			System.out.print("Tu respuesta (si/no): ");
			esperandoContinuar = true;
			break;

		default:
			System.err.println("Tipo de mensaje desconocido: " + tipo);
			break;
		}
	}

//Lee del teclado y se envía mensaje al servidor
	private void enviarMensajes(ObjectOutputStream salida, Scanner scanner) {
		try {
			boolean modoElegido = false;
			esperandoNombre = true;

			while (!cerrar) {
				String input = scanner.nextLine();
				if (input.isEmpty()) {
					continue;
				}
				if (esperandoNombre) {
					enviarMensaje(salida, Mensaje.ENVIAR_NOMBRE, input);
					esperandoNombre = false;
					continue;
				}

				if (!modoElegido) {
					// Validar que sea 1, 2 o salir
					if (input.equals("1")) {
						enviarMensaje(salida, Mensaje.ELEGIR_MODO, "turnos");
						modoElegido = true;
						continue;

					} else if (input.equals("2")) {
						enviarMensaje(salida, Mensaje.ELEGIR_MODO, "concurrente");
						modoElegido = true;
						continue;

					} else if (input.equals("salir")) {
						cerrarYSalir(salida);
						return;

					} else {
						// Opción no válida: mostrar mensaje
						System.out.println("\n  Opción no válida: \"" + input + "\"");
						System.out.println("   Escribe: 1 (turnos), 2 (concurrente) o 'salir'");
						System.out.print("\nElige una opción: ");
					}
					continue;
				}
				// Si estamos esperando la respuesta de continuar, forzar solo si/no
				if (esperandoContinuar) {
					String lower = input.trim().toLowerCase();
					if (lower.equals("si") || lower.equals("no")) {
						enviarMensaje(salida, Mensaje.RESPUESTA_CONTINUAR, lower);
						esperandoContinuar = false;
					} else {
						// Mensaje local: insistir hasta que el usuario escriba si/no
						System.out.println("\nRespuesta no válida. Escribe 'si' o 'no'.");
						System.out.print("Tu respuesta (si/no): ");
					}
					continue;
				}
				// Comando: SALIR
				if (input.equalsIgnoreCase("salir")) {
					cerrarYSalir(salida);
					return;
				} else if (input.equalsIgnoreCase("si") || input.equalsIgnoreCase("no")) {
					enviarMensaje(salida, Mensaje.RESPUESTA_CONTINUAR, input.toLowerCase());
				} // Intentar una letra
				else if (input.length() == 1 && Character.isLetter(input.charAt(0))) {
					enviarMensaje(salida, Mensaje.INTENTAR_LETRA, input.toUpperCase());

					// Entrada no válida
				} else {
					System.out.println("\n  Entrada no válida: \"" + input + "\"");

					if (input.length() > 1) {
						System.out.println("   Solo puedes escribir UNA letra");
					} else if (input.length() == 1) {
						System.out.println("   Debes escribir una LETRA (A-Z)");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Crea un objeto y lo envia al servidor
	private void enviarMensaje(ObjectOutputStream salida, String tipo, String contenido) {
		try {
			if (salida != null && !cerrar) {
				Mensaje mensaje = new Mensaje(tipo, contenido);
				salida.writeObject(mensaje);
				salida.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void cerrarYSalir(ObjectOutputStream salida) {
		cerrar = true;
		System.out.println("\nCerrando sesión...");
		enviarMensaje(salida, Mensaje.DESCONECTAR, "");
	}

	// Crea cliente y lo conecta al servidor
	public static void main(String[] args) {
		Cliente cliente = new Cliente();
		cliente.conectar();
	}
}