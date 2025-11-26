package cliente;

import javax.swing.*;
import java.awt.*;
import comun.Mensaje;

public class PanelContinuar extends JPanel {
    private ClienteGUI ventanaPrincipal;
    private String mensajeResultado;
    
    public PanelContinuar(ClienteGUI ventanaPrincipal, String mensajeCompleto) {
        this.ventanaPrincipal = ventanaPrincipal;
        this.mensajeResultado = mensajeCompleto;
        configurarPanel();
        crearComponentes();
    }
    
    private void configurarPanel() {
        setLayout(new GridBagLayout());
        setBackground(new Color(245, 240, 255)); 
    }
    
    private void crearComponentes() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        
        //Titulo
        JLabel titulo = new JLabel("Partida Finalizada");
        titulo.setFont(new Font("Lucida Handwriting", Font.PLAIN, 32));
        titulo.setForeground(Color.BLACK);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(titulo, gbc);
        
        //Resultado
        JTextArea areaResultado = new JTextArea();
        areaResultado.setText(formatearMensaje(mensajeResultado));
        areaResultado.setEditable(false);
        areaResultado.setFont(new Font("Arial", Font.PLAIN, 16));
        areaResultado.setBackground(Color.WHITE);
        areaResultado.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 180, 230), 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        areaResultado.setWrapStyleWord(true);
        areaResultado.setLineWrap(true);
        areaResultado.setRows(6);
        areaResultado.setColumns(40);
        
        JScrollPane scrollResultado = new JScrollPane(areaResultado);
        scrollResultado.setPreferredSize(new Dimension(500, 150));
        gbc.gridy = 1;
        add(scrollResultado, gbc);
        
        //Pregunta
        JLabel lblPregunta = new JLabel("¿Quieres jugar otra partida?");
        lblPregunta.setFont(new Font("Arial", Font.PLAIN, 22));
        lblPregunta.setForeground(Color.BLACK);
        gbc.gridy = 2;
        gbc.insets = new Insets(25, 15, 15, 15);
        add(lblPregunta, gbc);
        
        //Botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 0));
        panelBotones.setBackground(new Color(245, 240, 255));
        
        // Botón si
        JButton btnSi = new JButton("Sí");
        btnSi.setFont(new Font("Arial", Font.PLAIN, 18));
        btnSi.setPreferredSize(new Dimension(150, 60));
        btnSi.setBackground(new Color(200, 180, 230)); 
        btnSi.setForeground(Color.BLACK);
        btnSi.setFocusPainted(false);
        btnSi.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 160, 210), 3),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        btnSi.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSi.addActionListener(e -> responder("si"));
        panelBotones.add(btnSi);
        
        // Botón no
        JButton btnNo = new JButton("No");
        btnNo.setFont(new Font("Arial", Font.PLAIN, 18));
        btnNo.setPreferredSize(new Dimension(150, 60));
        btnNo.setBackground(new Color(230, 200, 220));
        btnNo.setForeground(Color.BLACK);
        btnNo.setFocusPainted(false);
        btnNo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 180, 200), 3),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        btnNo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnNo.addActionListener(e -> responder("no"));
        panelBotones.add(btnNo);
        
        gbc.gridy = 3;
        gbc.insets = new Insets(15, 15, 15, 15);
        add(panelBotones, gbc);
    }
    
    private String formatearMensaje(String mensaje) {
         StringBuilder resultado = new StringBuilder();
         String mensajeLimpio = mensaje
            .replace("¿Jugar otra ronda?", "")
            .replace("'si' para continuar", "")
            .replace("'no' para salir", "")
            .replace("\n¿Jugar otra ronda?", "")
            .replace("   'si' para continuar\n", "")
            .replace("   'no' para salir\n", "")
            .trim();
        
        if (!mensajeLimpio.isEmpty()) {
            resultado.append(mensajeLimpio);
        }
        
        // Si después de todo no hay nada, poner mensaje genérico
        if (resultado.length() == 0) {
            return "🎮 La partida ha finalizado";
        }
        
        return resultado.toString();
    }
    
    private void responder(String respuesta) {
        ventanaPrincipal.enviarMensaje(Mensaje.RESPUESTA_CONTINUAR, respuesta);
        
        if (respuesta.equals("si")) {
            ventanaPrincipal.reiniciarPartida();
        } else {
            ventanaPrincipal.salir();
        }
    }
}