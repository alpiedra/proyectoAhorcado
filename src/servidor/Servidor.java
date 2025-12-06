package servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import comun.Mensaje;

public class Servidor {
	private int puerto = 5000;
	private List<ManejadorCliente> clientes = new ArrayList<>(); // Todos los clientes que están conectados
	// Partida y jugadores modo turnos
	private Juego juegoTurnos = null;
	private List<ManejadorCliente> jugadoresTurnos = new ArrayList<>();
	private int turnoActual = 0;
	private Map<ManejadorCliente, Boolean> respuestasContinuarTurnos = new HashMap<>();;
	private int jugadoresQueRespondieron = 0;

	// Partida y jugadores modo concurrente
	private JuegoConcurrente juegoConcurrente = null;
	private List<ManejadorCliente> jugadoresConcurrentes = new ArrayList<>();
	private Map<ManejadorCliente, Boolean> respuestasContinuarConcurrente = new HashMap<>();;
	private int jugadoresQueRespondieroConc = 0;
	private int totalJugadoresConcurrentesRonda = 0;

	ExecutorService pool = Executors.newCachedThreadPool();

	public static void main(String[] args) {
		Servidor servidor = new Servidor();
		servidor.iniciar();
	}

	// Ctreo serverSocket y empieza a aceptar clienets
	public void iniciar() {
		try (ServerSocket serverSocket = new ServerSocket(puerto)) {
			while (true) {
				Socket cliente = serverSocket.accept();
				ManejadorCliente manejador = new ManejadorCliente(cliente, this);
				clientes.add(manejador);
				pool.execute(manejador); // Utilizamos pool de hilos
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Devuelve el juego compartido o lo inicia
	// Synchronized ya que si varios hilos llaman a este método a la vez se pueden
	// crear muchas instancias del juego y solo queremos una que la compartan
	public synchronized Juego getJuegoTurnos() throws IOException {
		if (juegoTurnos == null) {
			juegoTurnos = new Juego();
			System.out.println("Nueva partida por turnos creada\n");
		}
		return juegoTurnos;
	}

	// Añade un cliente y les avisa a los demás
	// Asigna el primer turno, si es el primero, o dice que espere
	// synchronized para evitar que accedan varios hilos y que causen errores
	public synchronized void unirseModoTurnos(ManejadorCliente cliente) throws IOException {
		jugadoresTurnos.add(cliente);
		String mensaje = "\n Jugadores conectados: " + jugadoresTurnos.size() + "\n";
		for (ManejadorCliente j : jugadoresTurnos) {
			mensaje += "   - " + j.getNombreJugador() + "\n";
		}
		notificarEstadoATodos(mensaje);

		Juego juego = getJuegoTurnos();
		String estadoInicial = cliente.construirEstadoJuego(juego);
		notificarEstadoATodos(estadoInicial);

		if (jugadoresTurnos.size() == 1) {
			turnoActual = 0;
			notificarTurnosATodos();
		} else {
			cliente.notificarTurno(false);
		}
	}

	// Método para notificar turnos a todos correctamente
	private void notificarTurnosATodos() {
		if (jugadoresTurnos.isEmpty()) {
			return;
		}
		ManejadorCliente jugadorActual = jugadoresTurnos.get(turnoActual);
		String nombreActual = jugadorActual.getNombreJugador();
		String mensajeTurno = "-Turno de: " + nombreActual + "\n";
		notificarEstadoATodos(mensajeTurno);
		for (int i = 0; i < jugadoresTurnos.size(); i++) {
			ManejadorCliente jugador = jugadoresTurnos.get(i);
			jugador.notificarTurno(i == turnoActual);
		}
	}

	// Pasa el turno al siguiente jugador
	// synchronized para evitar que dos hilos cambien el turno a la vez y se
	// produzcan errores
	public synchronized void siguienteTurno() {
		if (jugadoresTurnos.isEmpty()) {
			return;
		}
		turnoActual = (turnoActual + 1) % jugadoresTurnos.size();
		notificarTurnosATodos();
	}

	// synchronized para evitar que la lista cambie mientras otro hilo la recorre
	public synchronized void notificarEstadoATodos(String mensaje) {
		for (ManejadorCliente jugador : jugadoresTurnos) {
			jugador.enviarEstado(mensaje);
		}
	}

	// synchronized evita que la lista de jugadores cambie mientras se comprueba si
	// es el turno del cliente
	public synchronized boolean esTuTurno(ManejadorCliente cliente) {
		if (jugadoresTurnos.isEmpty()) {
			return false;
		}
		int indice = jugadoresTurnos.indexOf(cliente);
		return indice == turnoActual;
	}

	// Se usa cuando un cliente se desconecta
	// synchronized para que los clientes y turnos no se modifiquen a la vez
	public synchronized void eliminarCliente(ManejadorCliente cliente) {
		clientes.remove(cliente);
		if (jugadoresTurnos.contains(cliente)) {
			int indice = jugadoresTurnos.indexOf(cliente);
			jugadoresTurnos.remove(cliente);
			respuestasContinuarTurnos.remove(cliente);

			String mensaje = cliente.getNombreJugador() + " se ha desconectado.\n" + "Jugadores activos: "
					+ jugadoresTurnos.size() + "\n";
			notificarEstadoATodos(mensaje);

			// Ajustar turno si es necesario
			if (!jugadoresTurnos.isEmpty()) {
				if (indice <= turnoActual) {
					turnoActual = Math.max(0, turnoActual - 1);
				}
				turnoActual = turnoActual % jugadoresTurnos.size();
				notificarTurnosATodos();
			}
		}
		if (jugadoresConcurrentes.contains(cliente)) {
			jugadoresConcurrentes.remove(cliente);
			respuestasContinuarConcurrente.remove(cliente);
			String mensaje = cliente.getNombreJugador() + " se ha desconectado.\n" + "Jugadores activos: "
					+ jugadoresConcurrentes.size() + "\n";
			notificarConcurrentesATodos(mensaje);
		}
	}

	// Nueva partida cuando alguien gana o pierde
	// synchronized evita que otros hilos cambien algo, como la lista de jugadores
	// mientras se reinicia
	public synchronized void reiniciarJuego() throws IOException {
		juegoTurnos = new Juego();
		turnoActual = 0;
		respuestasContinuarTurnos.clear();
		jugadoresQueRespondieron = 0;

		String mensaje = "Nueva partida\n";
		notificarEstadoATodos(mensaje);

		if (!jugadoresTurnos.isEmpty()) {
			ManejadorCliente primerJugador = jugadoresTurnos.get(0);
			String estadoInicial = primerJugador.construirEstadoJuego(juegoTurnos);
			notificarEstadoATodos(estadoInicial);
			notificarTurnosATodos();
		}
	}

	// Se muestra elo mensaje al finalizar una partida
	public synchronized void preguntarContinuarTurnos() {
		respuestasContinuarTurnos.clear();
		jugadoresQueRespondieron = 0;
		String pregunta = "¿Jugar otra ronda?\n" + "   'si' para continuar\n" + "   'no' para salir\n";

		for (ManejadorCliente jugador : jugadoresTurnos) {
			jugador.enviarMensaje(Mensaje.PREGUNTAR_CONTINUAR, pregunta);
		}
	}

	public synchronized void procesarRespuestaContinuarTurnos(ManejadorCliente cliente, String respuesta)
			throws IOException {

		// Registrar la respuesta del jugador
		boolean quiereContinuar = "si".equalsIgnoreCase(respuesta);
		respuestasContinuarTurnos.put(cliente, quiereContinuar);
		jugadoresQueRespondieron++;

		if (!quiereContinuar) {
			cliente.enviarMensaje(Mensaje.ESTADO_JUEGO, "Has salido de la partida. ¡Hasta pronto!\n");

		} else {
			cliente.enviarMensaje(Mensaje.ESTADO_JUEGO, "Esperando a los demás jugadores...\n");
		}

		// Esperar a que todos respondan
		if (jugadoresQueRespondieron >= jugadoresTurnos.size()) {
			// Guardar resultado de la partida antes de procesar
			if (juegoTurnos != null && juegoTurnos.haTerminado()) {
				StringBuilder jugadores = new StringBuilder();
				for (int i = 0; i < jugadoresTurnos.size(); i++) {
					jugadores.append(jugadoresTurnos.get(i).getNombreJugador());
					if (i < jugadoresTurnos.size() - 1) {
						jugadores.append(", ");
					}
				}
				boolean victoria = juegoTurnos.getIntentosRestantes() > 0;
				GuardarPartida.guardarResultado("turnos", jugadores.toString(), juegoTurnos.getPalabraSecreta(),
						victoria, victoria ? "Jugador" : null);
			}

			// Eliminar jugadores que dijeron "no"
			List<ManejadorCliente> aEliminar = new ArrayList<>();
			for (Map.Entry<ManejadorCliente, Boolean> entry : respuestasContinuarTurnos.entrySet()) {
				if (!entry.getValue()) {
					aEliminar.add(entry.getKey());
				}
			}

			for (ManejadorCliente c : aEliminar) {
				jugadoresTurnos.remove(c);
				c.desconectar();
			}

			// Reiniciar si quedan jugadores
			if (!jugadoresTurnos.isEmpty()) {
				String mensaje = "\n¡Todos han respondido! Iniciando nueva partida...\n";
				notificarEstadoATodos(mensaje);
				reiniciarJuego();
			} else {
				System.out.println("No quedan jugadores en modo turnos");
			}
		}
	}

	public synchronized JuegoConcurrente getJuegoConcurrente() throws IOException {
		if (juegoConcurrente == null) {
			juegoConcurrente = new JuegoConcurrente();
		}
		return juegoConcurrente;
	}

	public synchronized void unirseModoConcurrente(ManejadorCliente cliente) {
		jugadoresConcurrentes.add(cliente);
		// Obtener el juego compartido

		try {
			JuegoConcurrente juego;
			juego = getJuegoConcurrente();
			String mensaje = "Modo concurrente\n";
			cliente.enviarEstado(mensaje);
			String jugadoresActivos = "Jugadores jugando: " + jugadoresConcurrentes.size() + "\n";
			for (ManejadorCliente j : jugadoresConcurrentes) {
				jugadoresActivos += "   - " + j.getNombreJugador() + "\n";
			}
			notificarConcurrentesATodos(jugadoresActivos);

			// Mostrar estado actual del juego
			String estadoInicial = construirEstadoConcurrente(juego);
			cliente.enviarEstado(estadoInicial);
			cliente.enviarMensaje(Mensaje.TU_TURNO, "Empieza!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public synchronized void notificarConcurrentesATodos(String mensaje) {
		for (ManejadorCliente jugador : jugadoresConcurrentes) {
			jugador.enviarEstado(mensaje);
		}
	}

	public String construirEstadoConcurrente(JuegoConcurrente juego) {
		StringBuilder sb = new StringBuilder();
		sb.append("  Palabra: ").append(juego.getPalabraActual()).append("\n");
		sb.append("  Intentos restantes: ").append(juego.getIntentosRestantes()).append("\n");
		sb.append("  Letras usadas: ").append(juego.getLetrasUsadas()).append("\n");
		return sb.toString();
	}

	public synchronized void reiniciarJuegoConcurrente() {
		juegoConcurrente = new JuegoConcurrente();
		respuestasContinuarConcurrente.clear();
		jugadoresQueRespondieroConc = 0;

		String mensaje = "Nueva partida!\n Escribe 'salir' si quieres abandonar la partida \n";
		notificarConcurrentesATodos(mensaje);
		String estadoInicial = construirEstadoConcurrente(juegoConcurrente);
		notificarConcurrentesATodos(estadoInicial);

		for (ManejadorCliente cliente : jugadoresConcurrentes) {
			cliente.enviarMensaje(Mensaje.TU_TURNO, "Empieza!");
		}
	}

	// se hace la pregunta cuando se acaba una partida
	public synchronized void preguntarContinuarConcurrente() {
		totalJugadoresConcurrentesRonda = jugadoresConcurrentes.size();
		respuestasContinuarConcurrente.clear();
		jugadoresQueRespondieroConc = 0;
		String pregunta = "¿Jugar otra ronda?\n" + "   'si' para continuar\n" + "   'no' para salir\n";

		for (ManejadorCliente jugador : jugadoresConcurrentes) {
			jugador.enviarMensaje(Mensaje.PREGUNTAR_CONTINUAR, pregunta);
		}
	}

	public synchronized void procesarRespuestaContinuarConcurrente(ManejadorCliente cliente, String respuesta) {

		// Registrar respuesta
		boolean quiereContinuar = "si".equalsIgnoreCase(respuesta);
		respuestasContinuarConcurrente.put(cliente, quiereContinuar);
		jugadoresQueRespondieroConc++;

		if (!quiereContinuar) {
			cliente.enviarMensaje(Mensaje.ESTADO_JUEGO, "Has salido de la partida. ¡Hasta pronto!\n");
		} else {
			cliente.enviarMensaje(Mensaje.ESTADO_JUEGO, "Esperando a los demás jugadores...\n");
		}

		// Esperar a que todos respondan
		if (jugadoresQueRespondieroConc >= totalJugadoresConcurrentesRonda) {
			// Guardar resultado
			if (juegoConcurrente != null && juegoConcurrente.haTerminado()) {
				StringBuilder jugadores = new StringBuilder();
				for (int i = 0; i < jugadoresConcurrentes.size(); i++) {
					jugadores.append(jugadoresConcurrentes.get(i).getNombreJugador());
					if (i < jugadoresConcurrentes.size() - 1) {
						jugadores.append(", ");
					}
				}
				String ganador = juegoConcurrente.getGanador();
				boolean victoria = ganador != null;
				GuardarPartida.guardarResultado("concurrente", jugadores.toString(),
						juegoConcurrente.getPalabraSecreta(), victoria, ganador);
			}

			// Eliminar jugadores que dijeron "no"
			List<ManejadorCliente> aEliminar = new ArrayList<>();
			for (Map.Entry<ManejadorCliente, Boolean> entry : respuestasContinuarConcurrente.entrySet()) {
				if (!entry.getValue()) {
					aEliminar.add(entry.getKey());
				}
			}

			for (ManejadorCliente c : aEliminar) {
				jugadoresConcurrentes.remove(c);
				c.desconectar();
			}

			// Reiniciar si quedan jugadores
			if (!jugadoresConcurrentes.isEmpty()) {
				String mensaje = "\n¡Todos han respondido! Iniciando nueva partida...\n";
				notificarConcurrentesATodos(mensaje);
				reiniciarJuegoConcurrente();
			} else {
				System.out.println("No quedan jugadores en modo concurrente");
			}
		}
	}

	public synchronized void notificarLetraUsada(String letra) {
		for (ManejadorCliente jugador : jugadoresTurnos) {
			jugador.enviarMensaje(Mensaje.LETRA_USADA, letra);
		}
	}

	public synchronized void notificarLetraUsadaConcurrente(String letra) {
		for (ManejadorCliente jugador : jugadoresConcurrentes) {
			jugador.enviarMensaje(Mensaje.LETRA_USADA, letra);
		}
	}
}
