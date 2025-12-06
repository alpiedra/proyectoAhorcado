package cliente;

import comun.Mensaje;

import java.io.EOFException;
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
	private volatile boolean cerrar = false;
	private boolean esperandoContinuar = false;

	// Comenzamos conexión con el servidor, lanzamos primer hilo
	// Crea cliente y lo conecta al servidor
	public static void main(String[] args) {
		Cliente cliente = new Cliente();
		cliente.conectar();
	}

	public void conectar() {
		try (Socket socket = new Socket(HOST, PUERTO);
				ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
				Scanner sc = new Scanner(System.in)) {
			// Creamos y lanzamos hilo para escuchar los mensajes
			Thread hilo = new Thread(new Runnable() {
				@Override
				public void run() {
					recibirMensajes(ois);
				}
			});
			hilo.setDaemon(true);// Se cierra cuando principal termina
			hilo.start();

			// Hilo principal lee del teclado y envia
			enviarMensajes(oos, sc);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Mensajes del servidor
	private void recibirMensajes(ObjectInputStream ois) {
		try {
			while (!cerrar) {
				Mensaje mensaje = (Mensaje) ois.readObject();
				procesarMensajeServidor(mensaje);
				
				// Si recibimos orden de desconectar, salir del bucle
				if (cerrar) {
					break;
				}
			}
		} catch (EOFException e) {
			// El servidor cerró la conexión
			if (!cerrar) {
				System.out.println("\nEl servidor cerró la conexión.");
			}
		} catch (IOException e) {
			if (!cerrar) {
			e.printStackTrace();
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			// Forzar salida cuando el servidor cierra la conexión
			if (cerrar) {
				System.exit(0);
			}
		}
	}


	// Maneja el mensaje del servidor según el tipo que sea
	private void procesarMensajeServidor(Mensaje mensaje) {
		String tipo = mensaje.getTipo();
		String contenido = mensaje.getContenido();

		switch (tipo) {
		case Mensaje.BIENVENIDA:
			System.out.println(contenido);
			break;

		case Mensaje.SOLICITAR_MODO:
			System.out.println(contenido + "\n");
			if (contenido.toLowerCase().contains("nombre")) {
				this.esperandoNombre = true;
				this.esperandoContinuar = false;
				System.out.print("Tu nombre: \n");
			} else {
				this.esperandoNombre = false;
				this.esperandoContinuar = false;
				System.out.print("Elige una opción: \n");
			}
			break;

		case Mensaje.TU_TURNO:
			this.esperandoContinuar = false;
			System.out.println(contenido + "\n");
			System.out.print("Introduce una letra: \n");
			break;

		case Mensaje.ESPERAR_TURNO:
			this.esperandoContinuar = false;
			System.out.println(contenido);
			break;

		case Mensaje.ESTADO_JUEGO:
			System.out.println(contenido + "\n");
			if (contenido.contains("Partida finalizada") | contenido.contains("Has salido de la partida")) {
				this.cerrar = true;
				
			}
			break;

		case Mensaje.RESULTADO_INTENTO:
			System.out.println(contenido + "\n");
			break;

		case Mensaje.PARTIDA_TERMINADA:
			System.out.println(contenido + "\n");
			break;

		case Mensaje.ERROR:
			System.err.println("Error: " + contenido + "\n");
			break;

		case Mensaje.PREGUNTAR_CONTINUAR:
			System.out.println(contenido + "\n");
			System.out.print("Tu respuesta (si/no): \n");
			this.esperandoContinuar = true;
			break;
		case Mensaje.DESCONECTAR:
		    this.cerrar = true;
		    return;


		default:
			break;
		}
	}

//Lee del teclado y se envía mensaje al servidor
	private void enviarMensajes(ObjectOutputStream oos, Scanner sc) {
		boolean modoElegido = false;
		this.esperandoNombre = true;

		while (!this.cerrar && sc.hasNextLine()) {
			String s = sc.nextLine();
			if (this.cerrar) {
				break;
			}
			if (s.isEmpty()) {
				continue;
			}
			if (this.esperandoNombre) {
				enviarMensaje(oos, Mensaje.ENVIAR_NOMBRE, s);
				this.esperandoNombre = false;
				continue;
			}

			if (!modoElegido) {
				// Validar que sea 1, 2 o salir
				if (s.equals("1")) {
					enviarMensaje(oos, Mensaje.ELEGIR_MODO, "turnos");
					modoElegido = true;
					continue;

				} else if (s.equals("2")) {
					enviarMensaje(oos, Mensaje.ELEGIR_MODO, "concurrente");
					modoElegido = true;
					continue;

				} else if (s.equals("salir")) {
					cerrarYSalir(oos);
					return;

				} else {
					// Opción no válida: mostrar mensaje
					System.out.println("  Opción no válida: \"" + s + "\"");
					System.out.println("   Escribe: 1 (turnos), 2 (concurrente) o 'salir'");
					System.out.print("\nElige una opción: \n");
				}
				continue;
			}
			// Si estamos esperando la respuesta de continuar, forzar solo si/no
			if (this.esperandoContinuar) {
				String lower = s.trim().toLowerCase();
				if (lower.equals("si") || lower.equals("no")) {
					enviarMensaje(oos, Mensaje.RESPUESTA_CONTINUAR, lower);
					this.esperandoContinuar = false;
				} else {
					// Mensaje local: insistir hasta que el usuario escriba si/no
					System.out.println("Respuesta no válida. Escribe 'si' o 'no'.\n");
					System.out.print("Tu respuesta (si/no): \n");
				}
				continue;
			}
			// Comando: SALIR
			if (s.equalsIgnoreCase("salir")) {
				cerrarYSalir(oos);
				return;
			} // Intentar una letra
			else if (s.length() == 1 && Character.isLetter(s.charAt(0))) {
				enviarMensaje(oos, Mensaje.INTENTAR_LETRA, s.toUpperCase());

				// Entrada no válida
			} else {
				System.out.println("  Entrada no válida: \"" + s + "\"\n");

				if (s.length() > 1) {
					System.out.println("   Solo puedes escribir UNA letra\n");
				} else if (s.length() == 1) {
					System.out.println("   Debes escribir una LETRA (A-Z)\n");
				}
			}
		}
	}

	// Crea un objeto y lo envia al servidor
	private void enviarMensaje(ObjectOutputStream oos, String tipo, String contenido) {
		try {
			if (oos != null && !this.cerrar) {
				Mensaje mensaje = new Mensaje(tipo, contenido);
				oos.writeObject(mensaje);
				oos.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void cerrarYSalir(ObjectOutputStream oos) {
		this.cerrar = true;
		System.out.println("Cerrando sesión...\n");
		enviarMensaje(oos, Mensaje.DESCONECTAR, "");
	}

}