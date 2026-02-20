/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo;

/**
 *
 * @author diego
 */
public class Proceso {
    // Datos estáticos del proceso
    private String id;
    private String nombre;
    private int instruccionesTotales;
    private String tipoRequerimiento; // "CPU" o "E/S"
    private int deadline; 
    private int prioridad;
    
    // Elementos dinámicos del PCB
    private EstadoProceso estado;
    private int pc; // Program Counter
    private int mar; // Memory Address Register
    private int ciclosRestantesDeadline;
    
    // Variables para E/S
    private int ciclosParaExcepcion;
    private int ciclosParaSatisfacer;

    public Proceso(String id, String nombre, int instruccionesTotales, String tipoRequerimiento, int deadline, int prioridad) {
        this.id = id;
        this.nombre = nombre;
        this.instruccionesTotales = instruccionesTotales;
        this.tipoRequerimiento = tipoRequerimiento;
        this.deadline = deadline;
        this.prioridad = prioridad;
        
        // El estado inicial siempre es NUEVO según el modelo [cite: 48]
        this.estado = EstadoProceso.NUEVO;
        this.pc = 0;
        this.mar = 0;
        this.ciclosRestantesDeadline = deadline;
    }

    // --- GETTERS Y SETTERS ---
    // En NetBeans: Presiona Alt + Insert (o clic derecho -> Insert Code) 
    // Selecciona "Getter and Setter" -> Marca la casilla "Proceso" para seleccionar todos -> Generate.

    // Método para simular el avance de un ciclo de reloj en la CPU
    public void avanzarCiclo() {
        this.pc++;
        this.mar++;
        this.ciclosRestantesDeadline--;
    }
}
