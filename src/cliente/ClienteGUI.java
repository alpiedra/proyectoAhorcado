package cliente;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import comun.Mensaje;

public class ClienteGUI extends JFrame {
    private String HOST = "localhost";
    private int PUERTO = 5000;
    
    //Conexión
    private Socket socket;
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    
    //Paneles
    private JPanel panelActual;
    private PanelBienvenida panelBienvenida;
    private PanelModoJuego panelModoJuego;
    private PanelJuego panelJuego;
    private PanelContinuar panelContinuar;
    
    //Datos del juego
    private String nombreJugador;
    private String modoJuego;
    private boolean partidaIniciada = false;
    private StringBuilder mensajePartidaFinalizada = new StringBuilder();
    
    public ClienteGUI() {
        configurarVentana();
        mostrarPanelBienvenida();
    }
    
    private void configurarVentana() {
        setTitle("Ahorcado");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
    }
    
    //Paneles
    public void mostrarPanelBienvenida() {
        panelBienvenida = new PanelBienvenida(this);
        cambiarPanel(panelBienvenida);
    }
    
    public void mostrarPanelModoJuego() {
        panelModoJuego = new PanelModoJuego(this);
        cambiarPanel(panelModoJuego);
    }
    
    public void mostrarPanelJuego() {
        panelJuego = new PanelJuego(this);
        cambiarPanel(panelJuego);
        partidaIniciada = true;
    }
    
    public void mostrarPanelContinuar(String mensaje) {
        panelContinuar = new PanelContinuar(this, mensaje);
        cambiarPanel(panelContinuar);
    }
    
    private void cambiarPanel(JPanel nuevoPanel) {
        if (panelActual != null) {
            remove(panelActual);
        }
        panelActual = nuevoPanel;
        add(panelActual);
        revalidate();
        repaint();
    }
    
    //Realizar conexión con el servidor
    
    public void conectarAlServidor() {
        try {
            socket = new Socket(HOST, PUERTO);
            salida = new ObjectOutputStream(socket.getOutputStream());
            entrada = new ObjectInputStream(socket.getInputStream());
            
            // Iniciar hilo para recibir mensajes
            Thread hiloReceptor = new Thread(this::recibirMensajes);
            hiloReceptor.setDaemon(true);
            hiloReceptor.start();
            
        } catch (IOException e) {
            e.printStackTrace();
            }
    }
    
    private void recibirMensajes() {
        try {
            while (true) {
                Mensaje mensaje = (Mensaje) entrada.readObject();
                procesarMensajeServidor(mensaje);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    private void procesarMensajeServidor(Mensaje mensaje) {
        String tipo = mensaje.getTipo();
        String contenido = mensaje.getContenido();
        
        // Usar SwingUtilities para actualizar la interfaz desde el hilo de red
        SwingUtilities.invokeLater(() -> {
            switch (tipo) {
                case Mensaje.BIENVENIDA:
                    System.out.println("Bienvenida recibida");
                    break;
                    
                case Mensaje.SOLICITAR_MODO:
                    if (contenido.toLowerCase().contains("modo")) {
                        System.out.println("Mostrando panel de modo");
                        mostrarPanelModoJuego();
                    }
                    break;
                    
                case Mensaje.TU_TURNO:
                    System.out.println("Es tu turno");
                    if (panelJuego != null) {
                        panelJuego.habilitarJuego(true);
                        panelJuego.mostrarMensaje("¡Es tu turno!");
                    }
                    break;
                    
                case Mensaje.ESPERAR_TURNO:
                    System.out.println("Esperando turno");
                    if (panelJuego != null) {
                        panelJuego.habilitarJuego(false);
                        panelJuego.mostrarMensaje("Esperando tu turno...");
                    }
                    break;
                    
                case Mensaje.ESTADO_JUEGO:
                    System.out.println("Estado del juego recibido");
                    if (!partidaIniciada && (contenido.contains("Jugadores") || 
                        contenido.contains("Palabra:") || 
                        contenido.contains("jugando"))) {
                        mostrarPanelJuego();
                    }
                    
                    if (panelJuego != null) {
                        panelJuego.actualizarEstado(contenido);
                        // Guardar mensaje si la partida ha terminado
                        if (contenido.toLowerCase().contains("ganó") || 
                            contenido.toLowerCase().contains("ganado") ||
                            contenido.toLowerCase().contains("acabaron los intentos") ||
                            contenido.toLowerCase().contains("perdiste")) {
                            mensajePartidaFinalizada.append(contenido).append("\n");
                        }
                    }
                    break;
                    
                case Mensaje.RESULTADO_INTENTO:
                    System.out.println("Resultado de intento");
                    if (panelJuego != null) {
                        panelJuego.mostrarMensaje(contenido);
                    }
                    break;
                    
                case Mensaje.PARTIDA_TERMINADA:
                    System.out.println("Partida terminada");
                    mensajePartidaFinalizada.append(contenido).append("\n");
                    if (panelJuego != null) {
                        panelJuego.mostrarResultado(contenido);
                    }
                    break;
                    
                case Mensaje.PREGUNTAR_CONTINUAR:
                    System.out.println("Preguntar continuar");
                    partidaIniciada = false;
                    String mensajeFinal;
                    if (mensajePartidaFinalizada.length() > 0) {
                        mensajeFinal = mensajePartidaFinalizada.toString();
                    } else {
                        mensajeFinal = contenido;
                    }
                    
                    mostrarPanelContinuar(mensajeFinal);
                    mensajePartidaFinalizada.setLength(0); // Limpiar para la próxima
                    break;
                    
                case Mensaje.ERROR:
                    System.out.println("Error: " + contenido);
                    JOptionPane.showMessageDialog(this, contenido, 
                        "Error", JOptionPane.ERROR_MESSAGE);
                    break;
                    
                default:
                    System.out.println("Mensaje no reconocido: " + tipo);
                    break;
            }
        });
    }
    
    //Envio de mensajes
    public void enviarMensaje(String tipo, String contenido) {
        try {
            if (salida != null) {
                Mensaje mensaje = new Mensaje(tipo, contenido);
                salida.writeObject(mensaje);
                salida.flush();
                System.out.println("Mensaje enviado: " + tipo + " - " + contenido); // DEBUG
            } else {
                System.err.println("ERROR: salida es null");
            }
        } catch (IOException e) {
            System.err.println("Error al enviar mensaje: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    //Getters y setters
    public void setNombreJugador(String nombre) {
        this.nombreJugador = nombre;
    }
    
    public String getNombreJugador() {
        return nombreJugador != null ? nombreJugador : "Jugador";
    }
    
    public void setModoJuego(String modo) {
        this.modoJuego = modo;
    }
    
    public String getModoJuego() {
        return modoJuego != null ? modoJuego : "Modo";
    }
    
    //Método para reiniciar cuando se continúa jugando
    public void reiniciarPartida() {
        partidaIniciada = false;
    }
    
    public void salir() {
        enviarMensaje(Mensaje.DESCONECTAR, "");
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClienteGUI gui = new ClienteGUI();
            gui.setVisible(true);
        });
    }
}