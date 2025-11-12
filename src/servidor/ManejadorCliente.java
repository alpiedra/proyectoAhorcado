package servidor;
import comun.Mensaje;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ManejadorCliente implements Runnable {
    private Socket socket;
    private Servidor servidor;
    private String modoJuego;
    
   public ManejadorCliente(Socket socket, Servidor servidor) {
        this.socket = socket;
        this.servidor = servidor;
    }
    
    @Override
    public void run() {
        try (
            ObjectOutputStream salida = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream())
        ) {
            enviarMensaje(salida, Mensaje.BIENVENIDA, 
                "¡Bienvenido al Ahorcado!");
            
            enviarMensaje(salida, Mensaje.SOLICITAR_MODO,
                "¿Qué modo quieres jugar?\n1. Por turnos\n2. Concurrente");
            
            while (true) {
                Mensaje mensaje = (Mensaje) entrada.readObject();
                procesarMensaje(mensaje, salida);
            }
        
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    private void procesarMensaje(Mensaje mensaje, ObjectOutputStream salida) {
       String tipo = mensaje.getTipo();
        if (tipo.equals(Mensaje.ELEGIR_MODO)) {
            modoJuego = mensaje.getContenido();
            if ("TURNOS".equals(modoJuego)) {
                enviarMensaje(salida, Mensaje.ESPERAR_TURNO,
                    "Esperando tu turno...");
            } else if ("CONCURRENTE".equals(modoJuego)) {
                enviarMensaje(salida, Mensaje.TU_TURNO,
                    "Empieza");
            }
            
        } else if (tipo.equals(Mensaje.INTENTAR_LETRA)) {
            // TODO: Procesar letra (días 3-4)
            
        } else if (tipo.equals(Mensaje.DESCONECTAR)) {
            cerrarSocket();
            
        } else {
            enviarMensaje(salida, Mensaje.ERROR, "Mensaje no reconocido");
        }
    }
    
   private void enviarMensaje(ObjectOutputStream salida, String tipo, String contenido) {
        try {
            Mensaje mensaje = new Mensaje(tipo, contenido);
            salida.writeObject(mensaje);
            salida.flush();
        } catch (IOException e) {
            System.out.println("Error al enviar: " + e.getMessage());
        }
    }
     private void cerrarSocket() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            servidor.eliminarCliente(this);
        } catch (IOException e) {
           e.printStackTrace();
        }
    }
}