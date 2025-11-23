package servidor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
//Clase que contiene la lógica del juego 
//Cada instancia representa una partida
//Gestiona: palabra secreta, letras acertadas, letras usadas,
//intentos restantes y estado general
public class Juego {
	//Guardamos palabbras para no hacer tantas llamadas a la API
	private static List<String> palabras = new ArrayList<>();
	private static final int tamaño_palabras = 50;
	   
	private String palabraSecreta;
	private char[] palabraActual;
	private Set<Character> letrasUsadas;
	private int intentosRestantes;  
	private static final int maxIntentos = 10;
	
	public Juego() {
		this.palabraSecreta= cargarPalabra();
		this.palabraActual = new char[palabraSecreta.length()];
		for(int i=0; i<palabraActual.length; i++) {
			palabraActual[i]='_';
		}
		this.letrasUsadas = new HashSet<>();
		this.intentosRestantes= maxIntentos;
		}
	
	//Métodos estaticos para la carga de palabras:
	
	public static String cargarPalabraAleatoria() {
        //Utilizar palabras de la lista si hay palabras
        if (!palabras.isEmpty()) {
            Random random = new Random();
            int n = random.nextInt(palabras.size());
            String palabra = palabras.remove(n);
            return palabra;
        }
        // Intentamos API (rellena el cache con 50 palabras)
        try {
            cargarPalabrasAPI();
            if (!palabras.isEmpty()) {
                String palabra = palabras.remove(0);
                return palabra;
            }
        } catch (Exception e) {
           e.printStackTrace();	        }
        
        //Intentar archivo palabras.txt
        try {
            String palabraArchivo = cargarPalabra();
            if (palabraArchivo != null) {
            	return palabraArchivo;
            }
        } catch (Exception e) {
           e.printStackTrace();
        }
        //Caso de que fallen las dos anteriores
        return obtenerPalabraPorDefecto();
    }
	 private static String obtenerPalabraPorDefecto() {
		 String[] palabrasDefecto = { "gato", "perro", "casa", "mesa", "silla",
				    "libro", "papel", "lapiz", "agua", "fuego",
				    "tierra", "cielo", "sol", "luna", "estrella",
				    "montaña", "rio", "mar", "playa", "bosque",
				    "arbol", "flor", "nube", "viento", "lluvia"};
		 Random random = new Random();
	     return palabrasDefecto[random.nextInt(palabrasDefecto.length)];
	    }
	//Selecciona palabras del archivo
    private static String cargarPalabra() {
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
        //Devuelve una aleatoria en mayusculas
        Random random = new Random();
        return palabras.get(random.nextInt(palabras.size()));
    }
    //Hace petición HTTP y carga 50 palabras
    private static void cargarPalabrasAPI() throws IOException{
        HttpURLConnection conn = null;
        BufferedReader in = null;
        try {
            // Crear conexión get
            String urlString = "https://random-word-api.herokuapp.com/word?lang=es&number=" + tamaño_palabras;
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()) );
            String respuesta = in.readLine();
            //Cambiamos a palabras nomales, vienen así ["palabra1","palabra2","palabra3",...]
            if (respuesta != null) {
                String[] palabrasAPI = respuesta.replace("[", "").replace("]", "").replace("\"", "").split(",");
                for (String palabra : palabrasAPI) {
                    String palabraSola = palabra.trim().toUpperCase();
                    palabras.add(palabraSola);
                }
             }
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    
    //Lógica del juego:
    
    // Método sincronizado para que varios hilos no modifiquen a la vez el estado
    // Intento de una letra y devuelve un objeto ResultadoIntento con datos del turno
    public synchronized ResultadoIntento intentarLetra(char letra) {
        letra = Character.toUpperCase(letra);
        //Comprobamos si se ha usado la letra
        if (letrasUsadas.contains(letra)) {
            return new ResultadoIntento(false, "Ya usaste esa letra\n", false, false);
        }
        //Añadimos la letra
        letrasUsadas.add(letra);
        boolean acierto = false;
        //Buscamos la letra en la palabra y la mostramos si está
        for (int i = 0; i < palabraSecreta.length(); i++) {
            if (palabraSecreta.charAt(i) == letra) {
                palabraActual[i] = letra; 
                acierto = true;
            }
        }
       //Si falla
       if (!acierto) {
            intentosRestantes--;
        }
        //Compruebo si ha ganado o perdido
        boolean ganado = estaCompletada();
        boolean perdido = intentosRestantes <= 0;
        //Creo mensaje
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
   
    //Getters de información que usa el cliente
    public String getPalabraActual() {
        StringBuilder sb = new StringBuilder();
        for (char c : palabraActual) {
            sb.append(c).append(" ");
        }
        return sb.toString();
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
    
    //Tiene la información de cada intento
    //Usamos la clase para devolver varios valores a la vez en solo un objeto
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
  //Main para probar que funciona el juego
//	public static void main(String[] args) {
//        Scanner sc = new Scanner(System.in);
//        Juego juego = new Juego();
//        
//        System.out.println("Juego de Ahorcado");
//        System.out.println("Palabra: " + juego.getPalabraActual());
//        System.out.println("Intentos: " + juego.getIntentosRestantes());
//        
//        while (!juego.haTerminado()) {
//            System.out.print("\nIntroduce una letra: ");
//            char letra = sc.next().charAt(0);
//            
//            ResultadoIntento resultado = juego.intentarLetra(letra);
//            
//            System.out.println(resultado.mensaje);
//            System.out.println("Palabra: " + juego.getPalabraActual());
//            System.out.println("Intentos: " + juego.getIntentosRestantes());
//            System.out.println("Usadas: " + juego.getLetrasUsadas());
//            
//            if (resultado.ganado) {
//                System.out.println("\n¡GANASTE!");
//                break;
//            }
//            if (resultado.perdido) {
//                System.out.println("\nPERDISTE. La palabra era: " + juego.getPalabraSecreta());
//                break;
//            }
//        }
//        
//        sc.close();
//    }
}

