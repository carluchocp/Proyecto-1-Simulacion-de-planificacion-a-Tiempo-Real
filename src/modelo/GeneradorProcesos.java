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
            // Lo metemos a la memoria de forma segura (el método ya usa el semáforo)
            memoria.encolarListo(p);
        }
        System.out.println(cantidad + " procesos generados y encolados en Listos exitosamente.");
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
        
        // Tipo de requerimiento (50% probabilidad de CPU, 50% de E/S)
        String[] tipos = {"CPU", "E/S"};
        String tipo = tipos[random.nextInt(2)];
        
        // Deadline (Debe ser mayor a las instrucciones para que sea posible cumplirlo)
        // Le sumamos un extra aleatorio entre 10 y 50 ciclos más.
        int deadline = instrucciones + random.nextInt(41) + 10;
        
        // Prioridad aleatoria (Supongamos que va del 1 al 5, donde 1 es la más alta)
        int prioridad = random.nextInt(5) + 1;

        // Retornamos el proceso recién fabricado
        return new Proceso(id, nombre, instrucciones, tipo, deadline, prioridad);
    }
}