/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo;

/**
 *
 * @author diego
 */
import java.util.Random;


public class GeneradorProcesos {
    
    private int contadorId;
    private Random random;

    public GeneradorProcesos() {
        // Inicializamos el contador en 1 para los IDs (P001, P002, etc.)
        this.contadorId = 1;
        this.random = new Random();
    }

    
    public void generarProcesosIniciales(Memoria memoria, int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            Proceso p = crearProcesoAleatorio();
            memoria.encolarNuevo(p);
        }
        System.out.println(cantidad + " procesos generados y encolados en Nuevos.");
    }

    /**
     * Fabrica un solo proceso con valores aleatorios.
     * @return El objeto Proceso configurado.
     */
    public Proceso crearProcesoAleatorio() {
        // Generar ID con formato P001, P002, etc.
        String id = "P" + String.format("%03d", contadorId++);
        String nombre = "Proceso_" + id;
        
        // Instrucciones aleatorias (por ejemplo, entre 10 y 50 ciclos)
        int instrucciones = random.nextInt(41) + 10; 
        
        // CPU-bound (true) o IO-bound (false) con 50% probabilidad
        boolean cpuBound = random.nextBoolean();

        // 7.1: Cada cuántos ciclos genera excepción de E/S
        int ciclosParaES;
        // 7.2: Cuántos ciclos necesita para satisfacer la E/S
        int duracionES;

        if (cpuBound) {
            // CPU-bound: E/S infrecuente y corta
            ciclosParaES = 15 + random.nextInt(20);  // cada 15-34 ciclos
            duracionES = 1 + random.nextInt(3);       // 1-3 ciclos para satisfacer
        } else {
            // IO-bound: E/S frecuente y más larga
            ciclosParaES = 3 + random.nextInt(8);     // cada 3-10 ciclos
            duracionES = 3 + random.nextInt(6);       // 3-8 ciclos para satisfacer
        }

        // Deadline (mayor a instrucciones, +10 a +50 extra)
        int deadline = instrucciones + random.nextInt(41) + 10;
        
        // Prioridad aleatoria (1 a 5, donde 1 es la más alta)
        int prioridad = random.nextInt(5) + 1;

        // Periódico o aperiódico (30% periódico)
        boolean periodico = random.nextInt(10) < 3;
        int periodo = periodico ? (random.nextInt(20) + 10) : 0;

        // Retornamos el proceso recién fabricado
        return new Proceso(id, nombre, instrucciones, prioridad, deadline,
                           periodico, periodo, ciclosParaES, duracionES, cpuBound);
    }
}