/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo;

import java.util.concurrent.Semaphore;

/**
 * @author diego
 */
public class Proceso implements Runnable {

    // --- Identificación ---
    private String id;
    private String nombre;

    // --- Estado y registros ---
    private EstadoProceso estado;
    private int pc;
    private int mar;

    // --- Planificación ---
    private int prioridad;
    private int deadline;
    private int tiempoRestanteDeadline;

    // --- Instrucciones ---
    private int instruccionesTotales;
    private int instruccionesEjecutadas;

    // --- Tipo y periodicidad ---
    private boolean periodico;
    private int periodo;

    // --- E/S ---
    private int ciclosParaES;
    private int duracionES;
    private int ciclosESRestantes;

    // --- CPU vs E/S ---
    private boolean cpuBound;

    // ========== 5.2 — Concurrencia del proceso ==========
    private final Semaphore semEjecutar = new Semaphore(0);  // CPU lo despierta
    private final Semaphore semTerminoCiclo = new Semaphore(0); // Proceso avisa que terminó ciclo
    private volatile boolean activo;  // controla el ciclo de vida del thread
    private Thread hilo;

    // ======================== Constructor ========================

    public Proceso(String id, String nombre, int instruccionesTotales,
                   int prioridad, int deadline,
                   boolean periodico, int periodo,
                   int ciclosParaES, int duracionES, boolean cpuBound) {
        this.id = id;
        this.nombre = nombre;
        this.instruccionesTotales = instruccionesTotales;
        this.prioridad = prioridad;
        this.deadline = deadline;
        this.tiempoRestanteDeadline = deadline;
        this.periodico = periodico;
        this.periodo = periodico ? periodo : 0;
        this.ciclosParaES = ciclosParaES;
        this.duracionES = duracionES;
        this.cpuBound = cpuBound;

        this.estado = EstadoProceso.NUEVO;
        this.pc = 0;
        this.mar = 0;
        this.instruccionesEjecutadas = 0;
        this.ciclosESRestantes = 0;
        this.activo = true;

        // 5.2 — Crear e iniciar el hilo del proceso
        this.hilo = new Thread(this, "Proceso-" + id);
        this.hilo.setDaemon(true);
        this.hilo.start();
    }

    // ========== 5.2 — run(): el proceso espera a que la CPU lo despierte ==========

    @Override
    public void run() {
        while (activo && !haTerminado()) {
            try {
                // Bloquearse hasta que la CPU libere un permit
                semEjecutar.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            if (!activo) break;

            // Ejecutar UNA instrucción
            this.pc++;
            this.instruccionesEjecutadas++;
            this.mar = this.pc;

            // Avisar a la CPU que terminó este ciclo
            semTerminoCiclo.release();
        }
    }

    /**
     * 5.2 — La CPU llama esto para ejecutar un ciclo del proceso.
     * Despierta al hilo del proceso y espera a que termine la instrucción.
     */
    public void ejecutarUnCiclo() {
        if (!activo || haTerminado()) return;
        semEjecutar.release();       // despertar al hilo del proceso
        try {
            semTerminoCiclo.acquire(); // esperar a que termine la instrucción
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Detiene el hilo del proceso (al terminar o al limpiar).
     */
    public void detenerHilo() {
        this.activo = false;
        semEjecutar.release(); // desbloquear si está esperando
        // Drenar por si hay permits pendientes
        semTerminoCiclo.drainPermits();
        if (hilo != null) {
            hilo.interrupt();
        }
    }

    // ======================== Transición validada ========================

    public void setEstado(EstadoProceso nuevoEstado) {
        if (this.estado == nuevoEstado) {
            return;
        }
        if (!this.estado.puedeTransicionarA(nuevoEstado)) {
            throw new IllegalStateException(
                String.format("Transición inválida: %s -> %s (Proceso %s)",
                    this.estado, nuevoEstado, this.id));
        }
        this.estado = nuevoEstado;
    }

    // ======================== Getters y Setters ========================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public EstadoProceso getEstado() { return estado; }

    public int getPc() { return pc; }
    public void setPc(int pc) { this.pc = pc; }

    public int getMar() { return mar; }
    public void setMar(int mar) { this.mar = mar; }

    public int getPrioridad() { return prioridad; }
    public void setPrioridad(int prioridad) { this.prioridad = prioridad; }

    public int getDeadline() { return deadline; }
    public void setDeadline(int deadline) { this.deadline = deadline; }

    public int getTiempoRestanteDeadline() { return tiempoRestanteDeadline; }
    public void setTiempoRestanteDeadline(int t) { this.tiempoRestanteDeadline = t; }

    public int getInstruccionesTotales() { return instruccionesTotales; }
    public void setInstruccionesTotales(int i) { this.instruccionesTotales = i; }

    public int getInstruccionesEjecutadas() { return instruccionesEjecutadas; }
    public void setInstruccionesEjecutadas(int i) { this.instruccionesEjecutadas = i; }

    public boolean isPeriodico() { return periodico; }
    public void setPeriodico(boolean periodico) { this.periodico = periodico; }

    public int getPeriodo() { return periodo; }
    public void setPeriodo(int periodo) { this.periodo = periodo; }

    public int getCiclosParaES() { return ciclosParaES; }
    public void setCiclosParaES(int c) { this.ciclosParaES = c; }

    public int getDuracionES() { return duracionES; }
    public void setDuracionES(int d) { this.duracionES = d; }

    public int getCiclosESRestantes() { return ciclosESRestantes; }
    public void setCiclosESRestantes(int c) { this.ciclosESRestantes = c; }

    public boolean isCpuBound() { return cpuBound; }
    public void setCpuBound(boolean cpuBound) { this.cpuBound = cpuBound; }

    // ======================== Métodos utilitarios ========================

    /**
     * @deprecated Usar ejecutarUnCiclo() desde la CPU para sincronización con threads.
     */
    public void avanzarCiclo() {
        this.pc++;
        this.instruccionesEjecutadas++;
    }

    public boolean haTerminado() {
        return instruccionesEjecutadas >= instruccionesTotales;
    }

    public boolean necesitaES() {
        return ciclosParaES > 0
                && instruccionesEjecutadas > 0
                && instruccionesEjecutadas % ciclosParaES == 0
                && !haTerminado();
    }

    public boolean deadlineExpirado() {
        return tiempoRestanteDeadline <= 0;
    }

    @Override
    public String toString() {
        return String.format(
            "Proceso[id=%s, nombre='%s', estado=%s, PC=%d, MAR=%d, prioridad=%d, "
            + "deadline=%d, deadlineRest=%d, instr=%d/%d, periodico=%b, periodo=%d, "
            + "ciclosParaES=%d, duracionES=%d, ciclosESRest=%d, cpuBound=%b]",
            id, nombre, estado, pc, mar, prioridad,
            deadline, tiempoRestanteDeadline,
            instruccionesEjecutadas, instruccionesTotales,
            periodico, periodo, ciclosParaES, duracionES, ciclosESRestantes, cpuBound);
    }
}
