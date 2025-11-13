package servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Servidor {
    private int puerto = 5000;
    private List<ManejadorCliente> clientes;
    private Juego juegoTurnos;
    private List<ManejadorCliente> jugadoresTurnos;
    private int turnoActual;
    
    public Servidor() {
        this.clientes = new ArrayList<>(); 
        this.juegoTurnos = null;
        this.jugadoresTurnos = new ArrayList<>();
        this.turnoActual = 0;
    }
    public static void main(String[] args) {
        Servidor servidor = new Servidor();
        servidor.iniciar();
    }
    public void iniciar() {
       try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            while (true) {
               try{
                   Socket socketCliente = serverSocket.accept();
                   ManejadorCliente manejador = new ManejadorCliente(socketCliente, this);
                   clientes.add(manejador);
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
    public synchronized Juego getJuegoTurnos() {
        if (juegoTurnos == null) {
            juegoTurnos = new Juego();
            System.out.println("Nueva partida por turnos creada");
        }
        return juegoTurnos;
    }
    public synchronized void unirseModoTurnos(ManejadorCliente cliente) {
    	jugadoresTurnos.add(cliente);
    	String mensaje = "\n Jugadores conectados: " + jugadoresTurnos.size() + "\n";
        for (ManejadorCliente j : jugadoresTurnos) {
            mensaje += "   - " + j.getNombreJugador() + "\n";
        }
        notificarEstadoATodos(mensaje);
        
        if (jugadoresTurnos.size() == 1) {
            turnoActual = 0;
            cliente.notificarTurno(true);
        } else {
            cliente.notificarTurno(false);
        }
    }
    public synchronized void siguienteTurno() {
        if (jugadoresTurnos.isEmpty()) {
            return;
        }
        turnoActual = (turnoActual + 1) % jugadoresTurnos.size();
        
        ManejadorCliente jugadorActual = jugadoresTurnos.get(turnoActual);
        String nombreActual = jugadorActual.getNombreJugador();
       
        String mensajeTurno = "Turno de: " + nombreActual + "\n";
        notificarEstadoATodos(mensajeTurno);
        
        for (int i = 0; i < jugadoresTurnos.size(); i++) {
            ManejadorCliente jugador = jugadoresTurnos.get(i);
            jugador.notificarTurno(i == turnoActual);
        }
    }
    public synchronized void notificarEstadoATodos(String mensaje) {
        for (ManejadorCliente jugador : jugadoresTurnos) {
            jugador.enviarEstado(mensaje);
        }
    }
    public synchronized boolean esTuTurno(ManejadorCliente cliente) {
        if (jugadoresTurnos.isEmpty()) {
            return false;
        }
        int indice = jugadoresTurnos.indexOf(cliente);
        return indice == turnoActual;
    }
    public synchronized void eliminarCliente(ManejadorCliente cliente) {
        clientes.remove(cliente);
        if (jugadoresTurnos.contains(cliente)) {
            int indice = jugadoresTurnos.indexOf(cliente);
            jugadoresTurnos.remove(cliente);
            if (indice == turnoActual && !jugadoresTurnos.isEmpty()) {
                turnoActual = turnoActual % jugadoresTurnos.size();
                siguienteTurno();
            }
        }
    }
    public synchronized void reiniciarJuego() {
        juegoTurnos = new Juego();
        turnoActual = 0;
        String mensaje = "\nNueva partida!\n";
        notificarEstadoATodos(mensaje);
        
       if (!jugadoresTurnos.isEmpty()) {
            ManejadorCliente primerJugador = jugadoresTurnos.get(0);
            String estadoInicial = primerJugador.construirEstadoJuego(juegoTurnos);
            notificarEstadoATodos(estadoInicial);
            siguienteTurno();
        }
    }
}