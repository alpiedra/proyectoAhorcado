package cliente;

import javax.swing.*;
import java.awt.*;

public class PanelAhorcado extends JPanel {
    private int errores = 0;
    
    public PanelAhorcado() {
        setPreferredSize(new Dimension(300, 400));
        setBackground(Color.WHITE);
    }
    
    public void setErrores(int errores) {
        this.errores = errores;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setStroke(new BasicStroke(3));
        
        if (errores >= 1) {
            g2d.drawLine(20, 350, 180, 350);
        }
        
        if (errores >= 2) {
            g2d.drawLine(60, 350, 60, 50);
        }
        
        if (errores >= 3) {
            g2d.drawLine(60, 50, 150, 50);
        }
        
        if (errores >= 4) {
            g2d.drawLine(150, 50, 150, 80);
        }
        
         if (errores >= 5) {
            g2d.drawOval(130, 80, 40, 40);
        }
        
        if (errores >= 6) {
            g2d.drawLine(150, 120, 150, 200);
        }
        
        if (errores >= 7) {
            g2d.drawLine(150, 140, 120, 170);
        }
        
        if (errores >= 8) {
            g2d.drawLine(150, 140, 180, 170);
        }
        
        if (errores >= 9) {
            g2d.drawLine(150, 200, 130, 250);
        }
        
        if (errores >= 10) {
            g2d.drawLine(150, 200, 170, 250);
        }
    }
}