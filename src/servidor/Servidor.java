package servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import comun.Mensaje;

public class Servidor {
    private int puerto = 5000;
    private List<ManejadorCliente> clientes; //Todos los clientes que están conectados
   //Partida y jugadores modo turnos
    private Juego juegoTurnos;
    private List<ManejadorCliente> jugadoresTurnos;
    private int turnoActual;
   //Partida y jugadores modo concurrente
    private JuegoConcurrente juegoConcurrente;
    private List<ManejadorCliente> jugadoresConcurrentes;
    
    public Servidor() {
        this.clientes = new ArrayList<>(); 
        this.juegoTurnos = null;
        this.jugadoresTurnos = new ArrayList<>();
        this.turnoActual = 0;
        this.juegoConcurrente = null;
        this.jugadoresConcurrentes = new ArrayList<>();
    }
    public synchronized JuegoConcurrente getJuegoConcurrente() {
        if (juegoConcurrente == null) {
            juegoConcurrente = new JuegoConcurrente();
        }
        return juegoConcurrente;
    }
    public synchronized void unirseModoConcurrente(ManejadorCliente cliente) {
        jugadoresConcurrentes.add(cliente);
        //Obtener el juego compartido
        JuegoConcurrente juego = getJuegoConcurrente();
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
        
        cliente.enviarMensaje(Mensaje.TU_TURNO, "Empieza");
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
        
        String mensaje = "Nueva partida\n";
        notificarConcurrentesATodos(mensaje);
        String estadoInicial = construirEstadoConcurrente(juegoConcurrente);
        notificarConcurrentesATodos(estadoInicial);
        
        for (ManejadorCliente cliente : jugadoresConcurrentes) {
            cliente.enviarMensaje(Mensaje.TU_TURNO, "Empieza!");
        }
    }
    
    public static void main(String[] args) {
        Servidor servidor = new Servidor();
        servidor.iniciar();
    }
    //Ctreo serverSocket y empieza a aceptar clienets
    public void iniciar() {
       try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            while (true) {
               try{
                   Socket socketCliente = serverSocket.accept();
                   ManejadorCliente manejador = new ManejadorCliente(socketCliente, this);
                   clientes.add(manejador);
                   //creo e inicio hilo nuevo para cada cliente
                   Thread hilo = new Thread(manejador);
                   hilo.start();
               }catch(IOException e) {
            	   e.printStackTrace();
               }
            }
        } catch (IOException e) {
        	e.printStackTrace();
        }
    }   
    //Devuelve el juego compartido o lo inicia
    //Synchronized ya que si varios hilos llaman a este método a la vez se pueden crear
    //muchas instancias del juego y solo queremos una que la compartan
    public synchronized Juego getJuegoTurnos() {
        if (juegoTurnos == null) {
            juegoTurnos = new Juego();
            System.out.println("Nueva partida por turnos creada");
        }
        return juegoTurnos;
    }
    //Añade un cliente y les avisa a los demás
    //Asigna el primer turno, si es el primero, o dice que espere
    //synchronized para evitar que accedan varios hilos y que causen errores
    public synchronized void unirseModoTurnos(ManejadorCliente cliente) {
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
            cliente.notificarTurno(true);
        } else {
            cliente.notificarTurno(false);
        }
    }
    
    //Pasa el turno al siguiente jugador
    //synchronized para evitar que dos hilos cambien el turno a la vez y se produzcan errores
    public synchronized void siguienteTurno() {
        if (jugadoresTurnos.isEmpty()) {
            return;
        }
        turnoActual = (turnoActual + 1) % jugadoresTurnos.size();
        
        ManejadorCliente jugadorActual = jugadoresTurnos.get(turnoActual);
        String nombreActual = jugadorActual.getNombreJugador();
       
        String mensajeTurno = "Turno de: " + nombreActual + "\n";
        notificarEstadoATodos(mensajeTurno);
        //Dice a cada jugador si es su turno o no
        for (int i = 0; i < jugadoresTurnos.size(); i++) {
            ManejadorCliente jugador = jugadoresTurnos.get(i);
            jugador.notificarTurno(i == turnoActual);
        }
    }
    //synchronized para evitar que la lista cambie mientras otro hilo la recorre
    public synchronized void notificarEstadoATodos(String mensaje) {
        for (ManejadorCliente jugador : jugadoresTurnos) {
            jugador.enviarEstado(mensaje);
        }
    }
    //synchronized evita que la lista de jugadores cambie mientras se comprueba si es el turno del cliente
    public synchronized boolean esTuTurno(ManejadorCliente cliente) {
        if (jugadoresTurnos.isEmpty()) {
            return false;
        }
        int indice = jugadoresTurnos.indexOf(cliente);
        return indice == turnoActual;
    }
    //Se usa cuando un cliente se desconecta
    //synchronized para que los clientes y turnos no se modifiquen a la vez
    public synchronized void eliminarCliente(ManejadorCliente cliente) {
        clientes.remove(cliente);
        if (jugadoresTurnos.contains(cliente)) {
            int indice = jugadoresTurnos.indexOf(cliente);
            jugadoresTurnos.remove(cliente);
            //Si era su turno y hay mas jugadores
            if (indice == turnoActual && !jugadoresTurnos.isEmpty()) {
                turnoActual = turnoActual % jugadoresTurnos.size();
                siguienteTurno();
            }
        }
        if (jugadoresConcurrentes.contains(cliente)) {
            jugadoresConcurrentes.remove(cliente);
            String mensaje = cliente.getNombreJugador() + " se ha desconectado.\n" +
                           "Jugadores activos: " + jugadoresConcurrentes.size() + "\n";
            notificarConcurrentesATodos(mensaje);
        }
    }
    //Nueva partida cuando alguien gana o pierde
    //synchronized evita que otros hilos cambien algo, como la lista de jugadores mientras se reinicia
    public synchronized void reiniciarJuego() {
        juegoTurnos = new Juego();
        turnoActual = 0;
        String mensaje = "\nNueva partida\n";
        notificarEstadoATodos(mensaje);
        
       if (!jugadoresTurnos.isEmpty()) {
            ManejadorCliente primerJugador = jugadoresTurnos.get(0);
            String estadoInicial = primerJugador.construirEstadoJuego(juegoTurnos);
            notificarEstadoATodos(estadoInicial);
            siguienteTurno();
        }
    }
}