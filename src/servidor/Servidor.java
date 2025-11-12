package servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Servidor {
    private int puerto = 6666;
    private List<ManejadorCliente> clientes;
    private Juego juegoTurnos;
    
    public Servidor() {
        this.clientes = new ArrayList<>(); 
        this.juegoTurnos = null;
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
    
    public synchronized void eliminarCliente(ManejadorCliente cliente) {
        clientes.remove(cliente);
    }
}