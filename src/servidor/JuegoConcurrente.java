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
//Synchronized en todos los métodos ya que todos los hilos acceden a la vez, evitar condiciones de carrera
public class JuegoConcurrente {
	private String palabraSecreta;
	private char[] palabraActual;
	private Set<Character> letrasUsadas;
	private int intentosRestantes;
	private static final int max_intentos=10;
	private String ganador;
	private boolean partidaTerminada;
	
	public JuegoConcurrente() throws IOException {
		this.palabraSecreta = Juego.cargarPalabraAleatoria();
        this.palabraActual = new char[palabraSecreta.length()];
        
        for (int i = 0; i < palabraActual.length; i++) {
            palabraActual[i] = '_';
        }
        this.letrasUsadas = new HashSet<>();
        this.intentosRestantes = max_intentos;
        this.ganador = null;
        this.partidaTerminada = false;
	}
    // 
	//Intento de letra hecho por un jugador
    // Está sincronizado porque varios hilos llaman a este método a la vez
	public synchronized ResultadoConcurrente intentarLetra(String nombreJugador, char letra) {
		letra=  Character.toUpperCase(letra);
		if(partidaTerminada) {
			return new ResultadoConcurrente(false, "Partida terminada. Ganador: "+ganador, 
					false, false, false, nombreJugador);
		}
		 if (letrasUsadas.contains(letra)) {
	            return new ResultadoConcurrente(
	                false, "Ya se utilizo esa letra \n",false, false,false, nombreJugador
	            );
	     }
		 letrasUsadas.add(letra);
		 boolean acierto=false;
		 for (int i = 0; i < palabraSecreta.length(); i++) {
	            if (palabraSecreta.toUpperCase().charAt(i) == letra) {
	            	palabraActual[i] = letra;
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
    

    // Getters sincronizados porque consultan estado compartido
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
    

    // Clase usada para devolver resultado de un intento
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
