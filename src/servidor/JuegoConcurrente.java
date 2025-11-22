package servidor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

//Todos los jugadores comparten misma palabra, intentos y letras usadas
//Gana quien pone la última letra
//Synchronized en todos los métodos ya que todos los hilos acceden a la vez
public class JuegoConcurrente {
	private String palabraSecreta;
	private char[] palabraActual;
	private Set<Character> letrasUsadas;
	private int intentosRestantes;
	private static final int max_intentos=10;
	private String ganador;
	private boolean partidaTerminada;
	
	public JuegoConcurrente() {
		this.palabraSecreta = cargarPalabraAleatoria();
        this.palabraActual = new char[palabraSecreta.length()];
        
        for (int i = 0; i < palabraActual.length; i++) {
            palabraActual[i] = '_';
        }
        this.letrasUsadas = new HashSet<>();
        this.intentosRestantes = max_intentos;
        this.ganador = null;
        this.partidaTerminada = false;
	}
	
	public synchronized ResultadoConcurrente intentarLetra(String nombreJugador, char letra) {
		letra=  Character.toUpperCase(letra);
		if(partidaTerminada) {
			return new ResultadoConcurrente(false, "Partida terminada. Ganador: "+ganador, 
					false, false, false, nombreJugador);
		}
		 if (letrasUsadas.contains(letra)) {
	            return new ResultadoConcurrente(
	                false, "Esa letra ya fue usada",false, false,false, nombreJugador
	            );
	     }
		 letrasUsadas.add(letra);
		 boolean acierto=false;
		 for (int i = 0; i < palabraSecreta.length(); i++) {
	            if (palabraSecreta.charAt(i) == letra) {
	                acierto = true;
	            }
	      }
		 if (!acierto) {
	            intentosRestantes--;
	     }
		 boolean completada = estaCompletada();
	     boolean perdido = intentosRestantes <= 0;
	     boolean esGanador = false;
	
	     if (completada) {
	            ganador = nombreJugador;
	            partidaTerminada = true;
	            esGanador = true;
	            System.out.println(nombreJugador+" gano la partida!");
	     }else if (perdido) {
	            partidaTerminada = true;
	            System.out.println("Partida perdida");
	     }
	     String mensaje = acierto ? "¡Correcto!" : "Letra incorrecta";
	     return new ResultadoConcurrente(
	          acierto, mensaje,completada,perdido,esGanador,nombreJugador
	     );
	}
    private boolean estaCompletada() {
	        for (char c : palabraActual) {
	            if (c == '_') return false;
	        }
	        return true;
	   }
    public synchronized String getPalabraActual() {
        StringBuilder sb = new StringBuilder();
        for (char c : palabraActual) {
            sb.append(c).append(" ");
        }
        return sb.toString().trim();
    }
    public synchronized int getIntentosRestantes() {
        return intentosRestantes;
    }
    
    public synchronized String getLetrasUsadas() {
        return letrasUsadas.toString();
    }
    
    public String getPalabraSecreta() {
        return palabraSecreta;
    }
    
    public synchronized String getGanador() {
        return ganador;
    }
    
    public synchronized boolean haTerminado() {
        return partidaTerminada;
    }
	
	private String cargarPalabraAleatoria() {
        List<String> palabras = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("palabras.txt"))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                palabras.add(linea.toUpperCase());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Random random = new Random();
        return palabras.get(random.nextInt(palabras.size()));
    }
	public static class ResultadoConcurrente {
        public boolean acierto;           
        public String mensaje;           
        public boolean completada;      
        public boolean perdido;          
        public boolean ganador;         
        public String nombreJugador;   
        
        public ResultadoConcurrente(boolean acierto, String mensaje, boolean completada, boolean perdido, 
                                   boolean ganador, String nombreJugador) {
            this.acierto = acierto;
            this.mensaje = mensaje;
            this.completada = completada;
            this.perdido = perdido;
            this.ganador = ganador;
            this.nombreJugador = nombreJugador;
        }
    }

}
