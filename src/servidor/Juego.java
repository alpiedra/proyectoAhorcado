package servidor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

public class Juego {
	public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Juego juego = new Juego();
        
        System.out.println("Juego de Ahorcado");
        System.out.println("Palabra: " + juego.getPalabraActual());
        System.out.println("Intentos: " + juego.getIntentosRestantes());
        
        while (!juego.haTerminado()) {
            System.out.print("\nIntroduce una letra: ");
            char letra = sc.next().charAt(0);
            
            ResultadoIntento resultado = juego.intentarLetra(letra);
            
            System.out.println(resultado.mensaje);
            System.out.println("Palabra: " + juego.getPalabraActual());
            System.out.println("Intentos: " + juego.getIntentosRestantes());
            System.out.println("Usadas: " + juego.getLetrasUsadas());
            
            if (resultado.ganado) {
                System.out.println("\n¡GANASTE!");
                break;
            }
            if (resultado.perdido) {
                System.out.println("\nPERDISTE. La palabra era: " + juego.getPalabraSecreta());
                break;
            }
        }
        
        sc.close();
    }
	private String palabraSecreta;
	private char[] palabraActual;
	private Set<Character> letrasUsadas;
	private int intentosRestantes;  
	private int maxIntentos = 10;
	
	public Juego() {
		this.palabraSecreta= cargarPalabra();
		this.palabraActual = new char[palabraSecreta.length()];
		for(int i=0; i<palabraActual.length; i++) {
			palabraActual[i]='_';
		}
		this.letrasUsadas = new HashSet<>();
		this.intentosRestantes= maxIntentos;
		}
	
    private String cargarPalabra() {
        List<String> palabras = new ArrayList<>();
        
        try(BufferedReader br = new BufferedReader(
                new FileReader("palabras.txt"))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                palabras.add(linea.trim().toUpperCase());
            }
            
            br.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        Random random = new Random();
        return palabras.get(random.nextInt(palabras.size()));
    }
    public synchronized ResultadoIntento intentarLetra(char letra) {
        letra = Character.toUpperCase(letra);
        if (letrasUsadas.contains(letra)) {
            return new ResultadoIntento(false, "Ya usaste esa letra", false, false);
        }
        letrasUsadas.add(letra);
        boolean acierto = false;
        for (int i = 0; i < palabraSecreta.length(); i++) {
            if (palabraSecreta.charAt(i) == letra) {
                palabraActual[i] = letra; 
                acierto = true;
            }
        }
        
       if (!acierto) {
            intentosRestantes--;
        }
        
        boolean ganado = estaCompletada();
        
        boolean perdido = intentosRestantes <= 0;
        
        String mensaje = acierto ? "¡Correcto!" : "Letra incorrecta";
        
        return new ResultadoIntento(acierto, mensaje, ganado, perdido);
    }
    private boolean estaCompletada() {
        for (char c : palabraActual) {
            if (c == '_') {
                return false;  
            }
        }
        return true;  
    }
    
    public String getPalabraActual() {
        StringBuilder sb = new StringBuilder();
        for (char c : palabraActual) {
            sb.append(c).append(" ");
        }
        return sb.toString().trim();
    }
    
    public int getIntentosRestantes() {
        return intentosRestantes;
    }
    
    public String getLetrasUsadas() {
        return letrasUsadas.toString();
    }
    
    public String getPalabraSecreta() {
        return palabraSecreta;
    }
    
    public boolean haTerminado() {
        return estaCompletada() || intentosRestantes <= 0;
    }
    public static class ResultadoIntento {
        public final boolean acierto;   
        public final String mensaje;    
        public final boolean ganado;   
        public final boolean perdido; 
        
        public ResultadoIntento(boolean acierto, String mensaje, 
                               boolean ganado, boolean perdido) {
            this.acierto = acierto;
            this.mensaje = mensaje;
            this.ganado = ganado;
            this.perdido = perdido;
        }
    }
}

