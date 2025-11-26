package cliente;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import comun.Mensaje;

public class PanelJuego extends JPanel {
	private ClienteGUI ventanaPrincipal;

	private JComboBox<String> comboLetras;
	private JTextArea areaLetrasUsadas;
	private JLabel lblPalabra;
	private JLabel lblIntentos;
	private JLabel lblMensaje;
	private JButton btnEnviar;
	private PanelAhorcado panelAhorcado;

	private ArrayList<String> letrasDisponibles;

	public PanelJuego(ClienteGUI ventanaPrincipal) {
		this.ventanaPrincipal = ventanaPrincipal;
		inicializarLetras();
		configurarPanel();
		crearComponentes();
	}

	private void configurarPanel() {
		setLayout(new BorderLayout(10, 10));
		setBackground(new Color(245, 240, 255));
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	}

	private void inicializarLetras() {
		letrasDisponibles = new ArrayList<>();
		for (char c = 'A'; c <= 'Z'; c++) {
			letrasDisponibles.add(String.valueOf(c));
		}
	}

	private void crearComponentes() {
		// Titulo
		JPanel panelSuperior = new JPanel(new BorderLayout());
		panelSuperior.setBackground(new Color(245, 240, 255));

		JLabel titulo = new JLabel("AHORCADO - " + ventanaPrincipal.getModoJuego().toUpperCase());
		titulo.setFont(new Font("Lucida Handwriting", Font.PLAIN, 22));
		titulo.setForeground(Color.BLACK);
		titulo.setHorizontalAlignment(SwingConstants.CENTER);
		panelSuperior.add(titulo, BorderLayout.CENTER);
		// Salir
		JButton btnSalir = new JButton("Salir");
		btnSalir.setFont(new Font("Arial", Font.PLAIN, 12));
		btnSalir.setBackground(new Color(230, 200, 220));
		btnSalir.setForeground(Color.BLACK);
		btnSalir.setFocusPainted(false);
		btnSalir.setBorder(
				BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(210, 180, 200), 2),
						BorderFactory.createEmptyBorder(4, 10, 4, 10)));
		btnSalir.setCursor(new Cursor(Cursor.HAND_CURSOR));
		btnSalir.addActionListener(e -> ventanaPrincipal.salir());
		panelSuperior.add(btnSalir, BorderLayout.EAST);

		add(panelSuperior, BorderLayout.NORTH);

		JPanel panelCentral = new JPanel(new GridLayout(1, 2, 10, 10));
		panelCentral.setBackground(new Color(245, 240, 255));

		// Panel izquierdo: Dibujo del ahorcado
		panelAhorcado = new PanelAhorcado();
		panelAhorcado.setBackground(Color.WHITE);
		panelAhorcado
				.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(200, 180, 230), 2),
						"Ahorcado", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
						javax.swing.border.TitledBorder.DEFAULT_POSITION, new Font("Arial", Font.PLAIN, 14)));
		panelCentral.add(panelAhorcado);

		// Panel derecho: Información del juego
		JPanel panelInfo = new JPanel();
		panelInfo.setLayout(new BoxLayout(panelInfo, BoxLayout.Y_AXIS));
		panelInfo.setBackground(Color.WHITE);
		panelInfo
				.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(200, 180, 230), 2),
						"Información", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
						javax.swing.border.TitledBorder.DEFAULT_POSITION, new Font("Arial", Font.PLAIN, 14)));

		// Palabra
		lblPalabra = new JLabel("Palabra: _ _ _ _ _");
		lblPalabra.setFont(new Font("Monospaced", Font.PLAIN, 20));
		lblPalabra.setAlignmentX(Component.CENTER_ALIGNMENT);
		lblPalabra.setPreferredSize(new Dimension(350, 60));
		lblPalabra.setMaximumSize(new Dimension(350, 60));
		lblPalabra.setHorizontalAlignment(SwingConstants.CENTER);
		panelInfo.add(lblPalabra);

		panelInfo.add(Box.createVerticalStrut(20));

		// Intentos restantes
		lblIntentos = new JLabel("Intentos restantes: 10");
		lblIntentos.setFont(new Font("Arial", Font.PLAIN, 16));
		lblIntentos.setAlignmentX(Component.CENTER_ALIGNMENT);
		panelInfo.add(lblIntentos);

		panelInfo.add(Box.createVerticalStrut(20));

		// Letras usadas
		JLabel lblTituloUsadas = new JLabel("Letras usadas:");
		lblTituloUsadas.setFont(new Font("Arial", Font.PLAIN, 14));
		lblTituloUsadas.setAlignmentX(Component.CENTER_ALIGNMENT);
		panelInfo.add(lblTituloUsadas);

		areaLetrasUsadas = new JTextArea(3, 20);
		areaLetrasUsadas.setEditable(false);
		areaLetrasUsadas.setFont(new Font("Arial", Font.PLAIN, 14));
		areaLetrasUsadas.setLineWrap(true);
		areaLetrasUsadas.setWrapStyleWord(true);
		JScrollPane scrollUsadas = new JScrollPane(areaLetrasUsadas);
		panelInfo.add(scrollUsadas);

		panelInfo.add(Box.createVerticalStrut(20));

		// Estado
		lblMensaje = new JLabel("Esperando...");
		lblMensaje.setFont(new Font("Arial", Font.ITALIC, 14));
		lblMensaje.setForeground(new Color(100, 80, 120));
		lblMensaje.setAlignmentX(Component.CENTER_ALIGNMENT);
		panelInfo.add(lblMensaje);

		panelCentral.add(panelInfo);

		add(panelCentral, BorderLayout.CENTER);

		// Combobox y botón
		JPanel panelInferior = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
		panelInferior.setBackground(new Color(245, 240, 255));

		JLabel lblElige = new JLabel("Elige una letra:");
		lblElige.setFont(new Font("Arial", Font.PLAIN, 16));
		lblElige.setForeground(Color.BLACK);
		panelInferior.add(lblElige);

		comboLetras = new JComboBox<>(letrasDisponibles.toArray(new String[0]));
		comboLetras.setFont(new Font("Arial", Font.PLAIN, 18));
		comboLetras.setPreferredSize(new Dimension(60, 40));
		comboLetras.setBackground(Color.WHITE);
		panelInferior.add(comboLetras);

		btnEnviar = new JButton("Enviar Letra");
		btnEnviar.setFont(new Font("Arial", Font.PLAIN, 14));
		btnEnviar.setBackground(new Color(200, 180, 230));
		btnEnviar.setForeground(Color.BLACK);
		btnEnviar.setFocusPainted(false);
		btnEnviar.setBorder(
				BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(180, 160, 210), 2),
						BorderFactory.createEmptyBorder(8, 15, 8, 15)));
		btnEnviar.setCursor(new Cursor(Cursor.HAND_CURSOR));
		btnEnviar.addActionListener(e -> enviarLetra());
		panelInferior.add(btnEnviar);

		add(panelInferior, BorderLayout.SOUTH);
	}

	private void enviarLetra() {
		if (comboLetras.getItemCount() == 0) {
	        JOptionPane.showMessageDialog(this, 
	            "No quedan letras disponibles", 
	            "Aviso", 
	            JOptionPane.WARNING_MESSAGE);
	        return;
		}
		String letra = (String) comboLetras.getSelectedItem();
	    ventanaPrincipal.enviarMensaje(Mensaje.INTENTAR_LETRA, letra);
	}
	public void eliminarLetraDelCombo(String letra) {
	    comboLetras.removeItem(letra);
	}

	// Para actualizar
	public void actualizarEstado(String estado) {
	    // El estado viene con formato:
	    // "Palabra: _ A _ A\nIntentos restantes: 8\nLetras usadas: [A, E]"
	     String[] lineas = estado.split("\n");
	    
	    for (String linea : lineas) {
	        linea = linea.trim(); //Eliminar espacios al inicio/final
	        if (linea.contains("Palabra:")) {
	            // Extraer solo la parte después de "Palabra:"
	            int indice = linea.indexOf(":");
	            if (indice != -1 && indice + 1 < linea.length()) {
	                String palabra = linea.substring(indice + 1).trim();
	                lblPalabra.setText("Palabra: " + palabra);
	            }
	            
	        } else if (linea.contains("Intentos restantes:")) {
	            int indice = linea.indexOf(":");
	            if (indice != -1 && indice + 1 < linea.length()) {
	                String intentos = linea.substring(indice + 1).trim();
	                lblIntentos.setText("Intentos restantes: " + intentos);
	                
	                // Actualizar dibujo del ahorcado
	                try {
	                    int numIntentos = Integer.parseInt(intentos);
	                    int errores = 10 - numIntentos;
	                    panelAhorcado.setErrores(errores);
	                } catch (NumberFormatException e) {
	                    e.printStackTrace();
	                }
	            }
	            
	        } else if (linea.contains("Letras usadas:")) {
	            int indice = linea.indexOf(":");
	            if (indice != -1 && indice + 1 < linea.length()) {
	                String letras = linea.substring(indice + 1).trim();
	                areaLetrasUsadas.setText(letras);
	            }
	        }
	    }
	}

	public void mostrarMensaje(String mensaje) {
		lblMensaje.setText(mensaje);
	}

	public void mostrarResultado(String resultado) {
		JOptionPane.showMessageDialog(this, resultado, "Resultado", JOptionPane.INFORMATION_MESSAGE);
	}

	public void habilitarJuego(boolean habilitar) {
		comboLetras.setEnabled(habilitar);
		btnEnviar.setEnabled(habilitar);
	}

	// Método para reiniciar el panel cuando se juega otra ronda
	public void reiniciar() {
		// Reiniciar letras disponibles
		comboLetras.removeAllItems();
		for (String letra : letrasDisponibles) {
			comboLetras.addItem(letra);
		}

		// Reiniciar interfaz
		lblPalabra.setText("Palabra: _ _ _ _ _");
		lblIntentos.setText("Intentos restantes: 10");
		areaLetrasUsadas.setText("");
		lblMensaje.setText("Esperando...");
		panelAhorcado.setErrores(0);

		// Habilitar controles
		habilitarJuego(true);
	}
}