package cliente;

import javax.swing.*;
import comun.Mensaje;
import java.awt.*;

public class PanelBienvenida extends JPanel {
    private ClienteGUI ventanaPrincipal;
    private JTextField campoNombre;
    
    public PanelBienvenida(ClienteGUI ventanaPrincipal) {
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
        gbc.insets = new Insets(10, 10, 10, 10);
        
        
        JLabel titulo = new JLabel("Bienvenido al Ahorcado");
        titulo.setFont(new Font("Lucida Handwriting", Font.PLAIN, 40));
        titulo.setForeground(Color.BLACK);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        panelCentral.add(titulo, gbc);
        
        //nombre
        JLabel lblNombre = new JLabel("Ingresa tu nombre:");
        lblNombre.setFont(new Font("Arial", Font.PLAIN, 18));
        lblNombre.setForeground(Color.BLACK);
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        panelCentral.add(lblNombre, gbc);
        
        //texto
        campoNombre = new JTextField(20);
        campoNombre.setFont(new Font("Arial", Font.PLAIN, 16));
        campoNombre.setBackground(Color.WHITE);
        campoNombre.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 180, 230), 2),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        gbc.gridx = 1;
        panelCentral.add(campoNombre, gbc);
        
        //Botón continuar 
        JButton btnContinuar = new JButton("Continuar");
        btnContinuar.setFont(new Font("Arial", Font.PLAIN, 14));
        btnContinuar.setBackground(new Color(200, 180, 230)); 
        btnContinuar.setForeground(Color.BLACK);
        btnContinuar.setFocusPainted(false);
        btnContinuar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 160, 210), 2),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        btnContinuar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnContinuar.addActionListener(e -> continuarAlModoJuego());
        gbc.gridx = 2;
        panelCentral.add(btnContinuar, gbc);
        
        add(panelCentral, BorderLayout.CENTER);
        
        //Botón salir
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
    
    private void continuarAlModoJuego() {
        String nombre = campoNombre.getText().trim();
        
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Por favor, ingresa tu nombre", 
                "Nombre requerido", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        ventanaPrincipal.setNombreJugador(nombre);
        ventanaPrincipal.conectarAlServidor();
        ventanaPrincipal.enviarMensaje(Mensaje.ENVIAR_NOMBRE, nombre);
    }
}