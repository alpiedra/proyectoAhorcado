package cliente;

import javax.swing.*;
import java.awt.*;
import comun.Mensaje;

public class PanelModoJuego extends JPanel {
    private ClienteGUI ventanaPrincipal;
    
    public PanelModoJuego(ClienteGUI ventanaPrincipal) {
        this.ventanaPrincipal = ventanaPrincipal;
        configurarPanel();
        crearComponentes();
    }
    
    private void configurarPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 240, 255)); 
    }
    
    private void crearComponentes() {
        JPanel panelCentral = new JPanel(new GridBagLayout());
        panelCentral.setBackground(new Color(245, 240, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        
        //Saludo
        JLabel lblSaludo = new JLabel("Hola, " + ventanaPrincipal.getNombreJugador() + "!");
        lblSaludo.setFont(new Font("Arial", Font.PLAIN, 28));
        lblSaludo.setForeground(Color.BLACK);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panelCentral.add(lblSaludo, gbc);
        
        JLabel titulo = new JLabel("Elige el modo de juego");
        titulo.setFont(new Font("Arial", Font.PLAIN, 20));
        titulo.setForeground(Color.BLACK);
        gbc.gridy = 1;
        gbc.insets = new Insets(10, 15, 30, 15);
        panelCentral.add(titulo, gbc);
        
        //Botón Turnos
        JButton btnTurnos = new JButton("Jugar por Turnos");
        btnTurnos.setFont(new Font("Arial", Font.PLAIN, 18));
        btnTurnos.setPreferredSize(new Dimension(280, 70));
        btnTurnos.setBackground(new Color(200, 180, 230)); 
        btnTurnos.setForeground(Color.BLACK);
        btnTurnos.setFocusPainted(false);
        btnTurnos.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 160, 210), 3),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        btnTurnos.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTurnos.addActionListener(e -> elegirModo("turnos"));
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(15, 15, 15, 15);
        panelCentral.add(btnTurnos, gbc);
        
        //Botón Concurrente
        JButton btnConcurrente = new JButton("Jugar Concurrente");
        btnConcurrente.setFont(new Font("Arial", Font.PLAIN, 18));
        btnConcurrente.setPreferredSize(new Dimension(280, 70));
        btnConcurrente.setBackground(new Color(220, 200, 240)); 
        btnConcurrente.setForeground(Color.BLACK);
        btnConcurrente.setFocusPainted(false);
        btnConcurrente.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 180, 220), 3),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        btnConcurrente.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnConcurrente.addActionListener(e -> elegirModo("concurrente"));
        gbc.gridx = 1;
        panelCentral.add(btnConcurrente, gbc);
        
        add(panelCentral, BorderLayout.CENTER);
        
        //Salir
        JPanel panelInferior = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        panelInferior.setBackground(new Color(245, 240, 255));
        
        JButton btnSalir = new JButton("Salir");
        btnSalir.setFont(new Font("Arial", Font.PLAIN, 14));
        btnSalir.setBackground(new Color(230, 200, 220)); 
        btnSalir.setForeground(Color.BLACK);
        btnSalir.setFocusPainted(false);
        btnSalir.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 180, 200), 2),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        btnSalir.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSalir.addActionListener(e -> ventanaPrincipal.salir());
        panelInferior.add(btnSalir);
        
        add(panelInferior, BorderLayout.SOUTH);
    }
    
    private void elegirModo(String modo) {
        ventanaPrincipal.setModoJuego(modo);
        ventanaPrincipal.enviarMensaje(Mensaje.ELEGIR_MODO, modo);
        ventanaPrincipal.mostrarPanelJuego();
    }
}