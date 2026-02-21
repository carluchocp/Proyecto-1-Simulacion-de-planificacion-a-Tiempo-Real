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
        
        // Deadline (mayor a instrucciones, +10 a +50 extra)
        int deadline = instrucciones + random.nextInt(41) + 10;
        
        // Prioridad aleatoria (1 a 5, donde 1 es la más alta)
        int prioridad = random.nextInt(5) + 1;

        // Periódico o aperiódico (30% periódico)
        boolean periodico = random.nextInt(10) < 3;
        int periodo = periodico ? (random.nextInt(20) + 10) : 0;

        // Ciclos para E/S: si es IO-bound cada 3-8 ciclos, si es CPU-bound cada 15-30 o 0
        int ciclosParaES = cpuBound 
            ? (random.nextBoolean() ? 0 : random.nextInt(16) + 15)
            : (random.nextInt(6) + 3);

        // Retornamos el proceso recién fabricado
        return new Proceso(id, nombre, instrucciones, prioridad, deadline,
                           periodico, periodo, ciclosParaES, cpuBound);
    }
}